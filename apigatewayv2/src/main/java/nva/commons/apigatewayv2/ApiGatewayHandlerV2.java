package nva.commons.apigatewayv2;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static nva.commons.apigatewayv2.MediaTypes.DEFAULT_SUPPORTED_MEDIA_TYPES;
import static nva.commons.apigatewayv2.MediaTypes.MOST_PREFERRED_DEFAULT_MEDIA_TYPE;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;
import nva.commons.apigatewayv2.exceptions.RedirectException;
import nva.commons.apigatewayv2.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.attempt.Try;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

public abstract class ApiGatewayHandlerV2<I, O>
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final String REQUEST_ID = "RequestId";
    public static final String INTERNAL_ERROR_MESSAGE = "Internal error";
    private static final String ALLOWED_ORIGIN = new Environment().readEnv("ALLOWED_ORIGIN");
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayHandlerV2.class);
    private Supplier<Map<String, String>> additionalSuccessHeadersSupplier;

    protected ApiGatewayHandlerV2() {
        additionalSuccessHeadersSupplier = Collections::emptyMap;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            logger.info("{}:{}", REQUEST_ID, context.getAwsRequestId());
            var output = processInput(input.getBody(), input, context);
            return createSuccessfulResponse(input, output);
        } catch (ApiGatewayException exception) {
            logger.error(ExceptionUtils.stackTraceInSingleLine(exception));
            var problem = createProblem(exception.getMessage(), exception.getStatusCode(), context);
            return problemToApiGatewayResponse(problem, exception);
        } catch (Exception exception) {
            logger.error(ExceptionUtils.stackTraceInSingleLine(exception));
            var problem = createProblem(INTERNAL_ERROR_MESSAGE, HTTP_INTERNAL_ERROR, context);
            return problemToApiGatewayResponse(problem, exception);
        }
    }

    protected void addAdditionalSuccessHeaders(Supplier<Map<String, String>> additionalHeaders) {
        this.additionalSuccessHeadersSupplier = Optional.ofNullable(additionalHeaders).orElse(Collections::emptyMap);
    }

    protected abstract Integer getSuccessStatusCode(String body, O output);

    protected abstract O processInput(String body, APIGatewayProxyRequestEvent input, Context context)
        throws ApiGatewayException;

    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_SUPPORTED_MEDIA_TYPES;
    }

    protected Map<String, String> getFailureHeaders() {
        var headers = new HashMap<>(defaultHeaders());
        headers.put(HttpHeaders.CONTENT_TYPE, MediaTypes.APPLICATION_PROBLEM_JSON.toString());
        return headers;
    }

    private ThrowableProblem createProblem(String message, Integer statusCode, Context context) {
        var status = Status.valueOf(statusCode);
        return Problem.builder()
            .withDetail(message)
            .withTitle(status.getReasonPhrase())
            .withStatus(status)
            .with(REQUEST_ID, context.getAwsRequestId())
            .build();
    }

    private MediaType calculateContentTypeHeader(APIGatewayProxyRequestEvent requestEvent)
        throws UnsupportedAcceptHeaderException {
        return attemptToMatchAcceptHeadersToSupportedHeaders(requestEvent)
            .orElseThrow(fail -> (UnsupportedAcceptHeaderException) fail.getException());
    }

    private Try<MediaType> attemptToMatchAcceptHeadersToSupportedHeaders(APIGatewayProxyRequestEvent requestEvent) {
        List<MediaType> supportedMediaTypes = listSupportedMediaTypes();
        return extractAcceptHeaders(requestEvent)
            .map(attempt(acceptedMediaTypes -> MediaTypes.parse(acceptedMediaTypes, supportedMediaTypes)))
            .orElse(Try.of(supportedMediaTypes.get(MOST_PREFERRED_DEFAULT_MEDIA_TYPE)));
    }

    private Optional<List<String>> extractAcceptHeaders(APIGatewayProxyRequestEvent requestEvent) {
        return Optional.ofNullable(requestEvent)
            .map(APIGatewayProxyRequestEvent::getMultiValueHeaders)
            .map(headers -> headers.get(HttpHeaders.ACCEPT));
    }

    private APIGatewayProxyResponseEvent createSuccessfulResponse(APIGatewayProxyRequestEvent input,
                                                                  O output) throws UnsupportedAcceptHeaderException {
        return new APIGatewayProxyResponseEvent()
            .withBody(Optional.ofNullable(output).map(Object::toString).orElse(null))
            .withStatusCode(getSuccessStatusCode(input.getBody(), output))
            .withHeaders(getSuccessHeaders(input));
    }

    private Map<String, String> getSuccessHeaders(APIGatewayProxyRequestEvent input)
        throws UnsupportedAcceptHeaderException {
        var successHeaders = new ConcurrentHashMap<>(defaultHeaders());
        var contentTypeHeader = calculateContentTypeHeader(input);
        successHeaders.put(HttpHeaders.CONTENT_TYPE, contentTypeHeader.toString());
        successHeaders.putAll(additionalSuccessHeadersSupplier.get());
        return successHeaders;
    }

    private Map<String, String> defaultHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
        return headers;
    }

    private APIGatewayProxyResponseEvent problemToApiGatewayResponse(ThrowableProblem problem,
                                                                     Exception exception) {

        var failureHeaders = getFailureHeaders();
        if (exception instanceof RedirectException) {
            failureHeaders.put(HttpHeaders.LOCATION, ((RedirectException) exception).getLocation().toString());
        }
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(problem.getStatus().getStatusCode())
            .withHeaders(failureHeaders)
            .withBody(attempt(problem::toString).orElseThrow());
    }
}
