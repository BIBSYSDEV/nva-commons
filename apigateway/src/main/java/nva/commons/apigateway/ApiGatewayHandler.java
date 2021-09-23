package nva.commons.apigateway;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static nva.commons.core.JsonUtils.objectMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ApiGatewayUncheckedException;
import nva.commons.apigateway.exceptions.GatewayResponseSerializingException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

public abstract class ApiGatewayHandler<I, O> extends RestRequestHandler<I, O> {

    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS =
        "Internal server error."
        + " Contact application administrator.";
    public static final String DEFAULT_ERROR_MESSAGE = "Unknown error in handler";
    public static final String REQUEST_ID = "requestId";

    private final ObjectMapper mapper;

    private Supplier<Map<String, String>> additionalSuccessHeadersSupplier;

    public ApiGatewayHandler(Class<I> iclass) {
        this(iclass, new Environment());
    }

    public ApiGatewayHandler(Class<I> iclass, Environment environment) {
        this(iclass, environment, objectMapper);
        this.additionalSuccessHeadersSupplier = Collections::emptyMap;
    }

    public ApiGatewayHandler(Class<I> iclass, Environment environment, ObjectMapper mapper) {
        super(iclass, environment);
        this.mapper = mapper;
        this.additionalSuccessHeadersSupplier = Collections::emptyMap;
    }

    @Override
    public void init(OutputStream outputStream, Context context) {
        this.allowedOrigin = environment.readEnv(ALLOWED_ORIGIN_ENV);
        super.init(outputStream, context);
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
    protected void writeOutput(I input, O output, RequestInfo requestInfo)
        throws IOException, GatewayResponseSerializingException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            GatewayResponse<O> gatewayResponse = new GatewayResponse<>(output, getSuccessHeaders(requestInfo),
                                                                       getSuccessStatusCode(input, output));
            String responseJson = mapper.writeValueAsString(gatewayResponse);
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
            writeFailure(runtimeException, HttpURLConnection.HTTP_INTERNAL_ERROR, requestId);
        } catch (GatewayResponseSerializingException e) {
            throw new ApiGatewayUncheckedException(e);
        }
    }

    /**
     * Add a function that adds headers to the response.
     *
     * @param additionalHeaders A supplier.
     */

    protected void addAdditionalHeaders(Supplier<Map<String, String>> additionalHeaders) {
        this.additionalSuccessHeadersSupplier = additionalHeaders;
    }

    /**
     * Use {@link ApiGatewayHandler#addAdditionalHeaders}.
     *
     * @param additionalHeaders a Map of additional success headers.
     */
    @Deprecated
    @JacocoGenerated
    protected void setAdditionalHeadersSupplier(Supplier<Map<String, String>> additionalHeaders) {
        addAdditionalHeaders(additionalHeaders);
    }

    /**
     * If you want to override this method, maybe better to override the
     * {@link ApiGatewayHandler#defaultHeaders(RequestInfo requestInfo)}.
     *
     * @return a map with the response headers in case of success.
     */
    protected Map<String, String> getSuccessHeaders(RequestInfo requestInfo) {
        Map<String, String> headers = defaultHeaders(requestInfo);
        headers.putAll(additionalSuccessHeadersSupplier.get());
        return headers;
    }

    /**
     * Method for sending error messages in case of failure. It can be overriden but it should not be necessary in the
     * general case. It returns to the API-client a specified status code, the message of the exception and optionally
     * another additional message.
     *
     * @param exception  the thrown Exception.
     * @param statusCode the statusCode that should be returned to the API-client.
     * @param requestId  the id of the request that caused the exception.
     * @throws IOException                         when the writer throws an IOException.
     * @throws GatewayResponseSerializingException when the writer throws an GatewayResponseSerializingException.
     */

    protected void writeFailure(Exception exception, Integer statusCode, String requestId)
        throws IOException, GatewayResponseSerializingException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
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
     * If you want to override this method, maybe better to override the
     * {@link ApiGatewayHandler#defaultHeaders(RequestInfo requestInfo)}.
     *
     * @return a map with the response headers in case of failure.
     */
    private Map<String, String> getFailureHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, MediaTypes.APPLICATION_PROBLEM_JSON.toString());
        return headers;
    }

    private Map<String, String> defaultHeaders(RequestInfo requestInfo) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, getDefaultResponseHeader(requestInfo).toString());
        return headers;
    }

    private String defaultErrorMessage() {
        return String.format("%s, class: %s", DEFAULT_ERROR_MESSAGE, this.getClass().getName());
    }
}
