package nva.commons.handlers;

import static java.util.Objects.isNull;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.ApiGatewayUncheckedException;
import nva.commons.exceptions.GatewayResponseSerializingException;
import nva.commons.exceptions.InvalidOrMissingTypeException;
import nva.commons.exceptions.LoggerNotSetException;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.SingletonCollector;
import nva.commons.utils.StringUtils;
import nva.commons.utils.log.LogUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

/**
 * Template class for implementing Lambda function handlers that get activated through a call to ApiGateway. This class
 * is for processing a HTTP query directly without the usage of a Jersey-server or a SpringBoot template.
 *
 * @param <I> Class of the object in the body field of the ApiGateway message.
 * @param <O> Class of the response object.
 * @see <a href="https://github.com/awslabs/aws-serverless-java-container">aws-serverless-container</a> for
 *     alternative solutions.
 */
public abstract class ApiGatewayHandler<I, O> implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_PROBLEM_JSON = "application/problem+json";
    public static final String MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS =
        "Internal server error."
            + " Contact application administrator.";
    public static final String DEFAULT_ERROR_MESSAGE = "Unknown error in handler";
    public static final String REQUEST_ID = "requestId";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    protected final Environment environment;
    private final transient Class<I> iclass;
    private final transient ApiMessageParser<I> inputParser = new ApiMessageParser<>();
    protected Logger logger;
    protected transient OutputStream outputStream;
    protected transient String allowedOrigin;
    private Supplier<Map<String, String>> additionalSuccessHeadersSupplier;

    /**
     * The input class should be set explicitly by the inheriting class.
     *
     * @param iclass The class object of the input class.
     */
    @JacocoGenerated
    public ApiGatewayHandler(Class<I> iclass, Logger logger) {
        this(iclass, new Environment(), logger);
    }

    /**
     * The input class should be set explicitly by the inherting class.
     *
     * @param iclass      The class object of the input class.
     * @param environment the Environment from where the handler will read ENV variables.
     */
    public ApiGatewayHandler(Class<I> iclass, Environment environment, Logger logger) {
        this.iclass = iclass;
        this.environment = environment;
        this.additionalSuccessHeadersSupplier = Collections::emptyMap;
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
            logger.warn(e.getMessage());
            logger.warn(getStackTraceString(e));
            writeExpectedFailure(inputObject, e, context.getAwsRequestId());
        } catch (InvalidTypeIdException e) {
            logger.warn(e.getMessage());
            logger.warn(getStackTraceString(e));
            InvalidOrMissingTypeException apiGatewayInvalidTypeException = transformExceptionToApiGatewayException(e);
            writeExpectedFailure(inputObject, apiGatewayInvalidTypeException, context.getAwsRequestId());
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error(getStackTraceString(e));
            writeUnexpectedFailure(inputObject, e, context.getAwsRequestId());
        }
    }

    protected void init(OutputStream outputStream, Context context) throws LoggerNotSetException {
        this.outputStream = outputStream;
        this.allowedOrigin = environment.readEnv(ALLOWED_ORIGIN_ENV);
        if (isNull(logger)) {
            logger = LoggerFactory.getLogger(ApiGatewayHandler.class);
            throw new LoggerNotSetException(LogUtils.toLoggerName(this.getClass()));
        }
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by {@link
     * ApiGatewayHandler#writeFailure} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @return the Response body that is going to be serialized in json
     * @throws ApiGatewayException all exceptions are caught by writeFaiure and mapped to error codes through the method
     *                             {@link ApiGatewayHandler#getFailureStatusCode}
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
     * Define the success status code.
     *
     * @param input  The request input.
     * @param output The response output
     * @return the success status code.
     */
    protected abstract Integer getSuccessStatusCode(I input, O output);

    /**
     * Add a function that adds headers to the response.
     * <p>
     * Example:
     * <pre>
     *  {@code
     *
     * @Override
     *     protected String processInput(String input, RequestInfo requestInfo, Context context) throws Exception {
     *
     *         byte[] md5 = DigestUtils.md5(input);
     *         setAdditionalHeadersSupplier(
     *                  () -> Collections.singletonMap(HttpHeaders.CONTENT_MD5, new String(md5))
     *          );
     *         String output =input;
     *         return output;
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param additionalHeadersSupplier A supplier.
     */

    protected void setAdditionalHeadersSupplier(Supplier<Map<String, String>> additionalHeadersSupplier) {
        this.additionalSuccessHeadersSupplier = additionalHeadersSupplier;
    }

    /**
     * If you want to override this method, maybe better to override the {@link ApiGatewayHandler#defaultHeaders()}.
     *
     * @return a map with the response headers in case of success.
     */
    protected Map<String, String> getSuccessHeaders() {
        Map<String, String> headers = defaultHeaders();
        headers.putAll(additionalSuccessHeadersSupplier.get());
        return headers;
    }

    /**
     * If you want to override this method, maybe better to override the {@link ApiGatewayHandler#defaultHeaders()}.
     *
     * @return a map with the response headers in case of failure.
     */
    protected Map<String, String> getFailureHeaders() {
        Map<String, String> headers = defaultHeaders();
        headers.put(CONTENT_TYPE, APPLICATION_PROBLEM_JSON);
        return headers;
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
     * This is the message for the success case. Sends a JSON string containing the response that APIGateway will send
     * to the user.
     *
     * @param input  the input object of class I
     * @param output the output object of class O
     * @throws IOException when serializing fails
     */
    protected void writeOutput(I input, O output)
        throws IOException, GatewayResponseSerializingException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            GatewayResponse<O> gatewayResponse = new GatewayResponse<>(output, getSuccessHeaders(),
                getSuccessStatusCode(input, output));
            String responseJson = objectMapper.writeValueAsString(gatewayResponse);
            writer.write(responseJson);
        }
    }

    /**
     * Sends a message to ApiGateway and to the API-client, in case of failure caused by an ApiGatewayException
     * (predicted exception). This method can be overriden for richer status codes, but in the general case it should
     * not be neccessary/
     *
     * @param input     the input object of class I
     * @param exception the exception
     * @throws IOException when serializing fails
     */
    protected void writeExpectedFailure(I input, ApiGatewayException exception, String requestId) throws IOException {
        try {
            writeFailure(exception, getFailureStatusCode(input, exception), requestId);
        } catch (GatewayResponseSerializingException e) {
            throw new ApiGatewayUncheckedException(e);
        }
    }

    /**
     * Method for sending error messages in case of failure. It can be overriden but it should not be necessary in the
     * general case. It returns to the API-client a specified status code, the message of the exception and optionally
     * another additional message.
     *
     * @param exception  the thrown Exception.
     * @param statusCode the statusCode that should be returned to the API-client
     * @throws IOException when the writer throws an IOException.
     */
    protected void writeFailure(Exception exception, Integer statusCode, String requestId)
        throws IOException, GatewayResponseSerializingException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String errorMessage = Optional.ofNullable(exception.getMessage()).orElse(defaultErrorMessage());
            Status status = Status.valueOf(statusCode);

            ThrowableProblem problem = Problem.builder().withStatus(status)
                .withTitle(status.getReasonPhrase())
                .withDetail(errorMessage)
                .with(REQUEST_ID, requestId)
                .build();

            GatewayResponse<ThrowableProblem> gatewayResponse =
                new GatewayResponse<>(problem, getFailureHeaders(), statusCode);
            String gateWayResponseJson = objectMapper.writeValueAsString(gatewayResponse);
            writer.write(gateWayResponseJson);
        }
    }

    /**
     * Sends a message to ApiGateway and to the API-client  in case of failure caused by an Exception that is not
     * ApiGatewayException (unpredicted exception). This method can be overriden for richer status codes, but in the
     * general case it should not be necessary.
     *
     * @param input     the input object of class I
     * @param exception the exception
     * @throws IOException when serializing fails
     */
    protected void writeUnexpectedFailure(I input, Exception exception, String requestId)
        throws IOException {
        try {
            RuntimeException runtimeException =
                new RuntimeException(MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS);
            writeFailure(runtimeException, HttpStatus.SC_INTERNAL_SERVER_ERROR, requestId);
        } catch (GatewayResponseSerializingException e) {
            throw new ApiGatewayUncheckedException(e);
        }
    }

    protected Map<String, String> defaultHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    private InvalidOrMissingTypeException transformExceptionToApiGatewayException(InvalidTypeIdException e) {
        return new InvalidOrMissingTypeException(e);
    }

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

    private String defaultErrorMessage() {
        return String.format("%s, class: %s", DEFAULT_ERROR_MESSAGE, this.getClass().getName());
    }
}

