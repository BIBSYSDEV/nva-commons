package nva.commons.apigateway;

import static no.unit.nva.auth.OAuthConstants.OAUTH_USER_INFO;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.RequestInfo.ERROR_FETCHING_COGNITO_INFO;
import static nva.commons.apigateway.RequestInfoConstants.AUTHORIZATION_FAILURE_WARNING;
import static nva.commons.apigateway.RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE;
import static nva.commons.apigateway.RequestInfoConstants.REQUEST_CONTEXT_FIELD;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.stubs.FakeAuthServer;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(PER_METHOD)
class RequestInfoTest {

    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String JSON_POINTER = "/authorizer/claims/key";
    public static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
                                                                       "eventWithUnknownRequestInfo.json");
    public static final Path EVENT_WITH_AUTH_HEADER = Path.of("apiGatewayMessages", "event_with_auth_header.json");
    public static final String FEIDE_ID_CLAIM = "custom:feideId";
    public static final String UNDEFINED_REQUEST_INFO_PROPERTY = "body";
    public static final String DOMAIN_NAME_FOUND_IN_RESOURCE_FILE = "id.execute-api.us-east-1.amazonaws.com";
    public static final String PATH_FOUND_IN_RESOURCE_FILE = "my/path";
    public static final Map<String, String> QUERY_PARAMS_FOUND_IN_RESOURCE_FILE;
    public static final String AT = "@";
    public static final String LOG_STRING_INTERPOLATION = "{}";
    public static final String EMPTY_STRING = "";
    private static final String API_GATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    private static final Path NULL_VALUES_FOR_MAPS = Path.of(API_GATEWAY_MESSAGES_FOLDER, "mapParametersAreNull.json");
    private static final Path MISSING_MAP_VALUES = Path.of(API_GATEWAY_MESSAGES_FOLDER, "missingRequestInfo.json");
    private static final Path AWS_SAMPLE_PROXY_EVENT = Path.of(API_GATEWAY_MESSAGES_FOLDER, "awsSampleProxyEvent.json");
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
    @DisplayName("RequestInfo initializes headers to empty map when JSON object sets " + "Headers to null")
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
    void requestInfoInitializesHeadersToEmptyMapWhenHeadersParameterIsMissing() throws JsonProcessingException {
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

        RequestInfo requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualCustomerId = requestInfo.getCurrentCustomer();
        assertThat(actualCustomerId, is(equalTo(expectedCurrentCustomer)));
    }

    @Test
    void shouldReturnUserInfoWhenRequestContainsAccessTokenWithAdminScope() throws UnauthorizedException {
        var expectedCurrentCustomer = randomUri();
        var cognitoUserEntry = createCognitoUserEntry(expectedCurrentCustomer, null);
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));

        RequestInfo requestInfo = createRequestInfoWithAccessTokenThatHasAdminScope();
        var actualCustomerId = requestInfo.getCurrentCustomer();
        assertThat(actualCustomerId, is(equalTo(expectedCurrentCustomer)));
    }

    @Test
    void shouldReturnThatUserHasAccessRightForSpecificCustomerWhenCognitoHasRespectiveEntry() {
        var usersCustomer = randomUri();
        var accessRights = Set.of(randomString(), randomString(), randomString());
        var accessRightsForCustomer = accessRights.stream()
                                          .map(right -> right + AT + usersCustomer)
                                          .collect(Collectors.toSet());
        var cognitoUserEntry = createCognitoUserEntry(usersCustomer, accessRightsForCustomer);
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        for (var accessRight : accessRights) {
            var userIsAuthorized = requestInfo.userIsAuthorized(accessRight);
            assertThat(userIsAuthorized, is(true));
        }
    }

    @Test
    void shouldReturnThatUserDoesNotHaveAccessRightForSpecificCustomerWhenCognitoDoesNotHaveRespectiveAccessRight() {
        var usersCustomer = randomUri();
        var accessRights = Set.of(randomString(), randomString(), randomString());
        var accessRightsForCustomer = accessRights.stream()
                                          .map(right -> right + AT + usersCustomer)
                                          .collect(Collectors.toSet());
        var cognitoUserEntry = CognitoUserInfo.builder()
                                   .withCurrentCustomer(usersCustomer)
                                   .withAccessRights(accessRightsForCustomer)
                                   .build();

        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var requestedAccessRight = randomString();
        var userIsAuthorized = requestInfo.userIsAuthorized(requestedAccessRight);
        assertThat(userIsAuthorized, is(false));
    }

    @Test
    void shouldReturnThatUserDoesNotHaveAccessRightForSpecificCustomerWhenUserDoesNotHaveAnyAccessRights() {
        var cognitoUserEntryWithoutAccessRights = CognitoUserInfo.builder().withCurrentCustomer(randomUri()).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutAccessRights));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
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
    void shouldReturnThatUserIsApplicationAdminWhenUserHasTheRespectiveAccessRight() throws JsonProcessingException {
        var customerId = randomUri();
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withCurrentCustomer(customerId)
                          .withAccessRights(customerId, AccessRight.ADMINISTRATE_APPLICATION.toString())
                          .build();
        var requestInfo = RequestInfo.fromRequest(request);
        assertThat(requestInfo.userIsApplicationAdmin(), is(true));
    }

    @Test
    void shouldReturnThatUserIsNotApplicationAdminWhenUserDoesNotHaveTheRespectiveAccessRight()
        throws JsonProcessingException {
        var customerId = randomUri();
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withCurrentCustomer(customerId)
                          .withAccessRights(customerId, AccessRight.APPROVE_DOI_REQUEST.toString(), randomString())
                          .build();
        var requestInfo = RequestInfo.fromRequest(request);
        assertThat(requestInfo.userIsApplicationAdmin(), is(false));
    }

    @Test
    void isBackendClientShouldReturnTrueWhenScopeContainsTheBackendScope() throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withScope(
            BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE).build();
        var requestInfo = RequestInfo.fromRequest(request);
        assertThat(requestInfo.clientIsInternalBackend(), is(true));
    }

    @Test
    void isBackendClientShouldReturnFalseWhenScopeContainsTheBackendScope() throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withScope(randomString()).build();
        var requestInfo = RequestInfo.fromRequest(request);
        assertThat(requestInfo.clientIsInternalBackend(), is(false));
    }

    @Test
    void canGetValueFromRequestContext() throws JsonProcessingException {

        Map<String, Map<String, Map<String, Map<String, String>>>> map = Map.of(REQUEST_CONTEXT_FIELD,
                                                                                Map.of(AUTHORIZER, Map.of(CLAIMS,
                                                                                                          Map.of(KEY,
                                                                                                                 VALUE))));

        RequestInfo requestInfo = defaultRestObjectMapper.readValue(defaultRestObjectMapper.writeValueAsString(map),
                                                                    RequestInfo.class);

        JsonPointer jsonPointer = JsonPointer.compile(JSON_POINTER);
        JsonNode jsonNode = requestInfo.getRequestContext().at(jsonPointer);

        assertFalse(jsonNode.isMissingNode());
        assertEquals(VALUE, jsonNode.textValue());
    }

    @Test
    void shouldReturnRequestUriFromRequestInfo() throws ApiIoException {
        RequestInfo requestInfo = extractAccessRightsFromApiGatewayEvent();

        URI requestUri = requestInfo.getRequestUri();
        URI expectedRequestUri = new UriWrapper(HTTPS, DOMAIN_NAME_FOUND_IN_RESOURCE_FILE).addChild(
            PATH_FOUND_IN_RESOURCE_FILE).addQueryParameters(QUERY_PARAMS_FOUND_IN_RESOURCE_FILE).getUri();

        assertThat(requestUri, is(equalTo(expectedRequestUri)));
    }

    @Test
    void shouldReturnUsernameFromCognitoWhenUserHasSelectedCustomerAndClaimInNotAvailableOffline()
        throws UnauthorizedException {
        var expectedUsername = randomString();
        var cognitoUserEntry = CognitoUserInfo.builder().withUserName(expectedUsername).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualUsername = requestInfo.getUserName();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void shouldReturnUsernameWhenClaimInAvailableOffline() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder().withUserName(randomString()).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();

        var expectedUsername = randomString();
        injectUserNameInRequestInfo(requestInfo, expectedUsername);

        var actualUsername = requestInfo.getUserName();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenUserNameIsNotAvailable() {
        var cognitoUserEntryWithoutUserName = CognitoUserInfo.builder().build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutUserName));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        assertThrows(UnauthorizedException.class, requestInfo::getUserName);
    }

    @Test
    void shouldReturnPersonCristinIdFromCognitoWhenUserHasSelectedCustomerAndClaimInNotAvailableOffline()
        throws UnauthorizedException {
        var expectedPersonCristinId = randomUri();
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonCristinId(expectedPersonCristinId).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualPersonCristinId = requestInfo.getPersonCristinId();
        assertThat(actualPersonCristinId, is(equalTo(expectedPersonCristinId)));
    }

    @Test
    void shouldReturnPersonCristinIdWhenClaimInAvailableOffline() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonCristinId(randomUri()).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var expectedPersonCristinIdDifferentFromCognito = randomUri();
        injectPersonCristinIdInRequestInfo(requestInfo, expectedPersonCristinIdDifferentFromCognito);
        var actualPersonCristinId = requestInfo.getPersonCristinId();
        assertThat(actualPersonCristinId, is(equalTo(expectedPersonCristinIdDifferentFromCognito)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenPersonCristinIdIsNotAvailable() {
        var cognitoUserEntryWithoutPersonCristinId = CognitoUserInfo.builder().build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutPersonCristinId));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
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
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        assertThrows(UnauthorizedException.class, requestInfo::getCurrentCustomer);
    }

    @Test
    void shouldReturnTopLevelOrgCristinIdWhenCurrentCustomerHasBeenSelectedForPerson() {
        var topOrgCristinId = randomUri();
        var cognitoUserEntry = CognitoUserInfo.builder().withTopOrgCristinId(topOrgCristinId).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        assertThat(requestInfo.getTopLevelOrgCristinId().orElseThrow(), is(equalTo(topOrgCristinId)));
    }

    @Test
    void shouldReturnTopLevelOrgCristinIdWhenRequestsAuthorizerNodeContainsCorrespondingClaim()
        throws JsonProcessingException {
        var topOrgCristinId = randomUri();
        var requestInfo = requestInfoWithAuthorizerClaim(topOrgCristinId.toString());
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
        assertThrows(UnauthorizedException.class, requestInfo::getUserName);
    }

    @Test
    void shouldLogWarningWhenAuthenticationFails() throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).build();
        var requestInfo = RequestInfo.fromRequest(request);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        requestInfo.userIsAuthorized(randomString());
        assertThat(logger.getMessages(), containsString(AUTHORIZATION_FAILURE_WARNING));
    }

    @Test
    void shouldReturnPersonNinFromCognitoWhenUserHasPersonNinInClaimAndIsNotOffline() {
        var expectedPersonNin = randomString();
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonNin(expectedPersonNin).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualPersonNin = requestInfo.getPersonNin();
        assertThat(actualPersonNin, is(equalTo(expectedPersonNin)));
    }

    @Test
    void shouldReturnPersonNinWhenUserHasPersonNinInClaimAvailableOffline() {
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonNin(randomString()).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var expectedPersonNinDifferentFromCognito = randomString();
        injectPersonNinInRequestInfo(requestInfo, expectedPersonNinDifferentFromCognito);
        var actualPersonNin = requestInfo.getPersonNin();
        assertThat(actualPersonNin, is(equalTo(expectedPersonNinDifferentFromCognito)));
    }

    @Test
    void shouldReturnPersonNinFromFeideNinClaimWhenOnlyFeideNinIsPresentInCognito() throws JsonProcessingException {
        var expectedPersonFeideNin = randomString();
        var claims = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_FEIDE_NIN_CLAIM, expectedPersonFeideNin);
        var cognitoUserInfo = objectMapper.readValue(claims.toString(), CognitoUserInfo.class);
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserInfo));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualPersonNin = requestInfo.getPersonNin();

        assertThat(actualPersonNin, is(equalTo(expectedPersonFeideNin)));
    }

    @Test
    void shouldReturnFeideIdFromCognitoWhenUserHasFeideIdInClaimAndIsNotOffline() throws UnauthorizedException {
        var expectedFeideId = randomString();
        var cognitoUserEntry = CognitoUserInfo.builder().withFeideId(expectedFeideId).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualFeideId = requestInfo.getFeideId();
        assertThat(actualFeideId, is(equalTo(expectedFeideId)));
    }

    @Test
    void shouldReturnFeideIdWhenUserHasFeideIdInClaimAvailableOffline()
        throws JsonProcessingException, UnauthorizedException {
        var expectedFeideId = randomString();
        var claims = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_FEIDE_ID_CLAIM, expectedFeideId);
        var cognitoUserInfo = objectMapper.readValue(claims.toString(), CognitoUserInfo.class);
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserInfo));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualFeideId = requestInfo.getFeideId();
        assertThat(actualFeideId, is(equalTo(expectedFeideId)));
    }
    @Test
    void shouldReturnFeideIdFromFeideClaimWhenOnlyFeideIdIsPresentInCognito()
        throws JsonProcessingException, UnauthorizedException {
        var expectedFeideId = randomString();
        var claims = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_FEIDE_ID_CLAIM, expectedFeideId);
        var cognitoUserInfo = objectMapper.readValue(claims.toString(), CognitoUserInfo.class);
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserInfo));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        var actualFeideId = requestInfo.getFeideId();
        assertThat(actualFeideId, is(equalTo(expectedFeideId)));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenNoTypeOfPersonNinIsAvailable() {
        var cognitoUserEntryWithoutPersonNin = CognitoUserInfo.builder().build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutPersonNin));
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        assertThrows(IllegalStateException.class, requestInfo::getPersonNin);
    }

    @Test
    void shouldLogFailureWhenFailingToFetchInfoFromCognito() {
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonNin(randomString()).build();
        cognito.setUserBase(Map.of(randomString(), cognitoUserEntry));
        var logger = LogUtils.getTestingAppenderForRootLogger();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope();
        assertThrows(UnauthorizedException.class, requestInfo::getUserName);
        assertThat(logger.getMessages(),
                   containsString(ERROR_FETCHING_COGNITO_INFO.replace(LOG_STRING_INTERPOLATION, EMPTY_STRING)));
    }

    private String randomAccessRight(URI usersCustomer) {
        return new AccessRightEntry(randomString(), usersCustomer).toString();
    }

    private RequestInfo requestInfoWithCustomerId(URI userCustomer) throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withCurrentCustomer(userCustomer).build();
        return RequestInfo.fromRequest(request);
    }

    private RequestInfo requestInfoWithAuthorizerClaim(String claim) throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withAuthorizerClaim(
            CognitoUserInfo.TOP_LEVEL_ORG_CRISTIN_ID_CLAIM, claim).build();
        return RequestInfo.fromRequest(request);
    }

    private CognitoUserInfo createCognitoUserEntry(URI usersCustomer, Set<String> accessRightsForCustomer) {
        return CognitoUserInfo.builder()
                   .withCurrentCustomer(usersCustomer)
                   .withAccessRights(accessRightsForCustomer)
                   .build();
    }

    private void injectUserNameInRequestInfo(RequestInfo requestInfo, String expectedUsername) {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.USER_NAME_CLAIM, expectedUsername);
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

    private void injectPersonNinInRequestInfo(RequestInfo requestInfo, String expectedPersonNinDifferentFromCognito) {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_NIN_CLAIM, expectedPersonNinDifferentFromCognito);
        authorizer.set("claims", claims);
        requestContext.set("authorizer", authorizer);
        requestInfo.setRequestContext(requestContext);
    }

    private RequestInfo createRequestInfoWithAccessTokenThatHasOpenIdScope() {
        var cognitoServerUri = UriWrapper.fromUri(cognito.getServerUri()).addChild(OAUTH_USER_INFO).getUri();
        // having openid scope means that the default cognito URI will be the successful one
        var requestInfo = new RequestInfo(httpClient, () -> cognitoServerUri, failingUri());
        requestInfo.setHeaders(Map.of(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken)));
        return requestInfo;
    }

    private RequestInfo createRequestInfoWithAccessTokenThatHasAdminScope() {
        var cognitoServerUri = UriWrapper.fromUri(cognito.getServerUri()).addChild(OAUTH_USER_INFO).getUri();
        // having admin scope means that the default cognito URI will fail and the alternative be the successful one
        var requestInfo = new RequestInfo(httpClient, failingUri(), () -> cognitoServerUri);
        requestInfo.setHeaders(Map.of(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken)));
        return requestInfo;
    }

    private Supplier<URI> failingUri() {
        return RandomDataGenerator::randomUri;
    }

    private RequestInfo createRequestInfoForOfflineAuthorization(List<String> usersAccessRights, URI userCustomer)
        throws JsonProcessingException {
        var requestStream = new HandlerRequestBuilder<Void>(dtoObjectMapper).withCurrentCustomer(userCustomer)
                                .withAccessRights(userCustomer, usersAccessRights.toArray(String[]::new))
                                .build();
        return RequestInfo.fromRequest(requestStream);
    }

    private String bearerToken(String userAccessToken) {
        return "Bearer " + userAccessToken;
    }

    private RequestInfo extractAccessRightsFromApiGatewayEvent() throws ApiIoException {
        String event = IoUtils.stringFromResources(RequestInfoTest.AWS_SAMPLE_PROXY_EVENT);
        ApiMessageParser<String> apiMessageParser = new ApiMessageParser<>();
        return apiMessageParser.getRequestInfo(event);
    }

    private void checkForNonNullMap(Path resourceFile, Function<RequestInfo, Object> getObject)
        throws JsonProcessingException {
        String apiGatewayEvent = IoUtils.stringFromResources(resourceFile);
        RequestInfo requestInfo = defaultRestObjectMapper.readValue(apiGatewayEvent, RequestInfo.class);
        assertNotNull(getObject.apply(requestInfo));
    }
}

