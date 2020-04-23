package nva.commons.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.ApiGatewayUncheckedException;
import nva.commons.exceptions.GatewayResponseSerializingException;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
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

    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    public static final String DEFAULT_ERROR_MESSAGE = "Unknown error in handler";
    public static final String STACK_TRACE_DELIMITER = ":";
    private static final String CAUSE_PREFIX = "EXCEPTION_CAUSE:";
    private static final String STACK_TRACE_PREFIX = "STACK_TRACE:";
    private static final String SUPPRESSED_PREFIX = "SUPPRESSED_STACK:";
    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_ID_LOGGING_TEMPLATE = "%s:%s";

    private final transient Class<I> iclass;
    protected transient LambdaLogger logger;
    protected transient OutputStream outputStream;
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";

    protected transient String allowedOrigin;

    protected final Environment environment;

    private Supplier<Map<String, String>> additionalSuccessHeadersSupplier;

    private final transient ApiMessageParser<I> inputParser = new ApiMessageParser<>();

    /**
     * The input class should be set explicitly by the inherting class.
     *
     * @param iclass The class object of the input class.
     */
    @JacocoGenerated
    public ApiGatewayHandler(Class<I> iclass) {
        this(iclass, new Environment());
    }

    /**
     * The input class should be set explicitly by the inherting class.
     *
     * @param iclass      The class object of the input class.
     * @param environment the Environment from where the handler will read ENV variables.
     */
    public ApiGatewayHandler(Class<I> iclass, Environment environment) {
        this.iclass = iclass;
        this.environment = environment;
        this.additionalSuccessHeadersSupplier = Collections::emptyMap;
    }

    private void init(OutputStream outputStream, Context context) {
        this.outputStream = outputStream;
        this.logger = context.getLogger();
        this.allowedOrigin = environment.readEnv(ALLOWED_ORIGIN_ENV);
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
    protected final O processInput(I input, String apiGatewayInputString, Context context) throws ApiGatewayException {
        RequestInfo requestInfo = inputParser.getRequestInfo(apiGatewayInputString, logger);
        return processInput(input, requestInfo, context);
    }

    /**
     * Define the response statusCode in case of failure.
     *
     * @param input The request input.
     * @param error The exception that caused the failure.
     * @return
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
            writeFailure(exception, HttpStatus.SC_INTERNAL_SERVER_ERROR, requestId);
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

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        init(output, context);
        I inputObject = null;
        String inputString = IoUtils.streamToString(input);
        try {
            inputObject = parseInput(inputString);

            O response;
            response = processInput(inputObject, inputString, context);

            writeOutput(inputObject, response);
        } catch (ApiGatewayException e) {
            logger.log(e.getMessage());
            logger.log(getStackTraceString(e, context.getAwsRequestId()));
            writeExpectedFailure(inputObject, e, context.getAwsRequestId());
        } catch (Exception e) {
            logger.log(e.getMessage());
            logger.log(getStackTraceString(e, context.getAwsRequestId()));
            writeUnexpectedFailure(inputObject, e, context.getAwsRequestId());
        }
    }

    private String getStackTraceString(Exception e, String requestId) {
        e.printStackTrace();
        String requestIdString = String.format(REQUEST_ID_LOGGING_TEMPLATE, REQUEST_ID, requestId);
        String causeQueue = CAUSE_PREFIX + createCauseString(e);
        String stackTrace = STACK_TRACE_PREFIX + arrayToStream(e.getStackTrace());
        String suppressed = Optional.ofNullable(arrayToStream(e.getSuppressed()))
                                    .map(m -> SUPPRESSED_PREFIX + m)
                                    .orElse("");
        return String.join(STACK_TRACE_DELIMITER, requestIdString, causeQueue, stackTrace, suppressed);
    }

    private String createCauseString(Exception e) {
        List<Throwable> causeQueue = populateQueue(e);
        return causeQueue.stream().map(Throwable::toString).collect(Collectors.joining(STACK_TRACE_DELIMITER));
    }

    private List<Throwable> populateQueue(Exception e) {
        List<Throwable> causeQueue = new ArrayList<>();
        Throwable currentException = e;
        while (currentException != null) {
            causeQueue.add(currentException);
            currentException = currentException.getCause();
        }
        return causeQueue;
    }

    private static <T> String arrayToStream(T[] suppressed) {
        return Stream.of(suppressed)
                     .map(Object::toString)
                     .collect(Collectors.joining(STACK_TRACE_DELIMITER));
    }

    private Class<I> getIClass() {
        return iclass;
    }

    private String defaultErrorMessage() {
        return String.format("%s, class: %s", DEFAULT_ERROR_MESSAGE, this.getClass().getName());
    }
}

