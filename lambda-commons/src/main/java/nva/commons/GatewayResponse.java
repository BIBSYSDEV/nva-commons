package nva.commons;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayResponse<T> implements Serializable {

    private final T body;
    private final Map<String, String> headers;
    private final int statusCode;

    /**
     * Constructor for GatewayResponse.
     *
     * @param body  body of response
     * @param headers   http headers for response
     * @param statusCode    status code for response
     */
    @JsonCreator
    public GatewayResponse(
        @JsonProperty("body") final T body,
        @JsonProperty("headers") final Map<String, String> headers,
        @JsonProperty("statusCode") final int statusCode) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = Collections.unmodifiableMap(Map.copyOf(headers));
    }

    public T getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static Map<String, String> defaultHeaders() {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("Content-Type", "application/json");
        return map;
    }
}

