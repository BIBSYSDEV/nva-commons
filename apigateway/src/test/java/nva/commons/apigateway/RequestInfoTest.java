package nva.commons.apigateway;

import static no.unit.nva.auth.CognitoUserInfo.TOP_LEVEL_ORG_CRISTIN_ID_CLAIM;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.RequestInfo.AUTHORIZATION_FAILURE_WARNING;
import static nva.commons.apigateway.RequestInfo.REQUEST_CONTEXT_FIELD;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.stubs.FakeAuthServer;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestInfoTest {

    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String JSON_POINTER = "/authorizer/claims/key";
    public static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
                                                                       "eventWithUnknownRequestInfo.json");
    public static final Path EVENT_WITH_AUTH_HEADER = Path.of("apiGatewayMessages",
                                                              "event_with_auth_header.json");

    public static final String UNDEFINED_REQUEST_INFO_PROPERTY = "body";
    public static final String PATH_DELIMITER = "/";
    public static final int UNNECESSARY_ROOT_NODE = 0;
    public static final int FIRST_NODE = 0;

    public static final String DOMAIN_NAME_FOUND_IN_RESOURCE_FILE = "id.execute-api.us-east-1.amazonaws.com";
    public static final String PATH_FOUND_IN_RESOURCE_FILE = "my/path";
    public static final Map<String, String> QUERY_PARAMS_FOUND_IN_RESOURCE_FILE;
    public static final String AT = "@";
    private static final String API_GATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";

    private static final Path NULL_VALUES_FOR_MAPS = Path.of(API_GATEWAY_MESSAGES_FOLDER,
                                                             "mapParametersAreNull.json");
    private static final Path MISSING_MAP_VALUES = Path.of(API_GATEWAY_MESSAGES_FOLDER,
                                                           "missingRequestInfo.json");
    private static final Path AWS_SAMPLE_PROXY_EVENT = Path.of(API_GATEWAY_MESSAGES_FOLDER,
                                                               "awsSampleProxyEvent.json");
    private static final String HARDCODED_AUTH_HEADER = "Bearer THE_ACCESS_TOKEN";

    static {
        QUERY_PARAMS_FOUND_IN_RESOURCE_FILE = new TreeMap<>();
        QUERY_PARAMS_FOUND_IN_RESOURCE_FILE.put("parameter1", "value1");
        QUERY_PARAMS_FOUND_IN_RESOURCE_FILE.put("parameter2", "value");
    }

    private FakeAuthServer cognito;
    private HttpClient httpClient;
    private String userAccessToken;

    @BeforeEach
    public void init() {
        this.cognito = new FakeAuthServer();
        this.httpClient = WiremockHttpClient.create();
        this.userAccessToken = randomString();
    }

    @AfterEach
    public void close() {
        this.cognito.close();
    }

    @Test
    @DisplayName("RequestInfo can accept unknown fields")
    void requestInfoAcceptsUnknownsFields() throws JsonProcessingException {
        String requestInfoString = IoUtils.stringFromResources(EVENT_WITH_UNKNOWN_REQUEST_INFO);
        RequestInfo requestInfo = defaultRestObjectMapper.readValue(requestInfoString, RequestInfo.class);

        assertThat(requestInfo.getOtherProperties(), hasKey(UNDEFINED_REQUEST_INFO_PROPERTY));
    }

    @Test
    @DisplayName("RequestInfo initializes queryParameters to empty map when JSON object sets "
                 + "queryStringParameters to null")
    void requestInfoInitializesQueryParametersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getQueryParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when JSON object sets "
                 + "Headers to null")
    void requestInfoInitializesHeadersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getHeaders);
    }

    @Test
    @DisplayName("RequestInfo initializes pathParameters to empty map when JSON object sets "
                 + "pathParameters to null")
    void requestInfoInitializesPathParametersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getPathParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes requestContext to empty JsonNode when JSON object sets "
                 + "requestContext to null")
    void requestInfoInitializesRequestContextToEmptyJsonNodeWhenJsonObjectsSetsRequestContextToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getRequestContext);
    }

    @Test
    @DisplayName("RequestInfo initializes queryParameters to empty map queryStringParameters is missing")
    void requestInfoInitializesQueryParametersToEmptyMapWhenQueryStringParametersIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getQueryParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when header parameter is missing")
    void requestInfoInitializesHeadersToEmptyMapWhenHeadersParameterIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getHeaders);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when header parameter is missing")
    void requestInfoInitializesPathsParamsToEmptyMapWhenPathParametersParameterIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getPathParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes requestContext to empty JsonNode requestContext is missing")
    void requestInfoInitializesRequestContextToEmptyJsonNodeWhenRequestContextIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getRequestContext);
    }

    @Test
    void shouldReturnCustomerIdWhenRequestDoesNotContainCustomerIdButHasAccessTokenAndCognitoUserHasSelectedCustomer()
        throws UnauthorizedException {
        var expectedCurrentCustomer = randomUri();
        var cognitoUserEntry = createCognitoUserEntry(expectedCurrentCustomer, null);
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));

        RequestInfo requestInfo = createRequestInfoWithAccessToken();
        var actualCustomerId = requestInfo.getCurrentCustomer();
        assertThat(actualCustomerId, is(equalTo(expectedCurrentCustomer)));
    }

    @Test
    void shouldReturnThatUserHasAccessRightForSpecificCustomerWhenCognitoHasRespectiveEntry()
        throws UnauthorizedException {
        var usersCustomer = randomUri();
        var accessRights = Set.of(randomString(), randomString(), randomString());
        var accessRightsForCustomer = accessRights.stream()
            .map(right -> right + AT + usersCustomer)
            .collect(Collectors.toSet());
        var cognitoUserEntry = createCognitoUserEntry(usersCustomer, accessRightsForCustomer);
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        for (var accessRight : accessRights) {
            var userIsAuthorized = requestInfo.userIsAuthorized(accessRight);
            assertThat(userIsAuthorized, is(true));
        }
    }

    @Test
    void shouldReturnThatUserDoesNotHaveAccessRightForSpecificCustomerWhenCognitoDoesNotHaveRespectiveAccessRight()
        throws UnauthorizedException {
        var usersCustomer = randomUri();
        var accessRights = Set.of(randomString(), randomString(), randomString());
        var accessRightsForCustomer = accessRights.stream()
            .map(right -> right + AT + usersCustomer)
            .collect(Collectors.toSet());
        var cognitoUserEntry = CognitoUserInfo.builder()
            .withCurrentCustomer(usersCustomer)
            .withAccessRights(accessRightsForCustomer).build();

        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        var requestedAccessRight = randomString();
        var userIsAuthorized = requestInfo.userIsAuthorized(requestedAccessRight);
        assertThat(userIsAuthorized, is(false));
    }

    @Test
    void shouldReturnThatUserDoesNotHaveAccessRightForSpecificCustomerWhenUserDoesNotHaveAnyAccessRights() {
        var cognitoUserEntryWithoutAccessRights =
            CognitoUserInfo.builder().withCurrentCustomer(randomUri()).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutAccessRights));
        var requestInfo = createRequestInfoWithAccessToken();
        var requestedAccessRight = randomString();
        assertThat(requestInfo.userIsAuthorized(requestedAccessRight), is(false));
    }

    @Test
    void shouldReturnThatUserIsAuthorizedWhenRequestInfoContainsACustomerIdAndTheRequestedAccessRight()
        throws JsonProcessingException {
        var usersCustomer = randomUri();
        var usersAccessRights = List.of(randomString(), randomString());
        RequestInfo requestInfo = createRequestInfoForOfflineAuthorization(usersAccessRights, usersCustomer);
        for (var accessRight : usersAccessRights) {
            assertThat(requestInfo.userIsAuthorized(accessRight), is(true));
        }
    }

    @Test
    void shouldReturnNotAuthorizedWhenRequestInfoDoesNotContainTheRequestedAccessRightAndCheckIsPerformedOffline()
        throws JsonProcessingException {
        var userCustomer = randomUri();
        var usersAccessRights = List.of(randomAccessRight(userCustomer), randomAccessRight(userCustomer));
        var requestInfo = createRequestInfoForOfflineAuthorization(usersAccessRights, userCustomer);
        var notAllocatedAccessRight = randomString();
        assertThat(requestInfo.userIsAuthorized(notAllocatedAccessRight), is(false));
    }

    @Test
    void shouldReturnNotAuthorizedWhenRequestInfoDoesNotContainAccessRightsAndCheckIsPerformedOffline()
        throws JsonProcessingException {
        var userCustomer = randomUri();
        var requestInfo = requestInfoWithCustomerId(userCustomer);
        var notAllocatedAccessRight = randomAccessRight(userCustomer);
        assertThat(requestInfo.userIsAuthorized(notAllocatedAccessRight), is(false));
    }

    @Test
    @DisplayName("should return current customer from cognito groups when online cognito information in unavailable")
    void shouldReturnCurrentCustomerFromCognitoGroupsWhenOnlineCognitoInformationIsUnavailable()
        throws UnauthorizedException, JsonProcessingException {
        var currentCustomer = randomUri();
        var requestInfo = requestInfoWithCustomerId(currentCustomer);
        assertThat(requestInfo.getCurrentCustomer(), is(equalTo(currentCustomer)));
    }

    @Test
    void canGetValueFromRequestContext() throws JsonProcessingException {

        Map<String, Map<String, Map<String, Map<String, String>>>> map = Map.of(
            REQUEST_CONTEXT_FIELD, Map.of(
                AUTHORIZER, Map.of(
                    CLAIMS, Map.of(
                        KEY, VALUE
                    )
                )
            )
        );

        RequestInfo requestInfo = defaultRestObjectMapper
            .readValue(defaultRestObjectMapper.writeValueAsString(map), RequestInfo.class);

        JsonPointer jsonPointer = JsonPointer.compile(JSON_POINTER);
        JsonNode jsonNode = requestInfo.getRequestContext().at(jsonPointer);

        assertFalse(jsonNode.isMissingNode());
        assertEquals(VALUE, jsonNode.textValue());
    }

    @Test
    void shouldReturnRequestUriFromRequestInfo() throws ApiIoException {
        RequestInfo requestInfo = extractAccessRightsFromApiGatewayEvent();

        URI requestUri = requestInfo.getRequestUri();
        URI expectedRequestUri = new UriWrapper(RequestInfo.HTTPS, DOMAIN_NAME_FOUND_IN_RESOURCE_FILE)
            .addChild(PATH_FOUND_IN_RESOURCE_FILE)
            .addQueryParameters(QUERY_PARAMS_FOUND_IN_RESOURCE_FILE)
            .getUri();

        assertThat(requestUri, is(equalTo(expectedRequestUri)));
    }

    @Test
    void shouldReturnNvaUsernameFromCognitoWhenUserHasSelectedCustomerAndClaimInNotAvailableOffline()
        throws UnauthorizedException {
        var expectedUsername = randomString();
        var cognitoUserEntry = CognitoUserInfo.builder().withNvaUsername(expectedUsername).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        var actualUsername = requestInfo.getNvaUsername();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void shouldReturnNvaUsernameWhenClaimInAvailableOffline() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder().withNvaUsername(randomString()).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();

        var expectedUsername = randomString();
        injectNvaUserNameInRequestInfo(requestInfo, expectedUsername);

        var actualUsername = requestInfo.getNvaUsername();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenNvaUsernameIsNotAvailable() {
        var cognitoUserEntryWithoutNvaUsername = CognitoUserInfo.builder().build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutNvaUsername));
        var requestInfo = createRequestInfoWithAccessToken();
        assertThrows(UnauthorizedException.class, requestInfo::getNvaUsername);
    }

    @Test
    void shouldReturnPersonCristinIdFromCognitoWhenUserHasSelectedCustomerAndClaimInNotAvailableOffline()
        throws UnauthorizedException {
        var expectedPersonCristinId = randomUri();
        var cognitoUserEntry =
            CognitoUserInfo.builder().withPersonCristinId(expectedPersonCristinId).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        var actualPersonCristinId = requestInfo.getPersonCristinId();
        assertThat(actualPersonCristinId, is(equalTo(expectedPersonCristinId)));
    }

    @Test
    void shouldReturnPersonCristinIdWhenClaimInAvailableOffline() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonCristinId(randomUri()).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        var expectedPersonCristinIdDifferentFromCognito = randomUri();
        injectPersonCristinIdInRequestInfo(requestInfo, expectedPersonCristinIdDifferentFromCognito);
        var actualPersonCristinId = requestInfo.getPersonCristinId();
        assertThat(actualPersonCristinId, is(equalTo(expectedPersonCristinIdDifferentFromCognito)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenPersonCristinIdIsNotAvailable() {
        var cognitoUserEntryWithoutPersonCristinId = CognitoUserInfo.builder().build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutPersonCristinId));
        var requestInfo = createRequestInfoWithAccessToken();
        assertThrows(UnauthorizedException.class, requestInfo::getPersonCristinId);
    }

    @Test
    void shouldReadCognitoUriFromEnvByDefaultThatContainsValidPersonCristinId()
        throws JsonProcessingException, UnauthorizedException {
        var requestInfoString = IoUtils.stringFromResources(EVENT_WITH_AUTH_HEADER);
        var requestInfo = dtoObjectMapper.readValue(requestInfoString, RequestInfo.class);
        assertNotNull(requestInfo.getPersonCristinId());
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenCustomerIdIsNotAvailable() {
        var cognitoUserEntryWithoutCustomerId = CognitoUserInfo.builder().build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutCustomerId));
        var requestInfo = createRequestInfoWithAccessToken();
        assertThrows(UnauthorizedException.class, requestInfo::getCurrentCustomer);
    }

    @Test
    void shouldReturnTopLevelOrgCristinIdWhenCurrentCustomerHasBeenSelectedForPerson() {
        var topOrgCristinId = randomUri();
        var cognitoUserEntry = CognitoUserInfo
            .builder()
            .withTopOrgCristinId(topOrgCristinId)
            .build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        assertThat(requestInfo.getTopLevelOrgCristinId().orElseThrow(), is(equalTo(topOrgCristinId)));
    }

    @Test
    void shouldReturnTopLevelOrgCristinIdWhenRequestsAuthorizerNodeContainsCorrespondingClaim()
        throws JsonProcessingException {
        var topOrgCristinId = randomUri();
        var requestInfo = requestInfoWithAuthorizerClaim(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM, topOrgCristinId.toString());
        assertThat(requestInfo.getTopLevelOrgCristinId().orElseThrow(), is(equalTo(topOrgCristinId)));
    }

    @Test
    void shouldReturnAuthHeaderWhenAuthHeaderIsAvailable() throws JsonProcessingException {
        var requestInfoString = IoUtils.stringFromResources(EVENT_WITH_AUTH_HEADER);
        var requestInfo = dtoObjectMapper.readValue(requestInfoString, RequestInfo.class);
        assertThat(requestInfo.getAuthHeader(), is(equalTo(HARDCODED_AUTH_HEADER)));
    }

    @Test
    void shouldReadCognitoUriFromEnvByDefault() throws JsonProcessingException {
        var requestInfoString = IoUtils.stringFromResources(EVENT_WITH_AUTH_HEADER);
        var requestInfo = dtoObjectMapper.readValue(requestInfoString, RequestInfo.class);
        assertThrows(UnauthorizedException.class, requestInfo::getNvaUsername);
    }

    @Test
    void shouldLogWarningWhenAuthenticationFails() throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).build();
        var requestInfo = RequestInfo.fromRequest(request);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        requestInfo.userIsAuthorized(randomString());
        assertThat(logger.getMessages(), containsString(AUTHORIZATION_FAILURE_WARNING));
    }

    private String randomAccessRight(URI usersCustomer) {
        return new AccessRight(randomString(), usersCustomer).toString();
    }

    private RequestInfo requestInfoWithCustomerId(URI userCustomer) throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper)
            .withCustomerId(userCustomer)
            .build();
        return RequestInfo.fromRequest(request);
    }

    private RequestInfo requestInfoWithAuthorizerClaim(String claimName, String claim)
        throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper)
            .withAuthorizerClaim(claimName, claim)
            .build();
        return RequestInfo.fromRequest(request);
    }

    private CognitoUserInfo createCognitoUserEntry(URI usersCustomer, Set<String> accessRightsForCustomer) {
        return CognitoUserInfo.builder()
            .withCurrentCustomer(usersCustomer)
            .withAccessRights(accessRightsForCustomer).build();
    }

    private void injectNvaUserNameInRequestInfo(RequestInfo requestInfo, String expectedUsername) {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.NVA_USERNAME_CLAIM, expectedUsername);
        authorizer.set("claims", claims);
        requestContext.set("authorizer", authorizer);
        requestInfo.setRequestContext(requestContext);
    }

    private void injectPersonCristinIdInRequestInfo(RequestInfo requestInfo, URI expectedPersonCristinId) {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_CRISTIN_ID_CLAIM, expectedPersonCristinId.toString());
        authorizer.set("claims", claims);
        requestContext.set("authorizer", authorizer);
        requestInfo.setRequestContext(requestContext);
    }

    private RequestInfo createRequestInfoWithAccessToken() {
        var requestInfo = new RequestInfo(httpClient, () -> cognito.getServerUri());
        requestInfo.setHeaders(Map.of(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken)));
        return requestInfo;
    }

    private RequestInfo createRequestInfoForOfflineAuthorization(List<String> usersAccessRights, URI userCustomer)
        throws JsonProcessingException {
        var requestStream = new HandlerRequestBuilder<Void>(dtoObjectMapper)
            .withCustomerId(userCustomer)
            .withAccessRights(userCustomer, usersAccessRights.toArray(String[]::new))
            .build();
        return RequestInfo.fromRequest(requestStream);
    }

    private String bearerToken(String userAccessToken) {
        return "Bearer " + userAccessToken;
    }

    private RequestInfo extractAccessRightsFromApiGatewayEvent()
        throws ApiIoException {
        String event = IoUtils.stringFromResources(RequestInfoTest.AWS_SAMPLE_PROXY_EVENT);
        ApiMessageParser<String> apiMessageParser = new ApiMessageParser<>();
        return apiMessageParser.getRequestInfo(event);
    }

    private List<SimpleEntry<String, ObjectNode>> createNodesForEachPathElement(JsonPointer jsonPointer) {
        List<SimpleEntry<String, ObjectNode>> nodes = createListWithEmptyObjectNodes(jsonPointer);
        nodes.remove(UNNECESSARY_ROOT_NODE);
        return nodes;
    }

    private void nestNodes(List<SimpleEntry<String, ObjectNode>> nodes) {
        for (int i = 0; i < lastIndex(nodes); i++) {
            SimpleEntry<String, ObjectNode> currentEntry = nodes.get(i);
            SimpleEntry<String, ObjectNode> nextEntry = nodes.get(i + 1);
            addNextEntryAsChildToCurrentEntry(currentEntry, nextEntry);
        }
    }

    private void insertTextValueToLeafNode(String value, SimpleEntry<String, ObjectNode> lastEntry) {
        lastEntry.getValue().put(lastEntry.getKey(), value);
    }

    private void addNextEntryAsChildToCurrentEntry(SimpleEntry<String, ObjectNode> currentEntry,
                                                   SimpleEntry<String, ObjectNode> nextEntry) {
        ObjectNode currentNode = currentEntry.getValue();
        currentNode.set(currentEntry.getKey(), nextEntry.getValue());
    }

    private List<SimpleEntry<String, ObjectNode>> createListWithEmptyObjectNodes(JsonPointer jsonPointer) {
        return Arrays.stream(jsonPointer.toString()
                                 .split(PATH_DELIMITER))
            .map(nodeName -> new SimpleEntry<>(nodeName, defaultRestObjectMapper.createObjectNode()))
            .collect(Collectors.toList());
    }

    private int lastIndex(List<SimpleEntry<String, ObjectNode>> nodes) {
        return nodes.size() - 1;
    }

    private void checkForNonNullMap(Path resourceFile, Function<RequestInfo, Object> getObject)
        throws JsonProcessingException {
        String apiGatewayEvent = IoUtils.stringFromResources(resourceFile);
        RequestInfo requestInfo = defaultRestObjectMapper.readValue(apiGatewayEvent, RequestInfo.class);
        assertNotNull(getObject.apply(requestInfo));
    }
}

