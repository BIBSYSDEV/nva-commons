package nva.commons.apigateway.testutils;

import static nva.commons.apigateway.RequestInfoConstants.PROXY_TAG;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import java.util.Collections;
import java.util.Map;
import nva.commons.apigateway.ApiGatewayProxyHandler;
import nva.commons.apigateway.ProxyResponse;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class ProxyHandler extends ApiGatewayProxyHandler<RequestBody, RequestBody> {

    public static final int HTTP_STATUS_CODE_TEST = 999;
    private Map<String, String> headers;
    private String proxy;
    private String path;
    private RequestBody body;

    public ProxyHandler() {
        super(RequestBody.class);
    }

    /**
     * Constructor that overrides default serialization.
     *
     * @param mapper Object Mapper
     */
    public ProxyHandler(ObjectMapper mapper) {
        super(RequestBody.class, new Environment(), mapper);
    }
    
    @Override
    protected ProxyResponse<RequestBody> processProxyInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = requestInfo.getPathParameters().get(PROXY_TAG);
        this.path = requestInfo.getPath();
        this.body = input;
        this.addAdditionalHeaders(() -> additionalHeaders(body));
        return new ProxyResponse<>(HTTP_STATUS_CODE_TEST, this.body);
    }

    private Map<String, String> additionalHeaders(RequestBody input) {
        return Collections.singletonMap(HttpHeaders.WARNING, body.getField1());
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getPath() {
        return path;
    }

    public RequestBody getBody() {
        return body;
    }
}