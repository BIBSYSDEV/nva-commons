package nva.commons.apigateway.testutils;

import static nva.commons.apigateway.RequestInfo.PROXY_TAG;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class Handler extends ApiGatewayHandler<RequestBody, RequestBody> {

    private Map<String, String> headers;
    private String proxy;
    private String path;
    private RequestBody body;

    /**
     * Constructor with environment.
     *
     * @param environment the environment.
     */
    public Handler(Environment environment) {
        super(RequestBody.class, environment);
    }

    /**
     * Constructor that overrides default serialization.
     * @param environment the environment
     * @param mapper      Object Mapper
     */
    public Handler(Environment environment, ObjectMapper mapper) {
        super(RequestBody.class, environment, mapper);
    }

    @Override
    protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = requestInfo.getPathParameters().get(PROXY_TAG);
        this.path = requestInfo.getPath();
        this.body = input;
        this.addAdditionalHeaders(() -> additionalHeaders(body));
        return this.body; //String.join(",", input.getField1(), input.getField2());
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