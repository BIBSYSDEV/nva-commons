package nva.commons.apigateway;

import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.auth.CognitoUserInfo.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.NVA_USERNAME_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.SELECTED_CUSTOMER_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.TOP_LEVEL_ORG_CRISTIN_ID_CLAIM;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.auth.FetchUserInfo;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;

public class RequestInfo {

    public static final Environment ENVIRONMENT = new Environment();
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

    public static final String ENTRIES_DELIMITER = ",";
    public static final String HTTPS = "https"; // Api Gateway only supports HTTPS
    public static final String DOMAIN_NAME_FIELD = "domainName";
    public static final String AT = "@";
    private static final String CLAIMS_PATH = "/authorizer/claims/";
    public static final JsonPointer CUSTOMER_ID = claimToJsonPointer(SELECTED_CUSTOMER_CLAIM);
    public static final JsonPointer ACCESS_RIGHTS = claimToJsonPointer(ACCESS_RIGHTS_CLAIM);
    public static final JsonPointer NVA_USERNAME = claimToJsonPointer(NVA_USERNAME_CLAIM);
    private static final JsonPointer TOP_LEVEL_ORG_CRISTIN_ID = claimToJsonPointer(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM);
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final URI DEFAULT_COGNITO_URI = URI.create(ENVIRONMENT.readEnv("COGNITO_URI"));

    private final HttpClient httpClient;
    private final URI cognitoUri;

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

    public RequestInfo(HttpClient httpClient, URI cognitoUri) {
        this.httpClient = httpClient;
        this.cognitoUri = cognitoUri;
    }

    public RequestInfo() {
        this.headers = new HashMap<>();
        this.pathParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
        this.otherProperties = new LinkedHashMap<>(); // ordinary HashMap and ConcurrentHashMap fail.
        this.requestContext = defaultRestObjectMapper.createObjectNode();
        this.httpClient = DEFAULT_HTTP_CLIENT;
        this.cognitoUri = DEFAULT_COGNITO_URI;
    }

    @JsonIgnore
    public String getHeader(String header) {
        return Optional.ofNullable(getHeaders().get(header))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_HEADERS + header));
    }

    @JsonIgnore
    public String getQueryParameter(String parameter) throws BadRequestException {
        return getQueryParameterOpt(parameter)
            .orElseThrow(() -> new BadRequestException(MISSING_FROM_QUERY_PARAMETERS + parameter));
    }

    @JsonIgnore
    public Optional<String> getQueryParameterOpt(String parameter) {
        return Optional.ofNullable(getQueryParameters()).map(params -> params.get(parameter));
    }

    @JsonIgnore
    public String getPathParameter(String parameter) {
        return Optional.ofNullable(getPathParameters().get(parameter))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_PATH_PARAMETERS + parameter));
    }

    @JsonIgnore
    public String getRequestContextParameter(JsonPointer jsonPointer) {
        return getRequestContextParameterOpt(jsonPointer)
            .orElseThrow(
                () -> new IllegalArgumentException(MISSING_FROM_REQUEST_CONTEXT + jsonPointer.toString()));
    }

    /**
     * Get request context parameter. The root node is the {@link RequestInfo#REQUEST_CONTEXT_FIELD} node of the {@link
     * RequestInfo} class.
     * <p>Example: {@code JsonPointer.compile("/authorizer/claims/custom:currentCustomer");  }
     * </p>
     *
     * @param jsonPointer A {@link JsonPointer}
     * @return a present {@link Optional} if there is a non empty value for the parameter, an empty {@link Optional}
     *     otherwise.
     */
    @JsonIgnore
    public Optional<String> getRequestContextParameterOpt(JsonPointer jsonPointer) {
        return Optional.ofNullable(getRequestContext())
            .map(context -> context.at(jsonPointer))
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
            this.requestContext = defaultRestObjectMapper.createObjectNode();
        } else {
            this.requestContext = requestContext;
        }
    }

    @JsonIgnore
    public URI getRequestUri() {
        return new UriWrapper(HTTPS, getDomainName())
            .addChild(getPath())
            .addQueryParameters(getQueryParameters())
            .getUri();
    }

    @JsonIgnore
    public String getDomainName() {
        return attempt(() -> this.getRequestContext()
            .get(DOMAIN_NAME_FIELD).asText())
            .orElseThrow();
    }

    @JsonIgnore
    public boolean userIsAuthorized(String accessRight) {
        return checkAuthorizationOffline(accessRight)
               || checkAuthorizationOnline(accessRight);
    }

    @JsonIgnore
    public Optional<String> getCustomerId() {
        return getRequestContextParameterOpt(CUSTOMER_ID)
            .or(this::fetchCustomerIdFromCognito);
    }

    @JsonIgnore
    public String getNvaUsername() {
        return extractNvaUsernameOffline().orElseGet(this::fetchUserNameFromCognito);
    }

    @JsonIgnore
    public URI getTopLevelOrgCristinId() {
        return extractTopLevelOrgIdOffline()
            .map(URI::create)
            .orElseGet(this::fetchTopLevelOrgCristinIdFromCognito);
    }

    private Optional<String> extractTopLevelOrgIdOffline() {
        return getRequestContextParameterOpt(TOP_LEVEL_ORG_CRISTIN_ID);
    }

    private URI fetchTopLevelOrgCristinIdFromCognito() {
        return fetchUserInfoFromCognito()
            .map(CognitoUserInfo::getTopOrgCristinid)
            .orElseThrow();
    }

    private Optional<String> extractNvaUsernameOffline() {
        return getRequestContextParameterOpt(NVA_USERNAME);
    }

    private String fetchUserNameFromCognito() {
        return fetchUserInfoFromCognito().map(CognitoUserInfo::getNvaUsername).orElseThrow();
    }

    private static JsonPointer claimToJsonPointer(String claim) {
        return JsonPointer.compile(CLAIMS_PATH + claim);
    }


    private boolean checkAuthorizationOffline(String accessRight) {
        return getAccessRights().stream()
            .anyMatch(assignedAccessRight -> assignedAccessRight.equalsIgnoreCase(accessRight));
    }

    private Boolean checkAuthorizationOnline(String accessRight) {
        var accessRightAtCustomer = fetchCustomerIdFromCognito()
            .map(customer -> accessRight + AT + customer)
            .map(requestedRight -> requestedRight.toLowerCase(Locale.getDefault()));

        var availableRights = fetchAvailableRights();
        return accessRightAtCustomer.map(availableRights::contains).orElse(false);
    }

    private List<String> fetchAvailableRights() {
        return fetchUserInfoFromCognito()
            .map(CognitoUserInfo::getAccessRights)
            .map(accessRights -> accessRights.toLowerCase(Locale.getDefault()))
            .map(accessRights -> accessRights.split(ENTRIES_DELIMITER))
            .map(Arrays::asList)
            .orElse(fail -> Collections.<String>emptyList());
    }

    private Try<CognitoUserInfo> fetchUserInfoFromCognito() {
        return attempt(() -> new FetchUserInfo(httpClient, cognitoUri, extractAuthorizationHeader()))
            .map(FetchUserInfo::fetch);
    }

    private String extractAuthorizationHeader() {
        return this.getHeader(HttpHeaders.AUTHORIZATION);
    }

    private Optional<String> fetchCustomerIdFromCognito() {
        return fetchUserInfoFromCognito()
            .toOptional()
            .map(CognitoUserInfo::getCurrentCustomer)
            .map(URI::toString);
    }

    @JsonIgnore
    private Set<String> getAccessRights() {
        return getRequestContextParameterOpt(ACCESS_RIGHTS)
            .stream()
            .filter(not(String::isBlank))
            .map(accessRightsStr -> accessRightsStr.split(ENTRIES_DELIMITER))
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());
    }

    private <K, V> Map<K, V> nonNullMap(Map<K, V> map) {
        if (isNull(map)) {
            return new HashMap<>();
        }
        return map;
    }
}

