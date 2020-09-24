package nva.commons.handlers;

import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;

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

    public static final String QUERY_STRING_PARAMETERS_FIELD = "queryStringParameters";
    public static final String PATH_PARAMETERS_FIELD = "pathParameters";
    public static final String PATH_FIELD = "path";
    public static final String HEADERS_FIELD = "headers";
    public static final String METHOD_ARN_FIELD = "methodArn";
    public static final String REQUEST_CONTEXT_FIELD = "requestContext";

    public static final String PROXY_TAG = "proxy";
    public static final String MISSING_FROM_HEADERS = "Missing from headers: ";
    public static final String MISSING_FROM_QUERY_PARAMETERS = "Missing from query parameters: ";
    public static final String MISSING_FROM_PATH_PARAMETERS = "Missing from pathParameters: ";
    public static final String MISSING_FROM_REQUEST_CONTEXT = "Missing from requestContext: ";

    public static final JsonPointer FEIDE_ID = JsonPointer.compile("/authorizer/claims/custom:feideId");
    public static final JsonPointer CUSTOMER_ID = JsonPointer.compile("/authorizer/claims/custom:customerId");
    public static final JsonPointer APPLICATION_ROLES = JsonPointer.compile(
        "/authorizer/claims/custom:applicationRoles");

    @JsonProperty(HEADERS_FIELD)
    private Map<String, String> headers;

    @JsonProperty(PATH_FIELD)
    private String path;

    @JsonProperty(PATH_PARAMETERS_FIELD)
    private Map<String, String> pathParameters;

    @JsonProperty(QUERY_STRING_PARAMETERS_FIELD)
    private Map<String, String> queryParameters;

    @JsonProperty(REQUEST_CONTEXT_FIELD)
    private JsonNode requestContext;

    @JsonProperty(METHOD_ARN_FIELD)
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

    @JsonIgnore
    public String getHeader(String header) {
        return Optional.ofNullable(getHeaders().get(header))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_HEADERS + header));
    }

    @JsonIgnore
    public String getQueryParameter(String parameter) {
        return Optional.ofNullable(getQueryParameters().get(parameter))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_QUERY_PARAMETERS + parameter));
    }

    @JsonIgnore
    public String getPathParameter(String parameter) {
        return Optional.ofNullable(getPathParameters().get(parameter))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_PATH_PARAMETERS + parameter));
    }

    @JsonIgnore
    @Deprecated
    public String getRequestContextParameter(JsonPointer jsonPointer) {
        return getRequestContextParameterOpt(jsonPointer)
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_REQUEST_CONTEXT + jsonPointer.toString()));
    }

    public Optional<String> getRequestContextParameterOpt(JsonPointer jsonPointer) {
        return Optional.ofNullable(getRequestContext())
            .map(requestContext -> requestContext.at(jsonPointer))
            .filter(not(JsonNode::isMissingNode))
            .map(JsonNode::asText);
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        if (isNull(headers)) {
            this.headers = new HashMap<>();
        } else {
            this.headers = headers;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public void setPathParameters(Map<String, String> pathParameters) {
        if (isNull(pathParameters)) {
            this.pathParameters = new HashMap<>();
        } else {
            this.pathParameters = pathParameters;
        }
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

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

    public Optional<String> getUsername() {
        return this.getRequestContextParameterOpt(FEIDE_ID);
    }

    public Optional<String> getCustomerId() {
        return getRequestContextParameterOpt(CUSTOMER_ID);
    }

    public Optional<String> getAssignedRoles() {
        return getRequestContextParameterOpt(APPLICATION_ROLES);
    }
}

