package nva.commons.apigatewayv2.testutils;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import com.google.common.net.HttpHeaders;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import nva.commons.apigatewayv2.ApiGatewayHandlerV2;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;

public class HandlerV2 extends ApiGatewayHandlerV2<RequestBody, RequestBody> {

    public static final String PROXY_TAG = "proxy";

    private Map<String, String> headers;
    private String proxy;
    private String path;
    private RequestBody body;

    public HandlerV2() {
        super();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getProxy() {
        return proxy;
    }

    public String getPath() {
        return path;
    }

    public RequestBody getBody() {
        return body;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, RequestBody output) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    protected RequestBody processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context)
        throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = Optional.ofNullable(requestInfo)
            .map(APIGatewayProxyRequestEvent::getPathParameters)
            .map(pathParameters -> pathParameters.get(PROXY_TAG))
            .orElse(null);
        this.path = requestInfo.getPath();
        this.body = attempt(() -> JSON.std.beanFrom(RequestBody.class, input)).orElse(fail -> null);
        this.addAdditionalSuccessHeaders(this::additionalHeaders);
        return this.body;
    }

    private Map<String, String> additionalHeaders() {
        return Collections.singletonMap(HttpHeaders.WARNING, body.getField1());
    }
}