package nva.commons.apigateway;

import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.essentials.JacocoGenerated;
import nva.commons.essentials.JsonUtils;

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

    public static final String FEIDE_ID_CLAIM = "custom:feideId";
    public static final String CUSTOMER_ID_CLAIM = "custom:customerId";
    public static final String APPLICATION_ROLES_CLAIM = "custom:applicationRoles";
    public static final String ACCESS_RIGHTS_CLAIM = "custom:accessRights";
    public static final String COMMA_DELIMITER = ",";
    private static final String CLAIMS_PATH = "/authorizer/claims/";
    public static final JsonPointer FEIDE_ID = claimToJsonPointer(FEIDE_ID_CLAIM);
    public static final JsonPointer CUSTOMER_ID = claimToJsonPointer(CUSTOMER_ID_CLAIM);
    public static final JsonPointer APPLICATION_ROLES = claimToJsonPointer(APPLICATION_ROLES_CLAIM);
    public static final JsonPointer ACCESS_RIGHTS = claimToJsonPointer(ACCESS_RIGHTS_CLAIM);

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
    public String getRequestContextParameter(JsonPointer jsonPointer) {
        return getRequestContextParameterOpt(jsonPointer)
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_REQUEST_CONTEXT + jsonPointer.toString()));
    }

    /**
     * Get request context parameter. The root node is the {@link RequestInfo#REQUEST_CONTEXT_FIELD} node of the {@link
     * RequestInfo} class.
     * <p>Example: {@code JsonPointer.compile("/authorizer/claims/custom:feideId");  }
     * </p>
     *
     * @param jsonPointer A {@link JsonPointer}
     * @return a present {@link Optional} if there is a non empty value for the parameter, an empty {@link Optional}
     *     otherwise.
     */
    public Optional<String> getRequestContextParameterOpt(JsonPointer jsonPointer) {
        return Optional.ofNullable(getRequestContext())
            .map(requestContext -> requestContext.at(jsonPointer))
            .filter(not(JsonNode::isMissingNode))
            .filter(not(JsonNode::isNull))
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
        this.headers = nonNullMap(headers);
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
        this.pathParameters = nonNullMap(pathParameters);
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(Map<String, String> queryParameters) {
        this.queryParameters = nonNullMap(queryParameters);
    }

    @JacocoGenerated
    public JsonNode getRequestContext() {
        return requestContext;
    }

    /**
     * Sets the request context.
     *
     * @param requestContext the request context.
     */
    @JacocoGenerated
    public void setRequestContext(JsonNode requestContext) {
        if (isNull(requestContext)) {
            this.requestContext = JsonUtils.objectMapper.createObjectNode();
        } else {
            this.requestContext = requestContext;
        }
    }

    @JsonIgnore
    public Optional<String> getFeideId() {
        return this.getRequestContextParameterOpt(FEIDE_ID);
    }

    @JsonIgnore
    public Optional<String> getCustomerId() {
        return getRequestContextParameterOpt(CUSTOMER_ID);
    }

    @JsonIgnore
    public Optional<String> getAssignedRoles() {
        return getRequestContextParameterOpt(APPLICATION_ROLES);
    }

    @JsonIgnore
    public Set<String> getAccessRights() {
        return getRequestContextParameterOpt(ACCESS_RIGHTS)
            .stream()
            .filter(not(String::isBlank))
            .map(accessRightsStr -> accessRightsStr.split(COMMA_DELIMITER))
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());
    }

    private static JsonPointer claimToJsonPointer(String claim) {
        return JsonPointer.compile(CLAIMS_PATH + claim);
    }

    private <K, V> Map<K, V> nonNullMap(Map<K, V> map) {
        if (isNull(map)) {
            return new HashMap<>();
        }
        return map;
    }
}

