package nva.commons.apigateway;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.util.Objects.isNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

public abstract class ApiGatewayHandlerV2<I, O>
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = List.of(JSON_UTF_8);
    private static final String ALLOWED_ORIGIN = new Environment().readEnv("ALLOWED_ORIGIN");
    private Class<I> iclass;
    private Supplier<Map<String, String>> additionalSuccessHeadersSupplier;

    protected ApiGatewayHandlerV2(Class<I> iclass) {
        this.iclass = iclass;
        additionalSuccessHeadersSupplier = Collections::emptyMap;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            var inputBody = parseBody(input.getBody());
            var output = processInput(inputBody, input, context);
            return new APIGatewayProxyResponseEvent()
                .withBody(Optional.ofNullable(output).map(Object::toString).orElse(null))
                .withStatusCode(getSuccessStatusCode(inputBody, output))
                .withHeaders(getSuccessHeaders(input));
        } catch (ApiGatewayException e) {
            var problem = Problem.builder()
                .withDetail(e.getMessage())
                .withStatus(Status.valueOf(e.getStatusCode()))
                .build();
            return problemToApiGatewayResponse(problem);
        } catch (Exception e) {
            var problem = Problem.builder()
                .withStatus(Status.valueOf(HTTP_INTERNAL_ERROR))
                .withDetail("Internal error")
                .build();
            return problemToApiGatewayResponse(problem);
        }
    }

    protected void addAdditionalSuccessHeaders(Supplier<Map<String, String>> additionalHeaders) {
        this.additionalSuccessHeadersSupplier = Optional.ofNullable(additionalHeaders).orElse(Collections::emptyMap);
    }

    protected abstract Integer getSuccessStatusCode(I input, O output);

    protected abstract O processInput(I body, APIGatewayProxyRequestEvent input, Context context)
        throws ApiGatewayException;

    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_SUPPORTED_MEDIA_TYPES;
    }

    private Map<String, String> getSuccessHeaders(APIGatewayProxyRequestEvent input) {
        Map<String, String> successHeaders = new HashMap<>(defaultHeaders(input));
        successHeaders.putAll(additionalSuccessHeadersSupplier.get());
        return successHeaders;
    }

    private Map<String, String> defaultHeaders(APIGatewayProxyRequestEvent input) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN);
        return headers;
    }

    private APIGatewayProxyResponseEvent problemToApiGatewayResponse(ThrowableProblem problem) {
        return new APIGatewayProxyResponseEvent().withStatusCode(problem.getStatus().getStatusCode())
            .withBody(problem.toString());
    }

    private I parseBody(String body) {
        if (isNull(body)) {
            return null;
        }
        else if (String.class.equals(iclass)) {
            return (I) body;
        }
        else {
            return attempt(() -> dtoObjectMapper.readValue(body, iclass)).orElseThrow();
        }
    }

   
}
