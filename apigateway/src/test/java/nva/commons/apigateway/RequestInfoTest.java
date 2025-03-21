package nva.commons.apigateway;

import static java.util.Objects.nonNull;
import static no.unit.nva.auth.CognitoUserInfo.PERSON_GROUPS_CLAIM;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomAccessRight;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_IMPORT;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.RequestInfoConstants.AUTHORIZATION_FAILURE_WARNING;
import static nva.commons.apigateway.RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE;
import static nva.commons.apigateway.RequestInfoConstants.REQUEST_CONTEXT_FIELD;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static nva.commons.core.paths.UriWrapper.HTTPS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    public static final String UNDEFINED_REQUEST_INFO_PROPERTY = "body";
    public static final String DOMAIN_NAME_FOUND_IN_RESOURCE_FILE = "id.execute-api.us-east-1.amazonaws.com";
    public static final String PATH_FOUND_IN_RESOURCE_FILE = "my/path";
    public static final Map<String, String> QUERY_PARAMS_FOUND_IN_RESOURCE_FILE;
    public static final String AT = "@";
    private static final String API_GATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    private static final Path NULL_VALUES_FOR_MAPS = Path.of(API_GATEWAY_MESSAGES_FOLDER, "mapParametersAreNull.json");
    private static final Path MISSING_MAP_VALUES = Path.of(API_GATEWAY_MESSAGES_FOLDER, "missingRequestInfo.json");
    private static final Path AWS_SAMPLE_PROXY_EVENT = Path.of(API_GATEWAY_MESSAGES_FOLDER, "awsSampleProxyEvent.json");
    private static final String HARDCODED_AUTH_HEADER = "Bearer THE_ACCESS_TOKEN";
    public static final String EXTERNAL_USER_POOL_URL = "https//user-pool.example.com/123";
    private static final String THIRD_PARTY_PUBLICATION_UPSERT_SCOPE = "https://api.nva.unit"
                                                                       + ".no/scopes/third-party/publication-upsert";

    static {
        QUERY_PARAMS_FOUND_IN_RESOURCE_FILE = new TreeMap<>();
        QUERY_PARAMS_FOUND_IN_RESOURCE_FILE.put("parameter1", "value1");
        QUERY_PARAMS_FOUND_IN_RESOURCE_FILE.put("parameter2", "value");
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
    @DisplayName("RequestInfo initializes multiValueQueryParameters to empty map when JSON object sets "
                 + "multiValueQueryStringParameters to null")
    void requestInfoInitializesMultiValueQueryParametersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getMultiValueQueryStringParameters);
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

        RequestInfo requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var actualCustomerId = requestInfo.getCurrentCustomer();
        assertThat(actualCustomerId, is(equalTo(expectedCurrentCustomer)));
    }

    @Test
    void shouldReturnThatUserHasAccessRightForSpecificCustomerWhenCognitoHasRespectiveEntryAndUsesLegacyFormat() {
        var usersCustomer = randomUri();
        var accessRights = randomAccessRights();
        Set<String> shortAccessRights = accessRights.stream()
                                            .map(AccessRight::toPersistedString)
                                            .collect(Collectors.toSet());
        var cognitoUserEntry = createCognitoUserEntry(usersCustomer, shortAccessRights);
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        for (var accessRight : accessRights) {
            var userIsAuthorized = requestInfo.userIsAuthorized(accessRight);
            assertThat(userIsAuthorized, is(true));
        }
    }

    @Test
    void shouldReturnThatUserHasAccessRightForSpecificCustomerWhenCognitoHasRespectiveEntry() {
        var usersCustomer = randomUri();
        var accessRights = randomAccessRights();
        var accessRightsForCustomer = accessRights.stream()
                                          .map(AccessRight::toPersistedString)
                                          .collect(Collectors.toSet());
        var cognitoUserEntry = createCognitoUserEntry(usersCustomer, accessRightsForCustomer);
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        for (var accessRight : accessRights) {
            var userIsAuthorized = requestInfo.userIsAuthorized(accessRight);
            assertThat(userIsAuthorized, is(true));
        }
    }

    @Test
    void shouldReturnThatUserDoesNotHaveAccessRightForSpecificCustomerWhenCognitoDoesNotHaveRespectiveAccessRight() {
        var usersCustomer = randomUri();
        var accessRights = Set.of(MANAGE_PUBLISHING_REQUESTS.toPersistedString());
        var cognitoUserEntry = CognitoUserInfo.builder()
                                   .withCurrentCustomer(usersCustomer)
                                   .withAccessRights(accessRights)
                                   .build();

        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var userIsAuthorized = requestInfo.userIsAuthorized(MANAGE_DOI);
        assertThat(userIsAuthorized, is(false));
    }

    private static Set<AccessRight> randomAccessRights() {
        return new HashSet<>(List.of(randomAccessRight(), randomAccessRight(), randomAccessRight()));
    }

    @Test
    void shouldReturnThatUserDoesNotHaveAccessRightForSpecificCustomerWhenUserDoesNotHaveAnyAccessRights() {
        var cognitoUserEntryWithoutAccessRights = CognitoUserInfo.builder().withCurrentCustomer(randomUri()).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntryWithoutAccessRights);
        var requestedAccessRight = randomAccessRight();
        assertThat(requestInfo.userIsAuthorized(requestedAccessRight), is(false));
    }

    @Test
    void shouldReturnThatUserIsAuthorizedWhenRequestInfoContainsACustomerIdAndTheRequestedAccessRight() {
        var usersCustomer = randomUri();
        var usersAccessRights = Set.of(MANAGE_DEGREE.toPersistedString(),
                                       MANAGE_IMPORT.toPersistedString());
        var cognitoUserEntry = CognitoUserInfo.builder()
                                   .withCurrentCustomer(usersCustomer)
                                   .withAccessRights(usersAccessRights)
                                   .build();
        RequestInfo requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        for (var accessRight : usersAccessRights) {
            assertThat(requestInfo.userIsAuthorized(AccessRight.fromPersistedString(accessRight)), is(true));
        }
    }

    @Test
    void shouldReturnNotAuthorizedWhenRequestInfoDoesNotContainTheRequestedAccessRightAndCheckIsPerformedOffline() {
        var userCustomer = randomUri();
        var usersAccessRights = Set.of(MANAGE_DEGREE.toPersistedString(), MANAGE_IMPORT.toPersistedString());
        var cognitoUserEntry = CognitoUserInfo.builder()
                                   .withCurrentCustomer(userCustomer)
                                   .withAccessRights(usersAccessRights)
                                   .build();
        RequestInfo requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var notAllocatedAccessRight = MANAGE_PUBLISHING_REQUESTS;
        assertThat(requestInfo.userIsAuthorized(notAllocatedAccessRight), is(false));
    }

    @Test
    void shouldReturnNotAuthorizedWhenRequestInfoDoesNotContainAccessRightsAndCheckIsPerformedOffline()
        throws JsonProcessingException, ApiIoException {
        var userCustomer = randomUri();
        var requestInfo = requestInfoWithCustomerId(userCustomer);
        var notAllocatedAccessRight = new AccessRightEntry(MANAGE_DEGREE, userCustomer);
        var accessRight = notAllocatedAccessRight.getAccessRight();
        assertThat(requestInfo.userIsAuthorized(accessRight), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "\"\"", "null", "[]", "true", "false", "\"custom:feideId\"" })
    void shouldThrowUnauthorizedOnEmptyOrInvalidClaimSyntax(String claims) throws ApiIoException {
        var payload = """
            {
              "requestContext": {
                "authorizer": {
                  "claims": %s
                }
              }
            }
            """.formatted(claims);
        var request = getRequestInfo(new ByteArrayInputStream(payload.getBytes()));

        assertThrows(UnauthorizedException.class, request::getCurrentCustomer);
    }

    @Test
    void shouldThrowUnauthorizedMissingClaims() throws ApiIoException {
        var payload = """
            {
              "requestContext": {
                "authorizer": {}
              }
            }
            """;
        var request = getRequestInfo(new ByteArrayInputStream(payload.getBytes()));

        assertThrows(UnauthorizedException.class, request::getCurrentCustomer);
    }

    @Test
    void shouldThrowUnauthorizedMissingAuthorizer() throws ApiIoException {
        var payload = """
            {
              "requestContext": {}
            }
            """;
        var request = getRequestInfo(new ByteArrayInputStream(payload.getBytes()));

        assertThrows(UnauthorizedException.class, request::getCurrentCustomer);
    }

    @Test
    void shouldReturnApiIoExceptionWhenObjectMapperFails() {
        var request = "This is not a valid request";
        assertThrows(ApiIoException.class, () -> RequestInfo.fromRequest(IoUtils.stringToStream(request)));
    }

    @Test
    void shouldReturnMultiValueQueryValuesWhenProvided()
        throws JsonProcessingException, ApiIoException {
        var key = randomString();
        var value1 = randomString();
        var value2 = randomString();

        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withMultiValueQueryParameters(
            Map.of(key, List.of(value1, value2))).build();

        var requestInfo = getRequestInfo(request);
        assertThat(requestInfo.getMultiValueQueryParameter(key), is(equalTo(List.of(value1, value2))));
    }

    @Test
    void isBackendClientShouldReturnTrueWhenScopeContainsTheBackendScope()
        throws JsonProcessingException, ApiIoException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withScope(
            BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE).build();
        var requestInfo = getRequestInfo(request);
        assertThat(requestInfo.clientIsInternalBackend(), is(true));
    }

    @Test
    void isBackendClientShouldReturnFalseWhenScopeContainsTheBackendScope()
        throws JsonProcessingException, ApiIoException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withScope(randomString()).build();
        var requestInfo = getRequestInfo(request);
        assertThat(requestInfo.clientIsInternalBackend(), is(false));
    }

    @Test
    void isThirdPartyShouldReturnTrueWhenScopeContainsTheExternalIssuedUserPool()
        throws JsonProcessingException, ApiIoException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withIssuer(EXTERNAL_USER_POOL_URL)
                          .withScope(THIRD_PARTY_PUBLICATION_UPSERT_SCOPE)
                          .build();
        var requestInfo = getRequestInfo(request);
        assertThat(requestInfo.clientIsThirdParty(), is(true));
    }

    @Test
    void isThirdPartyShouldReturnFalseWhenScopeContainsOtherUserPool()
        throws JsonProcessingException, ApiIoException {
        var issuer = RandomDataGenerator.randomUri().toString();
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withIssuer(issuer).build();
        var requestInfo = getRequestInfo(request);
        assertThat(requestInfo.clientIsThirdParty(), is(false));
    }

    @Test
    void getClientIdShouldReturnEmptyOptionalWhenClientIdIsNotInClaim()
        throws JsonProcessingException, ApiIoException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).build();
        var requestInfo = getRequestInfo(request);
        assertThat(requestInfo.getClientId().isEmpty(), is(true));
    }

    private RequestInfo getRequestInfo(InputStream request) throws ApiIoException {
        return RequestInfo.fromRequest(request);
    }

    @Test
    void getClientIdShouldReturnTheClientIdFromClaim()
        throws JsonProcessingException, ApiIoException {
        var clientId = RandomDataGenerator.randomString();
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withClientId(clientId).build();
        var requestInfo = getRequestInfo(request);
        assertThat(requestInfo.getClientId().get(), is(equalTo(clientId)));
    }

    @Test
    void canGetValueFromRequestContext() throws JsonProcessingException {

        Map<String, Map<String, Map<String, Map<String, String>>>> map = Map.of(REQUEST_CONTEXT_FIELD,
                                                                                Map.of(AUTHORIZER,
                                                                                       Map.of(CLAIMS,
                                                                                              Map.of(KEY, VALUE))));

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
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var actualUsername = requestInfo.getUserName();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void shouldReturnUsernameWhenClaimInAvailableOffline() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder().withUserName(randomString()).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);

        var expectedUsername = randomString();
        injectUserNameInRequestInfo(requestInfo, expectedUsername);

        var actualUsername = requestInfo.getUserName();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenUserNameIsNotAvailable() {
        var cognitoUserEntryWithoutUserName = CognitoUserInfo.builder().build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntryWithoutUserName);
        assertThrows(UnauthorizedException.class, requestInfo::getUserName);
    }

    @Test
    void shouldReturnPersonCristinIdFromCognitoWhenUserHasSelectedCustomerAndClaimInNotAvailableOffline()
        throws UnauthorizedException {
        var expectedPersonCristinId = randomUri();
        var cognitoUserEntry =
            CognitoUserInfo.builder()
                .withPersonCristinId(expectedPersonCristinId)
                .withCurrentCustomer(randomUri())
                .build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var actualPersonCristinId = requestInfo.getPersonCristinId();
        assertThat(actualPersonCristinId, is(equalTo(expectedPersonCristinId)));
    }

    @Test
    void shouldReturnPersonCristinIdWhenClaimInAvailableOffline() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonCristinId(randomUri()).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var expectedPersonCristinIdDifferentFromCognito = randomUri();
        injectPersonCristinIdInRequestInfo(requestInfo, expectedPersonCristinIdDifferentFromCognito);
        var actualPersonCristinId = requestInfo.getPersonCristinId();
        assertThat(actualPersonCristinId, is(equalTo(expectedPersonCristinIdDifferentFromCognito)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenPersonCristinIdIsNotAvailable() {
        var cognitoUserEntryWithoutPersonCristinId = CognitoUserInfo.builder().build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntryWithoutPersonCristinId);
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
    void shouldReturnPersonAffiliationFromCognitoWhenUserHasSelectedCustomerAndClaimInNotAvailableOffline()
        throws UnauthorizedException {
        var expectedPersonAffiliation = randomUri();
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonAffiliation(expectedPersonAffiliation).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var actualPersonAffiliation = requestInfo.getPersonAffiliation();
        assertThat(actualPersonAffiliation, is(equalTo(expectedPersonAffiliation)));
    }

    @Test
    void shouldReturnPersonAffiliationWhenClaimInAvailableOffline() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonAffiliation(randomUri()).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var expectedPersonAffiliationDifferentFromCognito = randomUri();
        injectPersonAffiliationInRequestInfo(requestInfo, expectedPersonAffiliationDifferentFromCognito);
        var actualPersonAffiliation = requestInfo.getPersonAffiliation();
        assertThat(actualPersonAffiliation, is(equalTo(expectedPersonAffiliationDifferentFromCognito)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenPersonAffiliationIsNotAvailable() {
        var cognitoUserEntryWithoutPersonAffiliation = CognitoUserInfo.builder().build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntryWithoutPersonAffiliation);
        assertThrows(UnauthorizedException.class, requestInfo::getPersonAffiliation);
    }

    @Test
    void shouldReadCognitoUriFromEnvByDefaultThatContainsValidPersonAffiliation()
        throws JsonProcessingException, UnauthorizedException {
        var requestInfoString = IoUtils.stringFromResources(EVENT_WITH_AUTH_HEADER);
        var requestInfo = dtoObjectMapper.readValue(requestInfoString, RequestInfo.class);
        assertNotNull(requestInfo.getPersonAffiliation());
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenCustomerIdIsNotAvailable() {
        var cognitoUserEntryWithoutCustomerId = CognitoUserInfo.builder().build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntryWithoutCustomerId);
        assertThrows(UnauthorizedException.class, requestInfo::getCurrentCustomer);
    }

    @Test
    void shouldReturnTopLevelOrgCristinIdWhenCurrentCustomerHasBeenSelectedForPerson() {
        var topOrgCristinId = randomUri();
        var cognitoUserEntry = CognitoUserInfo.builder().withTopOrgCristinId(topOrgCristinId).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        assertThat(requestInfo.getTopLevelOrgCristinId().orElseThrow(), is(equalTo(topOrgCristinId)));
    }

    @Test
    void shouldReturnTopLevelOrgCristinIdWhenRequestsAuthorizerNodeContainsCorrespondingClaim()
        throws JsonProcessingException, ApiIoException {
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
    void shouldReadMultiValueQueryParameters() throws JsonProcessingException {
        var requestInfoString = IoUtils.stringFromResources(EVENT_WITH_AUTH_HEADER);
        var requestInfo = dtoObjectMapper.readValue(requestInfoString, RequestInfo.class);
        assertThat(requestInfo.getMultiValueQueryStringParameters(), is(notNullValue()));
    }

    @Test
    void shouldLogWarningWhenAuthenticationFails() throws JsonProcessingException, ApiIoException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).build();
        var requestInfo = getRequestInfo(request);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        requestInfo.userIsAuthorized(randomAccessRight());
        assertThat(logger.getMessages(), containsString(AUTHORIZATION_FAILURE_WARNING));
    }

    @Test
    void shouldReturnPersonNinFromCognitoWhenUserHasPersonNinInClaimAndIsNotOffline() {
        var expectedPersonNin = randomString();
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonNin(expectedPersonNin).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var actualPersonNin = requestInfo.getPersonNin();
        assertThat(actualPersonNin, is(equalTo(expectedPersonNin)));
    }

    @Test
    void shouldReturnPersonNinWhenUserHasPersonNinInClaimAvailableOffline() {
        var cognitoUserEntry = CognitoUserInfo.builder().withPersonNin(randomString()).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var expectedPersonNinDifferentFromCognito = randomString();
        injectPersonNinInRequestInfo(requestInfo, expectedPersonNinDifferentFromCognito);
        var actualPersonNin = requestInfo.getPersonNin();
        assertThat(actualPersonNin, is(equalTo(expectedPersonNinDifferentFromCognito)));
    }

    @Test
    void shouldReturnPersonNinFromFeideNinClaimWhenOnlyFeideNinIsPresentInCognito()
        throws JsonProcessingException {
        var expectedPersonFeideNin = randomString();
        var claims = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_FEIDE_NIN_CLAIM, expectedPersonFeideNin);
        claims.put(CognitoUserInfo.SELECTED_CUSTOMER_CLAIM, randomString());
        var cognitoUserInfo = objectMapper.readValue(claims.toString(), CognitoUserInfo.class);
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserInfo);
        var actualPersonNin = requestInfo.getPersonNin();

        assertThat(actualPersonNin, is(equalTo(expectedPersonFeideNin)));
    }

    @Test
    void shouldReturnFeideIdFromCognitoWhenUserHasFeideIdInClaimAndIsNotOffline() {
        var expectedFeideId = randomString();
        var cognitoUserEntry = CognitoUserInfo.builder().withFeideId(expectedFeideId).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var actualFeideId = requestInfo.getFeideId();

        assertThat(requestInfo.getFeideId().isPresent(), is(true));
        assertThat(actualFeideId.orElseThrow(), is(equalTo(expectedFeideId)));
    }

    @Test
    void shouldReturnFeideIdWhenUserHasFeideIdInClaimAvailableOffline() {
        var cognitoUserEntry = CognitoUserInfo.builder().withFeideId(randomString()).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var expectedFeideIdDifferentFromCognito = randomString();
        injectFeideIdInRequestInfo(requestInfo, expectedFeideIdDifferentFromCognito);
        var actualFeideId = requestInfo.getFeideId();

        assertThat(requestInfo.getFeideId().isPresent(), is(true));
        assertThat(actualFeideId.orElseThrow(), is(equalTo(expectedFeideIdDifferentFromCognito)));
    }

    @Test
    void shouldReturnFeideIdFromFeideClaimWhenOnlyFeideIdIsPresentInCognito()
        throws JsonProcessingException {
        var expectedFeideId = randomString();
        var claims = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_FEIDE_ID_CLAIM, expectedFeideId);
        claims.put(CognitoUserInfo.SELECTED_CUSTOMER_CLAIM, randomString());
        var cognitoUserInfo = objectMapper.readValue(claims.toString(), CognitoUserInfo.class);
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserInfo);
        var actualFeideId = requestInfo.getFeideId();

        assertThat(requestInfo.getFeideId().isPresent(), is(true));
        assertThat(actualFeideId.orElseThrow(), is(equalTo(expectedFeideId)));
    }

    @Test
    void shouldReturnAccessRightsWhenOnlyInCognito() throws JsonProcessingException {
        var accessRights = randomAccessRights();
        var accessRightsString = accessRights.stream()
                                     .map(AccessRight::toPersistedString)
                                     .collect(Collectors.joining(","));

        var claims = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.ACCESS_RIGHTS_CLAIM, accessRightsString);
        claims.put(CognitoUserInfo.SELECTED_CUSTOMER_CLAIM, randomString());
        var cognitoUserInfo = objectMapper.readValue(claims.toString(), CognitoUserInfo.class);
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserInfo);
        var actualAccessRights = requestInfo.getAccessRights();

        assertThat(actualAccessRights, containsInAnyOrder(accessRights.toArray()));
    }

    @Test
    void shouldReturnOptionalEmptyWhenUserDoesNotHaveFeideId() {
        var cognitoUserEntryWithoutFeideId = CognitoUserInfo.builder().build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntryWithoutFeideId);

        assertThat(requestInfo.getFeideId().isPresent(), is(false));
        assertThat(requestInfo.getFeideId(), is(Optional.empty()));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenNoTypeOfPersonNinIsAvailable() {
        var cognitoUserEntryWithoutPersonNin = CognitoUserInfo.builder().build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntryWithoutPersonNin);

        assertThrows(IllegalStateException.class, requestInfo::getPersonNin);
    }

    @Test
    void shouldReturnEmptyListWhenAccessRightsCognitoStringIsEmpty() {
        var cognitoUserEntry = CognitoUserInfo.builder().build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var accessRights = requestInfo.getAccessRights();

        assertThat(accessRights, is(emptyIterable()));
    }

    @Test
    void shouldReturnListOfAllowedCustomers() throws UnauthorizedException {
        var allowedCustomers = List.of(randomUri(), randomUri());
        String allowedCustomersString = allowedCustomers.stream()
                                            .map(URI::toString)
                                            .collect(Collectors.joining(","));
        var cognitoUserEntry = CognitoUserInfo.builder().withAllowedCustomers(allowedCustomersString).build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var actual = requestInfo.getAllowedCustomers();

        assertThat(actual, containsInAnyOrder(allowedCustomers.toArray()));
    }

    @ParameterizedTest
    @MethodSource("emptyViewingScopeInputsProvider")
    void shouldReturnEmptyViewingScopeIfNotPresentForUser(String includes, String excludes)
        throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder()
                                   .withViewingScopeIncluded(includes)
                                   .withViewingScopeExcluded(excludes)
                                   .build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var getViewingScope = requestInfo.getViewingScope();
        assertThat(getViewingScope.includes(), is(emptyIterable()));
        assertThat(getViewingScope.excludes(), is(emptyIterable()));
    }

    @Test
    void shouldReturnViewingScopeFromRequestContextIfPresentForUser()
        throws UnauthorizedException, JsonProcessingException, ApiIoException {
        var requestInfo = createRequestInfoWithViewScopeRequestContext("1,2", "3,4");
        var getViewingScope = requestInfo.getViewingScope();
        assertThat(getViewingScope.includes(), is(hasItems("1", "2")));
        assertThat(getViewingScope.excludes(), is(hasItems("3", "4")));
    }

    @Test
    void shouldReturnViewingScopeFromCognitoIfPresentForUser() throws UnauthorizedException {
        var cognitoUserEntry = CognitoUserInfo.builder()
                                   .withViewingScopeIncluded("1,2")
                                   .withViewingScopeExcluded("3,4")
                                   .withCurrentCustomer(randomUri())
                                   .build();
        var requestInfo = createRequestInfoWithAccessTokenThatHasOpenIdScope(cognitoUserEntry);
        var getViewingScope = requestInfo.getViewingScope();
        assertThat(getViewingScope.includes(), is(hasItems("1", "2")));
        assertThat(getViewingScope.excludes(), is(hasItems("3", "4")));
    }

    public static Stream<Arguments> emptyViewingScopeInputsProvider() {
        return Stream.of(
            argumentSet("both includes and excludes is null", null, null),
            argumentSet("both includes and excludes is empty", "", ""),
            argumentSet("both includes and excludes is null string", "null", "null")
        );
    }

    private RequestInfo requestInfoWithCustomerId(URI userCustomer) throws JsonProcessingException,
                                                                           ApiIoException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).build();
        return getRequestInfo(request);
    }

    private RequestInfo requestInfoWithAuthorizerClaim(String claim) throws JsonProcessingException,
                                                                            ApiIoException {
        var request = new HandlerRequestBuilder<Void>(dtoObjectMapper).withAuthorizerClaim(
            CognitoUserInfo.TOP_LEVEL_ORG_CRISTIN_ID_CLAIM, claim).build();
        return getRequestInfo(request);
    }

    private CognitoUserInfo createCognitoUserEntry(URI usersCustomer, Set<String> accessRights) {
        return CognitoUserInfo.builder()
                   .withCurrentCustomer(usersCustomer)
                   .withAccessRights(accessRights)
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

    private void injectPersonAffiliationInRequestInfo(RequestInfo requestInfo, URI expectedPersonAffiliation) {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_AFFILIATION_CLAIM, expectedPersonAffiliation.toString());
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

    private void injectFeideIdInRequestInfo(RequestInfo requestInfo, String expectedFeideIdDifferentFromCognito) {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.PERSON_FEIDE_ID_CLAIM, expectedFeideIdDifferentFromCognito);
        authorizer.set("claims", claims);
        requestContext.set("authorizer", authorizer);
        requestInfo.setRequestContext(requestContext);
    }

    private static ObjectNode getRequestContext(CognitoUserInfo user) {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        user.getClaims().forEach(claims::put);

        if (nonNull(user.getCurrentCustomer())) {
            var customerAccessRigts = Arrays.stream(user.getAccessRights().split(","))
                                          .map(right -> right + AT + user.getCurrentCustomer().toString())
                                          .collect(Collectors.joining(","));
            claims.put(PERSON_GROUPS_CLAIM, customerAccessRigts);
        }

        authorizer.set("claims", claims);
        requestContext.set("authorizer", authorizer);
        return requestContext;
    }

    private RequestInfo createRequestInfoWithAccessTokenThatHasOpenIdScope(CognitoUserInfo user) {

        try {
            var info = RequestInfo.fromString("{}");
            info.setRequestContext(getRequestContext(user));
            return info;
        } catch (ApiIoException e) {
            throw new RuntimeException(e);
        }
    }

    private RequestInfo createRequestInfoWithViewScopeRequestContext(String includes, String excludes)
        throws JsonProcessingException, ApiIoException {
        var requestStream = new HandlerRequestBuilder<Void>(dtoObjectMapper)
                                .withAuthorizerClaim("custom:viewingScopeIncluded", includes)
                                .withAuthorizerClaim("custom:viewingScopeExcluded", excludes)
                                .build();
        return getRequestInfo(requestStream);
    }

    private RequestInfo extractAccessRightsFromApiGatewayEvent() throws ApiIoException {
        String event = IoUtils.stringFromResources(RequestInfoTest.AWS_SAMPLE_PROXY_EVENT);
        return getRequestInfo(new ByteArrayInputStream(event.getBytes()));
    }

    private void checkForNonNullMap(Path resourceFile, Function<RequestInfo, Object> getObject)
        throws JsonProcessingException {
        String apiGatewayEvent = IoUtils.stringFromResources(resourceFile);
        RequestInfo requestInfo = defaultRestObjectMapper.readValue(apiGatewayEvent, RequestInfo.class);
        assertNotNull(getObject.apply(requestInfo));
    }
}

