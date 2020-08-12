package nva.commons.handlers;

import static nva.commons.utils.JsonUtils.objectMapper;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.ApiGatewayUncheckedException;
import nva.commons.exceptions.GatewayResponseSerializingException;
import nva.commons.exceptions.LoggerNotSetException;
import nva.commons.utils.Environment;
import nva.commons.utils.JsonUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

public abstract class ApiGatewayHandler<I, O> extends RestRequestHandler<I, O> {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS =
        "Internal server error."
            + " Contact application administrator.";
    public static final String DEFAULT_ERROR_MESSAGE = "Unknown error in handler";
    public static final String REQUEST_ID = "requestId";

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_PROBLEM_JSON = "application/problem+json";

    private Supplier<Map<String, String>> additionalSuccessHeadersSupplier;

    public ApiGatewayHandler(Class<I> iclass, Logger logger) {
        super(iclass, logger);
        this.additionalSuccessHeadersSupplier = Collections::emptyMap;
    }

    public ApiGatewayHandler(Class<I> iclass, Environment environment, Logger logger) {
        super(iclass, environment, logger);
        this.additionalSuccessHeadersSupplier = Collections::emptyMap;
    }

    @Override
    public void init(OutputStream outputStream, Context context) throws LoggerNotSetException {
        this.allowedOrigin = environment.readEnv(ALLOWED_ORIGIN_ENV);
        super.init(outputStream, context);
    }

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
     * Sends a message to ApiGateway and to the API-client, in case of failure caused by an ApiGatewayException
     * (predicted exception). This method can be overriden for richer status codes, but in the general case it should
     * not be neccessary/
     *
     * @param input     the input object of class I
     * @param exception the exception
     * @throws IOException when serializing fails
     */
    @Override
    protected void writeExpectedFailure(I input, ApiGatewayException exception, String requestId) throws IOException {
        try {
            writeFailure(exception, getFailureStatusCode(input, exception), requestId);
        } catch (GatewayResponseSerializingException e) {
            throw new ApiGatewayUncheckedException(e);
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
    @Override
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
     * If you want to override this method, maybe better to override the {@link ApiGatewayHandler#defaultHeaders()}.
     *
     * @return a map with the response headers in case of failure.
     */
    protected Map<String, String> getFailureHeaders() {
        Map<String, String> headers = defaultHeaders();
        headers.put(CONTENT_TYPE, APPLICATION_PROBLEM_JSON);
        return headers;
    }

    protected Map<String, String> defaultHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    /**
     * This is the message for the success case. Sends a JSON string containing the response that APIGateway will send
     * to the user.
     *
     * @param input  the input object of class I
     * @param output the output object of class O
     * @throws IOException when serializing fails
     */
    @Override
    protected void writeOutput(I input, O output)
        throws IOException, GatewayResponseSerializingException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            GatewayResponse<O> gatewayResponse = new GatewayResponse<>(output, getSuccessHeaders(),
                getSuccessStatusCode(input, output));
            String responseJson = JsonUtils.objectMapper.writeValueAsString(gatewayResponse);
            writer.write(responseJson);
        }
    }

    private String defaultErrorMessage() {
        return String.format("%s, class: %s", DEFAULT_ERROR_MESSAGE, this.getClass().getName());
    }
}
