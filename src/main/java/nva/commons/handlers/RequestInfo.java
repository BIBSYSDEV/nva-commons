package nva.commons.handlers;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;

public class RequestInfo {

    public static final String PROXY_TAG = "proxy";
    public static final String MISSING_FROM_HEADERS = "Missing from headers: ";
    public static final String MISSING_FROM_QUERY_PARAMETERS = "Missing from query parameters: ";
    public static final String MISSING_FROM_PATH_PARAMETERS = "Missing from pathParameters: ";
    public static final String MISSING_FROM_REQUEST_CONTEXT = "Missing from requestContext: ";

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("path")
    private String path;

    @JsonProperty("pathParameters")
    private Map<String, String> pathParameters;

    @JsonProperty("queryStringParameters")
    private Map<String, String> queryParameters;

    @JsonProperty("requestContext")
    private JsonNode requestContext;

    @JsonProperty("methodArn")
    private String methodArn;

    @JsonAnySetter
    private Map<String, Object> otherProperties;

    /**
     * Default constructor.
     */
    public RequestInfo() {
        this.headers = new HashMap<>();
        this.pathParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
        this.otherProperties = new LinkedHashMap<>(); // ordinary HashMap and ConcurrentHashMap fail.
        this.requestContext = JsonUtils.objectMapper.createObjectNode();
    }

    /**
     * Get header from request info.
     *
     * @param header header name
     * @return header value
     */
    @JsonIgnore
    public String getHeader(String header) {
        return Optional.ofNullable(getHeaders().get(header))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_HEADERS + header));
    }

    /**
     * Get query parameter from request info.
     *
     * @param parameter parameter name
     * @return parameter value
     */
    @JsonIgnore
    public String getQueryParameter(String parameter) {
        return Optional.ofNullable(getQueryParameters().get(parameter))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_QUERY_PARAMETERS + parameter));
    }

    /**
     * Get path parameter from request info.
     *
     * @param parameter parameter name
     * @return parameter value
     */
    @JsonIgnore
    public String getPathParameter(String parameter) {
        return Optional.ofNullable(getPathParameters().get(parameter))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_PATH_PARAMETERS + parameter));
    }

    /**
     * Get parameter from request context baed on json pointer.
     *
     * @param jsonPointer json pointer to parameter
     * @return parameter value
     */
    @JsonIgnore
    public String getRequestContextParameter(JsonPointer jsonPointer) {
        JsonNode jsonNode = getRequestContext().at(jsonPointer);
        if (jsonNode.isMissingNode()) {
            throw new IllegalArgumentException(MISSING_FROM_REQUEST_CONTEXT + jsonPointer.toString());
        }
        return jsonNode.textValue();
    }

    @JacocoGenerated
    public String getMethodArn() {
        return methodArn;
    }

    @JacocoGenerated
    public void setMethodArn(String methodArn) {
        this.methodArn = methodArn;
    }

    @JacocoGenerated
    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    @JacocoGenerated
    public void setOtherProperties(Map<String, Object> otherProperties) {
        this.otherProperties = otherProperties;
    }

    /**
     * Get the headers map.
     *
     * @return headers.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Set headers. Null input results to an empty map.
     *
     * @param headers the headers.
     */
    public void setHeaders(Map<String, String> headers) {
        if (isNull(headers)) {
            this.headers = new HashMap<>();
        } else {
            this.headers = headers;
        }
    }

    /**
     * Get path.
     *
     * @return path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Set path.
     *
     * @param path path.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get pathParameters map.
     *
     * @return the pathParameters map
     */
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    /**
     * Set the pathParameters map. Null input results to an empty map.
     *
     * @param pathParameters the pathParameters map.
     */
    public void setPathParameters(Map<String, String> pathParameters) {
        if (isNull(pathParameters)) {
            this.pathParameters = new HashMap<>();
        } else {
            this.pathParameters = pathParameters;
        }
    }

    /**
     * Get the queryParameters map.
     *
     * @return the queryParameters map.
     */
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    /**
     * Set the queryParameters map. Null input results to an empty map.
     *
     * @param queryParameters the query parameters.
     */
    public void setQueryParameters(Map<String, String> queryParameters) {
        if (isNull(queryParameters)) {
            this.queryParameters = new HashMap<>();
        } else {
            this.queryParameters = queryParameters;
        }
    }

    @JacocoGenerated
    public JsonNode getRequestContext() {
        return requestContext;
    }

    /**
     * Set the requestContext.
     *
     * @param requestContext requestContext.
     */
    @JacocoGenerated
    public void setRequestContext(JsonNode requestContext) {
        if (isNull(requestContext)) {
            this.requestContext = JsonUtils.objectMapper.createObjectNode();
        } else {
            this.requestContext = requestContext;
        }
    }
}

