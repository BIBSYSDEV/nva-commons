package nva.commons.handlers;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestInfo {

    public static final String PROXY_TAG = "proxy";
    private Map<String, String> headers;
    private String path;
    private Map<String, String> pathParameters;
    @JsonProperty("queryStringParameters")
    private Map<String, String> queryParameters;
    private JsonNode requestContext;

    /**
     * Default constructor.
     */
    public RequestInfo() {
        this.headers = new HashMap<>();
        this.pathParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
        this.requestContext = JsonUtils.objectMapper.createObjectNode();
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

    @JacocoGenerated
    public void setRequestContext(JsonNode requestContext) {
        if (isNull(requestContext)) {
            this.requestContext = JsonUtils.objectMapper.createObjectNode();
        } else {
            this.requestContext = requestContext;
        }
    }
}

