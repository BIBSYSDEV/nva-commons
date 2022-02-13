package nva.commons.apigateway.testutils;

import static nva.commons.apigateway.RequestInfo.PROXY_TAG;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.ApiGatewayHandlerV2;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class HandlerV2 extends ApiGatewayHandlerV2<RequestBody, RequestBody> {

    private Map<String, String> headers;
    private String proxy;
    private String path;
    private RequestBody body;

    public HandlerV2() {
        super(RequestBody.class);
    }


    @Override
    protected RequestBody processInput(RequestBody input, APIGatewayProxyRequestEvent requestInfo, Context context)
        throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = requestInfo.getPathParameters().get(PROXY_TAG);
        this.path = requestInfo.getPath();
        this.body = input;
        this.addAdditionalSuccessHeaders(this::additionalHeaders);
        return this.body;
    }

    private Map<String, String> additionalHeaders() {
        return Collections.singletonMap(HttpHeaders.WARNING, body.getField1());
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, RequestBody output) {
        return HttpURLConnection.HTTP_OK;
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
}