package nva.commons.apigateway;

import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.auth.CognitoUserInfo.ELEMENTS_DELIMITER;
import static nva.commons.apigateway.RequestInfoConstants.AUTHORIZATION_FAILURE_WARNING;
import static nva.commons.apigateway.RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE;
import static nva.commons.apigateway.RequestInfoConstants.CLIENT_ID;
import static nva.commons.apigateway.RequestInfoConstants.CUSTOMER_ID;
import static nva.commons.apigateway.RequestInfoConstants.DEFAULT_COGNITO_URI;
import static nva.commons.apigateway.RequestInfoConstants.DOMAIN_NAME_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.EXTERNAL_USER_POOL_URI;
import static nva.commons.apigateway.RequestInfoConstants.FEIDE_ID;
import static nva.commons.apigateway.RequestInfoConstants.HEADERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.ISS;
import static nva.commons.apigateway.RequestInfoConstants.METHOD_ARN_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_HEADERS;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_PATH_PARAMETERS;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_QUERY_PARAMETERS;
import static nva.commons.apigateway.RequestInfoConstants.MISSING_FROM_REQUEST_CONTEXT;
import static nva.commons.apigateway.RequestInfoConstants.MULTI_VALUE_QUERY_STRING_PARAMETERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.PATH_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.PATH_PARAMETERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.PERSON_AFFILIATION;
import static nva.commons.apigateway.RequestInfoConstants.PERSON_CRISTIN_ID;
import static nva.commons.apigateway.RequestInfoConstants.PERSON_GROUPS;
import static nva.commons.apigateway.RequestInfoConstants.PERSON_NIN;
import static nva.commons.apigateway.RequestInfoConstants.QUERY_STRING_PARAMETERS_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.REQUEST_CONTEXT_FIELD;
import static nva.commons.apigateway.RequestInfoConstants.SCOPES_CLAIM;
import static nva.commons.apigateway.RequestInfoConstants.TOP_LEVEL_ORG_CRISTIN_ID;
import static nva.commons.apigateway.RequestInfoConstants.USER_NAME;
import static nva.commons.apigateway.RequestInfoConstants.VIEWING_SCOPE_EXCLUDED;
import static nva.commons.apigateway.RequestInfoConstants.VIEWING_SCOPE_INCLUDED;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.paths.UriWrapper.HTTPS;
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
import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.auth.FetchUserInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public class RequestInfo {

    public static final String ERROR_FETCHING_COGNITO_INFO = "Could not fetch user information from Cognito:{}";
    private static final Logger logger = LoggerFactory.getLogger(RequestInfo.class);
    private static final ObjectMapper mapper = defaultRestObjectMapper;
    private HttpClient httpClient;
    private final Supplier<URI> cognitoUri;
    private final Supplier<URI> e2eTestsUserInfoUri;
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

    public RequestInfo(HttpClient httpClient, Supplier<URI> cognitoUri, Supplier<URI> e2eTestsUserInfoUri) {
        this.httpClient = httpClient;
        this.cognitoUri = cognitoUri;
        this.e2eTestsUserInfoUri = e2eTestsUserInfoUri;
    }

    private RequestInfo() {
        this.headers = new HashMap<>();
        this.pathParameters = new HashMap<>();
        this.queryParameters = new HashMap<>();
        this.multiValueQueryStringParameters = new HashMap<>();
        this.otherProperties = new LinkedHashMap<>(); // ordinary HashMap and ConcurrentHashMap fail.
        this.requestContext = defaultRestObjectMapper.createObjectNode();
        this.httpClient = HttpClient.newBuilder().build();
        this.cognitoUri = DEFAULT_COGNITO_URI;
        this.e2eTestsUserInfoUri = RequestInfoConstants.E2E_TESTING_USER_INFO_ENDPOINT;
    }

    public static RequestInfo fromRequest(InputStream requestStream, HttpClient httpClient) throws ApiIoException {
        String inputString = IoUtils.streamToString(requestStream);
        return fromString(inputString, httpClient);
    }

    public static RequestInfo fromString(String inputString, HttpClient httpClient) throws ApiIoException {
        var requestInfo = new ApiMessageParser<>(mapper).getRequestInfo(inputString);
        requestInfo.setHttpClient(httpClient);
        return requestInfo;
    }

    @JsonIgnore
    public String getHeader(String header) {
        return getHeaders().entrySet().stream()
                   .filter(entry -> entry.getKey().equalsIgnoreCase(header))
                   .findFirst()
                   .map(Map.Entry::getValue)
                   .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_HEADERS + header));
    }

    @JsonIgnore
    public String getAuthHeader() {
        return getHeader(HttpHeaders.AUTHORIZATION);
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

    @JacocoGenerated
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
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
        return checkAuthorizationOnline(accessRight)
               || checkAuthorizationFromContext(accessRight);
    }

    public List<AccessRight> getAccessRights() {
        return extractAccessRightsForTests().or(this::fetchAccessRights).orElse(Collections.emptyList());
    }

    private Optional<List<AccessRight>> fetchAccessRights() {
        return fetchUserInfo().map(CognitoUserInfo::getAccessRights).map(this::parseAccessRights);
    }

    private List<AccessRight> parseAccessRights(String value) {
        return Arrays.stream(value.split(ELEMENTS_DELIMITER))
                   .filter(array -> !StringUtils.isEmpty(array))
                   .map(AccessRight::fromPersistedString)
                   .collect(Collectors.toList());
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
        return extractUserNameForTests().or(this::fetchUserName).orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public ViewingScope getViewingScope() throws UnauthorizedException {
        return extractViewingScopeForTests().or(this::fetchViewingScope).orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public Optional<String> getFeideId() {
        return extractFeideIdForTests().or(this::fetchFeideId);
    }

    @JsonIgnore
    public Optional<URI> getTopLevelOrgCristinId() {
        return extractTopLevelOrgForTests().or(this::fetchTopLevelOrgCristinId);
    }

    @JsonIgnore
    public Optional<String> getClientId() {
        return getRequestContextParameterOpt(CLIENT_ID);
    }

    @JsonIgnore
    public URI getCurrentCustomer() throws UnauthorizedException {
        return fetchCustomerId().or(this::fetchCustomerIdFromContext)
                   .or(this::extractCustomerIdForTests)
                   .orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public URI getPersonCristinId() throws UnauthorizedException {
        return extractPersonCristinIdForTests().or(this::fetchPersonCristinId)
                   .orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public URI getPersonAffiliation() throws UnauthorizedException {
        return extractPersonAffiliationForTests().or(this::fetchPersonAffiliation)
                   .orElseThrow(UnauthorizedException::new);
    }

    @JsonIgnore
    public String getPersonNin() {
        return extractPersonNinForTests().or(this::fetchPersonNin).orElseThrow(IllegalStateException::new);
    }

    public boolean clientIsInternalBackend() {
        return getRequestContextParameterOpt(SCOPES_CLAIM).map(
            value -> value.contains(BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE)).orElse(false);
    }

    public boolean clientIsThirdParty() {
        return getRequestContextParameterOpt(ISS).map(
            value -> value.equals(EXTERNAL_USER_POOL_URI.get())
        ).orElse(false);
    }

    private Optional<String> fetchFeideId() {
        return fetchUserInfo().map(CognitoUserInfo::getFeideId);
    }

    private Optional<String> extractFeideIdForTests() {
        return getRequestContextParameterOpt(FEIDE_ID);
    }

    private Optional<URI> fetchCustomerIdFromContext() {
        return getRequestContextParameterOpt(PERSON_GROUPS).stream()
                   .flatMap(AccessRightEntry::fromCsv)
                   .map(AccessRightEntry::getCustomerId)
                   .distinct()
                   .collect(SingletonCollector.tryCollect())
                   .toOptional();
    }

    private Optional<URI> extractCustomerIdForTests() {
        return getRequestContextParameterOpt(CUSTOMER_ID).map(URI::create);
    }

    private Optional<URI> extractTopLevelOrgForTests() {
        return getRequestContextParameterOpt(TOP_LEVEL_ORG_CRISTIN_ID).map(URI::create);
    }

    private Optional<URI> fetchTopLevelOrgCristinId() {
        return fetchUserInfo().map(CognitoUserInfo::getTopOrgCristinId);
    }

    private void logOnlineFetchResult(Failure<CognitoUserInfo> fail) {
        logger.warn(ERROR_FETCHING_COGNITO_INFO, ExceptionUtils.stackTraceInSingleLine(fail.getException()));
    }

    private Optional<ViewingScope> extractViewingScopeForTests() {
        var includedString = getRequestContextParameterOpt(VIEWING_SCOPE_INCLUDED);
        var excludedString = getRequestContextParameterOpt(VIEWING_SCOPE_EXCLUDED);

        if (includedString.isEmpty() || excludedString.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ViewingScope.from(includedString.get(), excludedString.get()));
    }

    private Optional<String> extractUserNameForTests() {
        return getRequestContextParameterOpt(USER_NAME);
    }

    private Optional<String> fetchUserName() {
        return fetchUserInfo().map(CognitoUserInfo::getUserName);
    }

    private Optional<ViewingScope> fetchViewingScope() {
        return fetchUserInfo().map(u -> ViewingScope.from(u.getViewingScopeIncluded(), u.getViewingScopeExcluded()));
    }

    private Optional<URI> extractPersonCristinIdForTests() {
        return getRequestContextParameterOpt(PERSON_CRISTIN_ID).map(URI::create);
    }

    private Optional<URI> extractPersonAffiliationForTests() {
        return getRequestContextParameterOpt(PERSON_AFFILIATION).map(URI::create);
    }

    private Optional<URI> fetchPersonCristinId() {
        return fetchUserInfo().map(CognitoUserInfo::getPersonCristinId);
    }

    private Optional<URI> fetchPersonAffiliation() {
        return fetchUserInfo().map(CognitoUserInfo::getPersonAffiliation);
    }

    private Optional<String> extractPersonNinForTests() {
        return getRequestContextParameterOpt(PERSON_NIN);
    }

    private Optional<String> fetchPersonNin() {
        return fetchUserInfo().map(CognitoUserInfo::getPersonNin);
    }

    private boolean checkAuthorizationFromContext(AccessRight accessRight) {
        return attempt(this::getCurrentCustomer)
                   .map(currentCustomer -> new AccessRightEntry(accessRight, currentCustomer))
                   .map(
                       requiredAccessRight -> fetchAvailableAccessRightsFromContext().anyMatch(
                           requiredAccessRight::equals))
                   .orElse(fail -> handleAuthorizationFailure());
    }

    private boolean handleAuthorizationFailure() {
        logger.warn(AUTHORIZATION_FAILURE_WARNING);
        return false;
    }

    private Stream<AccessRightEntry> fetchAvailableAccessRightsFromContext() {
        return getRequestContextParameterOpt(PERSON_GROUPS).stream().flatMap(AccessRightEntry::fromCsv);
    }

    private Boolean checkAuthorizationOnline(AccessRight accessRight) {
        var accessRightAtCustomer = fetchCustomerId().map(
            customer -> new AccessRightEntry(accessRight, customer));

        var availableRights = fetchAvailableRights();
        return accessRightAtCustomer.map(availableRights::contains).orElse(false);
    }

    private List<AccessRightEntry> fetchAvailableRights() {
        var userInfo = fetchUserInfo();
        return userInfo
                   .map(CognitoUserInfo::getAccessRights)
                   .map(accessRightEntryStr -> AccessRightEntry.fromCsvForCustomer(accessRightEntryStr, userInfo.get()
                                                                                                            .getCurrentCustomer()))
                   .map(stream -> stream.collect(Collectors.toList()))
                   .orElseGet(Collections::emptyList);
    }

    private Optional<List<AccessRight>> extractAccessRightsForTests() {
        return getRequestContextParameterOpt(PERSON_GROUPS)
                   .map(AccessRightEntry::fromCsv)
                   .map(stream -> stream.map(AccessRightEntry::getAccessRight))
                   .map(stream -> stream.collect(Collectors.toList()));
    }

    private Optional<CognitoUserInfo> fetchUserInfo() {
        return attempt(() -> fetchUserInfo(cognitoUri)).or(() -> fetchUserInfo(e2eTestsUserInfoUri))
                   .toOptional(this::logOnlineFetchResult);
    }

    private CognitoUserInfo fetchUserInfo(Supplier<URI> cognitoUri) {
        var userInfo = new FetchUserInfo(httpClient, cognitoUri, extractAuthorizationHeader());
        return userInfo.fetch();
    }

    private String extractAuthorizationHeader() {
        return this.getHeader(HttpHeaders.AUTHORIZATION);
    }

    private Optional<URI> fetchCustomerId() {
        return fetchUserInfo().map(CognitoUserInfo::getCurrentCustomer);
    }

    private <K, V> Map<K, V> nonNullMap(Map<K, V> map) {
        if (isNull(map)) {
            return new HashMap<>();
        }
        return map;
    }
}

