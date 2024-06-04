package nva.commons.apigateway.testutils;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.net.MediaType.XML_UTF_8;
import static nva.commons.apigateway.RequestInfoConstants.PROXY_TAG;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class RawStringResponseHandler extends ApiGatewayHandler<RequestBody, String> {

    private Map<String, String> headers;
    private String proxy;
    private String path;
    private RequestBody body;

    public RawStringResponseHandler() {
        super(RequestBody.class);
    }

    /**
     * Constructor that overrides default serialization.
     *
     * @param mapper Object Mapper
     */
    public RawStringResponseHandler(ObjectMapper mapper) {
        super(RequestBody.class, new Environment(), mapper);
    }


    @Override
    protected String processInput(RequestBody input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
        this.headers = requestInfo.getHeaders();
        this.proxy = requestInfo.getPathParameters().get(PROXY_TAG);
        this.path = requestInfo.getPath();
        this.body = input;
        this.addAdditionalHeaders(() -> additionalHeaders(body));

        ObjectMapper objectMapper = getObjectMapper(requestInfo);
        return attempt(() -> objectMapper.writeValueAsString(body)).orElseThrow();
    }

    private Map<String, String> additionalHeaders(RequestBody input) {
        return Collections.singletonMap(HttpHeaders.WARNING, body.getField1());
    }

    @Override
    protected List<MediaType> listSupportedMediaTypes() {
        return List.of(JSON_UTF_8, XML_UTF_8);
    }

    @Override
    protected void validateRequest(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //no-op
    }

    @Override
    protected Map<MediaType, ObjectMapper> getObjectMappers() {
        return Map.of(XML_UTF_8, new XmlMapper());
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