package nva.commons.apigateway;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.RequestInfo.APPLICATION_ROLES;
import static nva.commons.apigateway.RequestInfo.CUSTOMER_ID;
import static nva.commons.apigateway.RequestInfo.ENTRIES_DELIMITER;
import static nva.commons.apigateway.RequestInfo.FEIDE_ID;
import static nva.commons.apigateway.RequestInfo.REQUEST_CONTEXT_FIELD;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import no.unit.nva.auth.UserInfo;
import no.unit.nva.stubs.FakeAuthServer;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
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
    public static final String UNDEFINED_REQUEST_INFO_PROPERTY = "body";
    public static final String PATH_DELIMITER = "/";
    public static final int UNNECESSARY_ROOT_NODE = 0;
    public static final int FIRST_NODE = 0;

    public static final URI HARDCODED_URI_IN_RESOURCE_FILE =
        URI.create("https://example.org/someArbitrayCristinId234567890876545688");
    public static final String DOMAIN_NAME_FOUND_IN_RESOURCE_FILE = "id.execute-api.us-east-1.amazonaws.com";
    public static final String PATH_FOUND_IN_RESOURCE_FILE = "my/path";
    public static final Map<String, String> QUERY_PARAMS_FOUND_IN_RESOURCE_FILE;
    public static final String AT = "@";
    private static final String API_GATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    public static final Path EVENT_WITH_CRISTIN_ID = Path.of(API_GATEWAY_MESSAGES_FOLDER, "event_with_cristin_id.json");

    private static final Path NULL_VALUES_FOR_MAPS = Path.of(API_GATEWAY_MESSAGES_FOLDER,
                                                             "mapParametersAreNull.json");
    private static final Path MISSING_MAP_VALUES = Path.of(API_GATEWAY_MESSAGES_FOLDER,
                                                           "missingRequestInfo.json");
    private static final Path AWS_SAMPLE_PROXY_EVENT = Path.of(API_GATEWAY_MESSAGES_FOLDER,
                                                               "awsSampleProxyEvent.json");

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
    void requestInfoInitializesHeadersToEmptyMapWhenPathParametersParameterIsMissing()
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
    void shouldReturnFeideIdWhenRequestContainFeideIdClaim() {
        RequestInfo requestInfo = new RequestInfo();
        String expectedUsername = "orestis";
        requestInfo.setRequestContext(createNestedNodesFromJsonPointer(FEIDE_ID, expectedUsername));

        String actual = requestInfo.getFeideId().orElseThrow();
        assertEquals(actual, expectedUsername);
    }

    @Test
    void shouldFetchFeideIdWhenRequestDoesNotContainFeideIdClaimButHasAccessTokenAndCognitoUserContainsFeideIdClaim() {

        var expectedFeideId = randomString();
        var cognitoUserEntry = UserInfo.builder().withFeideId(expectedFeideId).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));

        var requestInfo = createRequestInfoWithAccessToken();
        var actualFeideId = requestInfo.getFeideId().orElseThrow();
        assertThat(actualFeideId, is(equalTo(expectedFeideId)));
    }

    @Test
    void requestInfoReturnsCustomerIdWhenRequestContainsCustomerIdClaim() {
        var requestInfo = new RequestInfo();
        var expectedCustomerId = "customerId";
        requestInfo.setRequestContext(createNestedNodesFromJsonPointer(CUSTOMER_ID, expectedCustomerId));
        var actual = requestInfo.getCustomerId().orElseThrow();
        assertEquals(actual, expectedCustomerId);
    }

    @Test
    void shouldReturnCustomerIdWhenRequestDoesNotContainCustomerIdButHasAccessTokenAndCognitoUserHasSelectedCustomer() {
        var expectedCurrentCustomer = randomUri();
        var cognitoUserEntry = UserInfo.builder().withCurrentCustomer(expectedCurrentCustomer).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));

        RequestInfo requestInfo = createRequestInfoWithAccessToken();
        var actualCustomerId = requestInfo.getCustomerId().orElseThrow();
        assertThat(URI.create(actualCustomerId), is(equalTo(expectedCurrentCustomer)));
    }

    @Test
    void shouldReturnThatUserHasAccessRightForSpecificCustomerWhenCognitoHasRespectiveEntry() {
        var usersCustomer = randomUri();
        var accessRights = Set.of(randomString(), randomString(), randomString());
        var accessRightsForCustomer = accessRights.stream()
            .map(right -> right + AT + usersCustomer)
            .collect(Collectors.toSet());
        var cognitoUserEntry = UserInfo.builder().withAccessRights(accessRightsForCustomer).build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        for (var accessRight : accessRights) {
            var userIsAuthorized = requestInfo.userIsAuthorized(accessRight, usersCustomer);
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
        var cognitoUserEntry = UserInfo.builder().withAccessRights(accessRightsForCustomer).build();

        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntry));
        var requestInfo = createRequestInfoWithAccessToken();
        var requestedAccessRight = randomString();
        var userIsAuthorized = requestInfo.userIsAuthorized(requestedAccessRight, usersCustomer);
        assertThat(userIsAuthorized, is(false));
    }

    @Test
    void shouldReturnThatUserDoesNotHaveAccessRightForSpecificCustomerWhenUserDoesNotHaveAnyAccessRights() {
        var cognitoUserEntryWithoutAccessRights = UserInfo.builder().build();
        cognito.setUserBase(Map.of(userAccessToken, cognitoUserEntryWithoutAccessRights));
        var requestInfo = createRequestInfoWithAccessToken();
        var requestedAccessRight = randomString();
        var userIsAuthorized = requestInfo.userIsAuthorized(requestedAccessRight, randomUri());
        assertThat(userIsAuthorized, is(false));
    }

    @Test
    void shouldReturnThatUserIsAuthorizedWhenRequestInfoContainsACustomerIdAndTheRequestedAccessRight() {
        var usersAccessRights = List.of(randomString(), randomString());
        var usersCustomer = randomUri();
        RequestInfo requestInfo = createRequestInfoForOfflineAuthorization(usersAccessRights, usersCustomer);
        for (var accessRight : usersAccessRights) {
            assertThat(requestInfo.userIsAuthorized(accessRight, usersCustomer), is(true));
        }
    }

    @Test
    void shouldReturnNotAuthorizedWhenRequestInfoDoesNotContainTheRequestedAccessRightAndCheckIsPerformedOffline() {
        var usersAccessRights = List.of(randomString(), randomString());
        var usersCustomer = randomUri();
        var requestInfo = createRequestInfoForOfflineAuthorization(usersAccessRights, usersCustomer);
        var notAllocatedAccessRight = randomString();
        assertThat(requestInfo.userIsAuthorized(notAllocatedAccessRight, usersCustomer), is(false));
    }

    @Test
    void shouldReturnNotAuthorizedWhenRequestInfoDoesNotContainAccessRightsAndCheckIsPerformedOffline() {
        var usersCustomer = randomUri();
        var requestInfo = new RequestInfo();
        var requestContext = dtoObjectMapper.createObjectNode();
        var authorizerNode = dtoObjectMapper.createObjectNode();
        var claimsNode = dtoObjectMapper.createObjectNode();
        claimsNode.put("custom:customerId", usersCustomer.toString());
        authorizerNode.set("claims", claimsNode);
        requestContext.set("authorizer", authorizerNode);
        requestInfo.setRequestContext(requestContext);
        var notAllocatedAccessRight = randomString();
        assertThat(requestInfo.userIsAuthorized(notAllocatedAccessRight, usersCustomer), is(false));
    }

    @Test
    void requestInfoReturnsAssignedRolesForRequestContextWithCredentials() {
        RequestInfo requestInfo = new RequestInfo();
        String expectedRoles = "role1,role2";
        requestInfo.setRequestContext(createNestedNodesFromJsonPointer(APPLICATION_ROLES, expectedRoles));

        String actual = requestInfo.getAssignedRoles().orElseThrow();
        assertEquals(actual, expectedRoles);
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
        RequestInfo requestInfo = extractRequestInfoFromApiGatewayEvent(AWS_SAMPLE_PROXY_EVENT);

        URI requestUri = requestInfo.getRequestUri();
        URI expectedRequestUri = new UriWrapper(RequestInfo.HTTPS, DOMAIN_NAME_FOUND_IN_RESOURCE_FILE)
            .addChild(PATH_FOUND_IN_RESOURCE_FILE)
            .addQueryParameters(QUERY_PARAMS_FOUND_IN_RESOURCE_FILE)
            .getUri();

        assertThat(requestUri, is(equalTo(expectedRequestUri)));
    }

    @Test
    void shouldReturnCustomerCristinIdFromRequestInfo() throws ApiIoException {
        var requestInfo = extractRequestInfoFromApiGatewayEvent(EVENT_WITH_CRISTIN_ID);
        var cristinId = requestInfo.getCustomerCristinId().orElseThrow();

        assertThat(cristinId, is(instanceOf(URI.class)));
        assertThat(cristinId, is(equalTo(HARDCODED_URI_IN_RESOURCE_FILE)));
    }

    private RequestInfo createRequestInfoWithAccessToken() {
        var requestInfo = new RequestInfo(httpClient, cognito.getServerUri());
        requestInfo.setHeaders(Map.of(HttpHeaders.AUTHORIZATION, bearerToken()));
        return requestInfo;
    }

    private RequestInfo createRequestInfoForOfflineAuthorization(List<String> usersAccessRights, URI usersCustomer) {
        var requestInfo = new RequestInfo();
        var requestContext = dtoObjectMapper.createObjectNode();
        var authorizerNode = dtoObjectMapper.createObjectNode();
        var claimsNode = dtoObjectMapper.createObjectNode();
        claimsNode.put("custom:accessRights", String.join(ENTRIES_DELIMITER, usersAccessRights));
        claimsNode.put("custom:customerId", usersCustomer.toString());
        authorizerNode.set("claims", claimsNode);
        requestContext.set("authorizer", authorizerNode);
        requestInfo.setRequestContext(requestContext);
        return requestInfo;
    }

    private String bearerToken() {
        return "Bearer " + userAccessToken;
    }

    private RequestInfo extractRequestInfoFromApiGatewayEvent(Path eventWithAccessRights)
        throws ApiIoException {
        String event = IoUtils.stringFromResources(eventWithAccessRights);
        ApiMessageParser<String> apiMessageParser = new ApiMessageParser<>();
        return apiMessageParser.getRequestInfo(event);
    }

    private ObjectNode createNestedNodesFromJsonPointer(JsonPointer jsonPointer, String value) {
        List<SimpleEntry<String, ObjectNode>> nodeList = createNodesForEachPathElement(jsonPointer);
        nestNodes(nodeList);
        SimpleEntry<String, ObjectNode> lastEntry = nodeList.get(lastIndex(nodeList));
        insertTextValueToLeafNode(value, lastEntry);

        return nodeList.get(FIRST_NODE).getValue();
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

