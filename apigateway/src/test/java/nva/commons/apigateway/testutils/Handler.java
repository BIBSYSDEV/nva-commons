package nva.commons.apigateway.testutils;

import static nva.commons.apigateway.RequestInfoConstants.PROXY_TAG;
import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class Handler extends ApiGatewayHandler<RequestBody, RequestBody> {

    private Map<String, String> headers;
    private String proxy;
    private String path;
    private RequestBody body;

    public Handler() {
        super(RequestBody.class);
    }

    /**
     * Constructor that overrides default serialization.
     */
    public Handler(Environment environment, HttpClient httpClient) {
        super(RequestBody.class, environment, httpClient);
    }

    @Override
    protected void validateRequest(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //no-op
    }

    @Override
    protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = requestInfo.getPathParameters().get(PROXY_TAG);
        this.path = requestInfo.getPath();
        this.body = input;
        this.addAdditionalHeaders(() -> additionalHeaders(body));
        return this.body;
    }

    private Map<String, String> additionalHeaders(RequestBody input) {
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