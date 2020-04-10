package nva.commons.hanlders;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import nva.commons.utils.JacocoGenerated;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestInfo {

    public static final String PROXY_TAG = "proxy";
    private Map<String, String> headers;
    private String path;
    private String proxy;

    public RequestInfo() {
        this.headers = new HashMap<>();
    }

    @JacocoGenerated
    public Map<String, String> getHeaders() {
        return headers;
    }

    @JacocoGenerated
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @JacocoGenerated
    public String getPath() {
        return path;
    }

    @JacocoGenerated
    public void setPath(String path) {
        this.path = path;
    }

    @JacocoGenerated
    public String getProxy() {
        return proxy;
    }

    @JacocoGenerated
    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    /**
     * Method for extracting the "proxy" value.
     *
     * @param pathParameters the pathParameters object in the json representation of the ApiGateway  input.
     */
    @JacocoGenerated
    @JsonProperty("pathParameters")
    public void unnestProxy(Map<String, Object> pathParameters) {
        if (pathParameters != null) {
            String proxy = (String) pathParameters.get(PROXY_TAG);
            setProxy(proxy);
        }
    }
}

