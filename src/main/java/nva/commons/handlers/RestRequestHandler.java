package nva.commons.handlers;

import static java.util.Objects.isNull;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.GatewayResponseSerializingException;
import nva.commons.exceptions.InvalidOrMissingTypeException;
import nva.commons.exceptions.LoggerNotSetException;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.SingletonCollector;
import nva.commons.utils.StringUtils;
import nva.commons.utils.log.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template class for implementing Lambda function handlers that get activated through a call to ApiGateway. This class
 * is for processing a HTTP query directly without the usage of a Jersey-server or a SpringBoot template.
 *
 * @param <I> Class of the object in the body field of the ApiGateway message.
 * @param <O> Class of the response object.
 * @see <a href="https://github.com/awslabs/aws-serverless-java-container">aws-serverless-container</a> for
 *     alternative solutions.
 */
public abstract class RestRequestHandler<I, O> implements RequestStreamHandler {

    protected final Environment environment;
    private final transient Class<I> iclass;
    private final transient ApiMessageParser<I> inputParser = new ApiMessageParser<>();
    protected Logger logger;
    protected transient OutputStream outputStream;
    protected transient String allowedOrigin;


    /**
     * The input class should be set explicitly by the inherting class.
     *
     * @param iclass      The class object of the input class.
     * @param environment the Environment from where the handler will read ENV variables.
     */
    public RestRequestHandler(Class<I> iclass, Environment environment, Logger logger) {
        this.iclass = iclass;
        this.environment = environment;
        this.logger = logger;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        I inputObject = null;
        try {

            init(output, context);
            String inputString = IoUtils.streamToString(input);
            inputObject = parseInput(inputString);

            O response;
            response = processInput(inputObject, inputString, context);

            writeOutput(inputObject, response);
        } catch (ApiGatewayException e) {
            handleExpectedException(context, inputObject, e);
        } catch (InvalidTypeIdException e) {
            handleTypeIdException(context, inputObject, e);
        } catch (Exception e) {
            handleUnexpectedException(context, inputObject, e);
        }
    }

    protected void handleUnexpectedException(Context context, I inputObject, Exception e) throws IOException {
        logger.error(e.getMessage());
        logger.error(getStackTraceString(e));
        writeUnexpectedFailure(inputObject, e, context.getAwsRequestId());
    }

    protected void handleTypeIdException(Context context, I inputObject, InvalidTypeIdException e) throws IOException {
        logger.warn(e.getMessage());
        logger.warn(getStackTraceString(e));
        InvalidOrMissingTypeException apiGatewayInvalidTypeException = transformExceptionToApiGatewayException(e);
        writeExpectedFailure(inputObject, apiGatewayInvalidTypeException, context.getAwsRequestId());
    }

    protected void handleExpectedException(Context context, I inputObject, ApiGatewayException e) throws IOException {
        logger.warn(e.getMessage());
        logger.warn(getStackTraceString(e));
        writeExpectedFailure(inputObject, e, context.getAwsRequestId());
    }

    protected void init(OutputStream outputStream, Context context) throws LoggerNotSetException {
        this.outputStream = outputStream;
        if (isNull(logger)) {
            logger = LoggerFactory.getLogger(RestRequestHandler.class);
            throw new LoggerNotSetException(LogUtils.toLoggerName(this.getClass()));
        }
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by {@link
     * RestRequestHandler#handleExpectedException} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @return the Response body that is going to be serialized in json
     * @throws ApiGatewayException all exceptions are caught by writeFailure and mapped to error codes through the
     *                             method {@link RestRequestHandler#getFailureStatusCode}
     */
    protected abstract O processInput(I input, RequestInfo requestInfo, Context context) throws ApiGatewayException;

    /**
     * Maps an object I to an object O.
     *
     * @param input                 the input object of class I
     * @param apiGatewayInputString The message of apiGateway, for extracting the headers and in case we need other
     *                              fields during the processing
     * @param context               the Context
     * @return an output object of class O
     * @throws IOException        when processing fails
     * @throws URISyntaxException when processing fails
     */
    protected O processInput(I input, String apiGatewayInputString, Context context) throws ApiGatewayException {
        RequestInfo requestInfo = inputParser.getRequestInfo(apiGatewayInputString);
        return processInput(input, requestInfo, context);
    }

    /**
     * Define the response statusCode in case of failure.
     *
     * @param input The request input.
     * @param error The exception that caused the failure.
     * @return the failure status code.
     */
    protected int getFailureStatusCode(I input, ApiGatewayException error) {
        return error.getStatusCode();
    }

    /**
     * Method for parsing the input object from the ApiGateway message.
     *
     * @param inputString the ApiGateway message.
     * @return an object of class I.
     * @throws IOException when parsing fails.
     */
    protected I parseInput(String inputString) throws IOException {
        return inputParser.getBodyElementFromJson(inputString, getIClass());
    }

    /**
     * Define the success status code.
     *
     * @param input  The request input.
     * @param output The response output
     * @return the success status code.
     */
    protected abstract Integer getSuccessStatusCode(I input, O output);

    protected abstract void writeOutput(I input, O output) throws IOException, GatewayResponseSerializingException;

    protected abstract void writeExpectedFailure(I input, ApiGatewayException exception, String requestId)
        throws IOException;

    protected abstract void writeUnexpectedFailure(I input, Exception exception, String requestId) throws IOException;

    private String getStackTraceString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionString = sw.toString();
        String exceptionStringNoNewLines = Stream.of(exceptionString)
            .map(StringUtils::removeMultipleWhiteSpaces)
            .map(StringUtils::replaceWhiteSpacesWithSpace)
            .collect(SingletonCollector.collect());
        return exceptionStringNoNewLines;
    }

    private Class<I> getIClass() {
        return iclass;
    }

    private InvalidOrMissingTypeException transformExceptionToApiGatewayException(InvalidTypeIdException e) {
        return new InvalidOrMissingTypeException(e);
    }
}

