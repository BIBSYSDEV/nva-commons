package nva.commons.apigateway;

import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.auth.CognitoUserInfo.ELEMENTS_DELIMITER;
import static nva.commons.apigateway.RequestInfoConstants.AUTHORIZATION_FAILURE_WARNING;
import static nva.commons.apigateway.RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE;
import static nva.commons.apigateway.RequestInfoConstants.CLAIMS_PATH;
import static nva.commons.apigateway.RequestInfoConstants.CLIENT_ID;
import static nva.commons.apigateway.RequestInfoConstants.CLIENT_ID_CLAIM;
import static nva.commons.apigateway.RequestInfoConstants.DOMAIN_NAME_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.HEADERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.METHOD_ARN_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_HEADERS;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_PATH_PARAMETERS;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_QUERY_PARAMETERS;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_REQUEST_CONTEXT;
import static nva.commons.apigateway.RequestInfoConstants.MULTI_VALUE_QUERY_STRING_PARAMETERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.PATH_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.PATH_PARAMETERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.QUERY_STRING_PARAMETERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.REQUEST_CONTEXT_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.SCOPE;
import static nva.commons.apigateway.RequestInfoConstants.SCOPES_CLAIM;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "PMD.CouplingBetweenObjects"})
public final class RequestInfo {

    private static final Logger logger = LoggerFactory.getLogger(RequestInfo.class);
    private static final ObjectMapper mapper = defaultRestObjectMapper;
    private static final String THIRD_PARTY_SCOPE_PREFIX = "https://api.nva.unit.no/scopes/third-party";
    private static final String COMMA = ",";
    private static final String AUTHORIZER_NAME = "authorizer";
    private static final String AUTHORIZER_PATH = "/" + AUTHORIZER_NAME;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String EMPTY_STRING = "";
    @JsonProperty(HEADERS_FIELD)
    private Map<String, String> headers;
    @JsonProperty(PATH_FIELD)
    private String path;
    @JsonProperty(PATH_PARAMETERS_FIELD)
    private Map<String, String> pathParameters;
    @JsonProperty(QUERY_STRING_PARAMETERS_FIELD)
    private Map<String, String> queryParameters;
    @JsonProperty(MULTI_VALUE_QUERY_STRING_PARAMETERS_FIELD)
    private Map<String, List<String>> multiValueQueryStringParameters;
    @JsonProperty(REQUEST_CONTEXT_FIELD)
    private JsonNode requestContext;
    @JsonProperty(METHOD_ARN_FIELD)
    private String methodArn;

    @JsonAnySetter
    private Map<String, Object> otherProperties;

    private RequestInfo() {
        this.headers = new HashMap<>();
        this.pathParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
        this.multiValueQueryStringParameters = new HashMap<>();
        this.otherProperties = new LinkedHashMap<>(); // ordinary HashMap and ConcurrentHashMap fail.
        this.requestContext = defaultRestObjectMapper.createObjectNode();
    }

    public static RequestInfo fromRequest(InputStream requestStream) throws ApiIoException {
        String inputString = IoUtils.streamToString(requestStream);
        return fromString(inputString);
    }

    public static RequestInfo fromString(String inputString) throws ApiIoException {
        return new ApiMessageParser<>(mapper).getRequestInfo(inputString);
    }

    @Deprecated(forRemoval = true)
    public String getHeader(String header) {
        return getHeaderOptional(header).orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_HEADERS + header));
    }

    @JsonIgnore
    public Optional<String> getHeaderOptional(String header) {
        return getHeaders().entrySet().stream()
                   .filter(entry -> entry.getKey().equalsIgnoreCase(header))
                   .findFirst()
                   .map(Map.Entry::getValue);
    }

    @JsonIgnore
    public String getAuthHeader() {
        return getHeaderOptional(HttpHeaders.AUTHORIZATION).orElseThrow(
            () -> new IllegalArgumentException(MISSING_FROM_HEADERS + HttpHeaders.AUTHORIZATION));
    }

    @JsonIgnore
    public Optional<String> getBearerToken() {
        return getHeaderOptional(HttpHeaders.AUTHORIZATION)
                   .map(authorizationHeader -> authorizationHeader.replaceFirst(BEARER_PREFIX, EMPTY_STRING));
    }

    @JsonIgnore
    public String getQueryParameter(String parameter) throws BadRequestException {
        return getQueryParameterOpt(parameter)
                   .orElseThrow(() -> new BadRequestException(MISSING_FROM_QUERY_PARAMETERS + parameter));
    }

    @JsonIgnore
    public List<String> getMultiValueQueryParameter(String parameter) {
        return Optional.ofNullable(getMultiValueQueryStringParameters().get(parameter))
                   .orElse(List.of());
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
        return getRequestContextParameterOpt(jsonPointer).orElseThrow(
            () -> new IllegalArgumentException(MISSING_FROM_REQUEST_CONTEXT + jsonPointer.toString()));
    }

    /**
     * Get request context parameter. The root node is the {@link RequestInfoConstants#REQUEST_CONTEXT_FIELD} node of
     * the {@link RequestInfo} class.
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

    public Map<String, List<String>> getMultiValueQueryStringParameters() {
        return multiValueQueryStringParameters;
    }

    public void setQueryParameters(Map<String, String> queryParameters) {
        this.queryParameters = nonNullMap(queryParameters);
    }

    public void setMultiValueQueryStringParameters(Map<String, List<String>> multiValueQueryStringParameters) {
        this.multiValueQueryStringParameters = nonNullMap(multiValueQueryStringParameters);
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
        return new UriWrapper(HTTPS, getDomainName()).addChild(getPath())
                   .addQueryParameters(getQueryParameters())
                   .getUri();
    }

    @JsonIgnore
    public String getDomainName() {
        return attempt(() -> this.getRequestContext().get(DOMAIN_NAME_FIELD).asText()).orElseThrow();
    }

    public boolean userIsAuthorized(AccessRight accessRight) {
        return getAccessRights().contains(accessRight) || handleAuthorizationFailure();
    }

    public boolean isGatewayAuthorized() {
        return getRequestContext().has(AUTHORIZER_NAME) && getRequestContext().at(AUTHORIZER_PATH).isObject();
    }

    private boolean handleAuthorizationFailure() {
        logger.warn(AUTHORIZATION_FAILURE_WARNING);
        return false;
    }

    public List<AccessRight> getAccessRights() {
        return new ArrayList<>(fetchAccessRights().orElse(Collections.emptyList()));
    }

    private Optional<List<AccessRight>> fetchAccessRights() {
        return fetchUserInfo().map(CognitoUserInfo::getAccessRights).map(this::parseAccessRights);
    }

    private Optional<CognitoUserInfo> fetchUserInfo() {
        if (isGatewayAuthorized()) {
            var claims = getRequestContext().at(CLAIMS_PATH);
            return claims.isObject() ? Optional.of(CognitoUserInfo.fromString(claims.toString())) : Optional.empty();
        } else {
            return getBearerToken().map(this::getCognitoUserInfoFromToken);
        }
    }

    private CognitoUserInfo getCognitoUserInfoFromToken(String token) {
        var claims = JWT.decode(token).getClaims().entrySet().stream()
                         .collect(Collectors.toMap(
                             Entry::getKey,
                             entry -> extractClaimValue(entry.getValue())
                         ));

        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(claims))
                   .map(CognitoUserInfo::fromString).orElseThrow();
    }

    private static String extractClaimValue(Claim value) {
        return Optional.of(value)
                   .map(Claim::asString)
                   .orElseGet(value::toString);
    }

    private List<AccessRight> parseAccessRights(String value) {
        return Arrays.stream(value.split(ELEMENTS_DELIMITER))
                   .filter(array -> !StringUtils.isEmpty(array))
                   .map(AccessRight::fromPersistedStringOptional)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .toList();
    }

    @JacocoGenerated
    @JsonIgnore
    @Deprecated(forRemoval = true)
    public URI getCustomerId() throws UnauthorizedException {
        return getCurrentCustomer();
    }

    @Deprecated(since = "1.25.5")
    @JacocoGenerated
    @JsonIgnore
    public String getNvaUsername() throws UnauthorizedException {
        return getUserName();
    }

    @JsonIgnore
    public String getUserName() throws UnauthorizedException {
        return fetchUserName().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public String getCognitoUsername() throws UnauthorizedException {
        return fetchCognitoUsername().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public URI getIssuer() throws UnauthorizedException {
        return fetchIssuer().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public ViewingScope getViewingScope() throws UnauthorizedException {
        return fetchViewingScope().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public Optional<String> getFeideId() {
        return fetchFeideId();
    }

    @JsonIgnore
    public Optional<URI> getTopLevelOrgCristinId() {
        return fetchTopLevelOrgCristinId();
    }

    @JsonIgnore
    public Optional<String> getClientId() {
        if (isGatewayAuthorized()) {
            return getRequestContextParameterOpt(CLIENT_ID);
        } else {
            return getClaimFromToken(CLIENT_ID_CLAIM);
        }
    }

    @JsonIgnore
    public URI getCurrentCustomer() throws UnauthorizedException {
        return fetchCustomerId().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public List<URI> getAllowedCustomers() throws UnauthorizedException {
        return fetchAllowedCustomers().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public URI getPersonCristinId() throws UnauthorizedException {
        return fetchPersonCristinId().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public URI getPersonAffiliation() throws UnauthorizedException {
        return fetchPersonAffiliation().orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public String getPersonNin() {
        return fetchPersonNin().orElseThrow(IllegalStateException::new);
    }

    public boolean clientIsInternalBackend() {
        var scope = isGatewayAuthorized() ?
                        getRequestContextParameterOpt(SCOPES_CLAIM) : getClaimFromToken(SCOPE);

        return scope.map(value -> value.contains(BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)).orElse(false);
    }

    public boolean clientIsThirdParty() {
        var scope = isGatewayAuthorized() ?
                        getRequestContextParameterOpt(SCOPES_CLAIM) : getClaimFromToken(SCOPE);
        return scope.map(this::isThirdPartyScope).orElse(false);
    }

    private Boolean isThirdPartyScope(String scope) {
        return Arrays.stream(scope.split(COMMA))
                   .anyMatch(value -> value.startsWith(
                       THIRD_PARTY_SCOPE_PREFIX));
    }

    private Optional<String> getClaimFromToken(String claim) {
        return getBearerToken()
                   .map(token -> JWT.decode(token).getClaim(claim))
                   .map(Claim::asString);
    }

    private Optional<String> fetchFeideId() {
        return fetchUserInfo().map(CognitoUserInfo::getFeideId);
    }

    private Optional<URI> fetchTopLevelOrgCristinId() {
        return fetchUserInfo().map(CognitoUserInfo::getTopOrgCristinId);
    }

    private Optional<String> fetchUserName() {
        return fetchUserInfo().map(CognitoUserInfo::getUserName);
    }

    private Optional<String> fetchCognitoUsername() {
        return fetchUserInfo().map(CognitoUserInfo::getCognitoUsername);
    }

    private Optional<URI> fetchIssuer() {
        return fetchUserInfo().map(CognitoUserInfo::getIssuer).map(URI::create);
    }

    private Optional<ViewingScope> fetchViewingScope() {
        return fetchUserInfo().map(
            userInfo -> ViewingScope.from(userInfo.getViewingScopeIncluded(), userInfo.getViewingScopeExcluded()));
    }

    private Optional<URI> fetchPersonCristinId() {
        return fetchUserInfo().map(CognitoUserInfo::getPersonCristinId);
    }

    private Optional<URI> fetchPersonAffiliation() {
        return fetchUserInfo().map(CognitoUserInfo::getPersonAffiliation);
    }

    private Optional<String> fetchPersonNin() {
        return fetchUserInfo().map(CognitoUserInfo::getPersonNin);
    }

    private Optional<URI> fetchCustomerId() {
        return fetchUserInfo().map(CognitoUserInfo::getCurrentCustomer);
    }

    private Optional<List<URI>> fetchAllowedCustomers() {
        return fetchUserInfo().map(CognitoUserInfo::getAllowedCustomers)
                   .map(customers -> customers.split(COMMA))
                   .map(Arrays::stream)
                   .map(stream -> stream.map(URI::create).toList());
    }

    private <K, V> Map<K, V> nonNullMap(Map<K, V> map) {
        if (isNull(map)) {
            return new HashMap<>();
        }
        return map;
    }
}

