package nva.commons;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.Collections;
import java.util.Map;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.hanlders.ApiGatewayHandler;
import nva.commons.hanlders.RequestInfo;
import nva.commons.utils.Environment;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

public class Handler extends ApiGatewayHandler<RequestBody, String> {

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

    @Override
    protected String processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = requestInfo.getProxy();
        this.path = requestInfo.getPath();
        this.body = input;
        this.setAdditionalHeadersSupplier(() -> additionalHeaders(body));
        return String.join(",", input.getField1(), input.getField2());
    }

    private Map<String, String> additionalHeaders(RequestBody input) {
        return Collections.singletonMap(HttpHeaders.WARNING, body.getField1());
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, String output) {
        return HttpStatus.SC_OK;
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