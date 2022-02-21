package nva.commons.apigateway.testutils;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.attempt.Try;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;

import static nva.commons.apigateway.RequestInfo.PROXY_TAG;

public class RawJsonResponseHandler extends ApiGatewayHandler<RequestBody, String> {

    private Map<String, String> headers;
    private String proxy;
    private String path;
    private RequestBody body;

    public RawJsonResponseHandler() {
        super(RequestBody.class);
    }

    /**
     * Constructor that overrides default serialization.
     *
     * @param mapper Object Mapper
     */
    public RawJsonResponseHandler(ObjectMapper mapper) {
        super(RequestBody.class, new Environment(), Collections.emptyMap(), mapper);
    }

    @Override
    protected String processInput(RequestBody input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = requestInfo.getPathParameters().get(PROXY_TAG);
        this.path = requestInfo.getPath();
        this.body = input;
        this.addAdditionalHeaders(() -> additionalHeaders(body));
        return Try.attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(body)).orElseThrow();
    }

    private Map<String, String> additionalHeaders(RequestBody input) {
        return Collections.singletonMap(HttpHeaders.WARNING, body.getField1());
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, String output) {
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