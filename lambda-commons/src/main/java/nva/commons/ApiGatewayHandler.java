package nva.commons;

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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.apache.http.entity.ContentType;

/**
 * Template class for implementing Lambda function handlers that get activated through a call to ApiGateway. This class
 * is for processing a HTTP query directly without the usage of a Jersey-server or a SpringBoot template.
 *
 * @param <I> Class of the object in the body field of the ApiGateway message.
 * @param <O> Class of the response object.
 * @see <a href="https://github.com/awslabs/aws-serverless-java-container">aws-serverless-container</a> for
 * alternative solutions.
 */
public abstract class ApiGatewayHandler<I, O> implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    public static final String CONTENT_TYPE = "Content-Type";

    private final static ObjectMapper objectMapper = JsonUtils.jsonParser;
    public static final String DEFAULT_ERROR_MESSAGE = "Unknown error in handler";

    private final transient Class<I> iclass;
    private transient LambdaLogger logger;
    protected transient OutputStream outputStream;
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";

    protected final Environment environment;

    protected final transient String allowedOrigin;

    private Supplier<Map<String, String>> additionalSuccessHeadersSupplier;

    private final transient ApiMessageParser<I> inputParser = new ApiMessageParser<>();

    public ApiGatewayHandler(Class<I> iclass) {
        this(iclass, new Environment());
    }

    public ApiGatewayHandler(Class<I> iclass, Environment environment) {
        this.iclass = iclass;
        this.environment = environment;
        this.allowedOrigin = environment.readEnv(ALLOWED_ORIGIN_ENV);
        this.additionalSuccessHeadersSupplier = () -> Collections.emptyMap();
    }

    private void init(OutputStream outputStream, Context context) {
        this.outputStream = outputStream;
        this.logger = context.getLogger();
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by {@link
     * ApiGatewayHandler#writeFailure) method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @return the Response body that is going to be serialized in json
     * @throws Exception all exceptions are caught by writeFaiure and mapped to error codes through the method {@link
     *                   ApiGatewayHandler#getFailureStatusCode}
     */
    protected abstract O processInput(I input, RequestInfo requestInfo, Context context) throws Exception;

    /**
     * Define the response headers in case of failure.
     *
     * @param input The request input.
     * @param error The exception that caused the failure.
     * @return
     */
    protected abstract int getFailureStatusCode(I input, Exception error);

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
     * </pre>
     *
     * @param additionalHeadersSupplier A supplier.
     */
    protected void setAdditionalHeadersSupplier(Supplier<Map<String, String>> additionalHeadersSupplier) {
        this.additionalSuccessHeadersSupplier = additionalHeadersSupplier;
    }

    /**
     * If you want to override this method, maybe better to override the {@link ApiGatewayHandler#defaultHeaders()}
     * @return a map with the response headers in case of success
     */
    protected Map<String, String> getSuccessHeaders() {
        Map<String, String> headers = defaultHeaders();
        headers.putAll(additionalSuccessHeadersSupplier.get());
        return headers;
    }

    /**
     * If you want to override this method, maybe better to override the {@link ApiGatewayHandler#defaultHeaders()}
     * @return a map with the response headers in case of failure
     */
    protected Map<String, String> getFailureHeaders() {
        return defaultHeaders();
    }

    /**
     * Method for parsing the input object from the ApiGateway message.
     *
     * @param inputString the ApiGateway message
     * @return an object of class I
     * @throws IOException when parsing fails
     */
    protected I parseInput(String inputString) throws IOException {
        return inputParser.getBodyElementFromJson(inputString, getIClass());
    }

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
    protected final O processInput(I input, String apiGatewayInputString, Context context) throws Exception {
        RequestInfo requestInfo = inputParser.getRequestInfo(apiGatewayInputString);
        return processInput(input, requestInfo, context);
    }

    /**
     * This is the message for the sucess case. Sends a JSON string containing the response that APIGateway will send to
     * the user.
     *
     * @param input  the input object of class I
     * @param output the output object of class O
     * @throws IOException when serializing fails
     */
    protected void writeOutput(I input, O output)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            GatewayResponse<O> gatewayResponse =
                new GatewayResponse<>(output, getSuccessHeaders(), getSuccessStatusCode(input, output));
            String responseJson = objectMapper.writeValueAsString(gatewayResponse);
            writer.write(responseJson);
        }
    }

    /**
     * Sends a message to ApiGateway and to the user, in case of failure. This method should be overriden for richer
     * status codes.
     *
     * @param input the input object of class I
     * @param error the exception
     * @throws IOException when serializing fails
     */

    protected void writeFailure(I input, Exception error) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String outputString = Optional.ofNullable(error.getMessage()).orElse(defaultErrorMessage());
            GatewayResponse<String> gatewayResponse =
                new GatewayResponse<>(outputString, getFailureHeaders(), getFailureStatusCode(input, error));
            String gateWayResponseJson = objectMapper.writeValueAsString(gatewayResponse);
            writer.write(gateWayResponseJson);
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
        } catch (Exception e) {
            logger.log(e.getMessage());
            writeFailure(inputObject, e);
        }
    }

    private Class<I> getIClass() {
        return iclass;
    }

    private String defaultErrorMessage() {
        return String.format("%s, class: %s", DEFAULT_ERROR_MESSAGE, this.getClass().getName());
    }
}

