package nva.commons.handlers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import nva.commons.utils.JacocoGenerated;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestInfo {

    public static final String PROXY_TAG = "proxy";
    private Map<String, String> headers;
    private String path;
    private Map<String,String> pathParameters;

    public RequestInfo() {
        this.headers = new HashMap<>();
        this.pathParameters = new HashMap<>();
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
    public Map<String,String> getPathParameters() {
        return pathParameters;
    }

    @JacocoGenerated
    public void setPathParameters(Map<String,String> pathParameters) {
        this.pathParameters = pathParameters;
    }

}

