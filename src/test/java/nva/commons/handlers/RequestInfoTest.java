package nva.commons.handlers;

import static nva.commons.handlers.RequestInfo.REQUEST_CONTEXT_FIELD;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RequestInfoTest {

    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String JSON_POINTER = "/authorizer/claims/key";
    public static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
        "eventWithUnknownRequestInfo.json");
    public static final String UNDEFINED_REQUEST_INFO_PROPERTY = "body";
    public static final String PATH_DELIMITER = "/";
    public static final int AVOID_ROOT_NODE = 1;
    public static final int AVOID_LAST_NODE = 1;
    public static final int UNECESSARY_ROOT_NODE = 0;
    private static final String API_GATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    private static final Path NULL_VALUES_FOR_MAPS = Path.of(API_GATEWAY_MESSAGES_FOLDER,
        "mapParametersAreNull.json");
    private static final Path MISSING_MAP_VALUES = Path.of(API_GATEWAY_MESSAGES_FOLDER,
        "missingRequestInfo.json");
    public static final int FIRST_NODE = 0;

    @Test
    @DisplayName("RequestInfo can accept unknown fields")
    public void requestInfoAcceptsUnknownsFields() throws JsonProcessingException {
        String requestInfoString = IoUtils.stringFromResources(EVENT_WITH_UNKNOWN_REQUEST_INFO);
        RequestInfo requestInfo = objectMapper.readValue(requestInfoString, RequestInfo.class);

        assertThat(requestInfo.getOtherProperties(), hasKey(UNDEFINED_REQUEST_INFO_PROPERTY));
    }

    @Test
    @DisplayName("RequestInfo initializes queryParameters to empty map when JSON object sets "
        + "queryStringParameters to null")
    public void requestInfoInitializesQueryParametesToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getQueryParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when JSON object sets "
        + "Headers to null")
    public void requestInfoInitializesHeadersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getHeaders);
    }

    @Test
    @DisplayName("RequestInfo initializes pathParameters to empty map when JSON object sets "
        + "pathParameters to null")
    public void requestInfoInitializesPathParametersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getPathParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes requestContext to empty JsonNode when JSON object sets "
        + "requestContext to null")
    public void requestInfoInitializesRequestContextToEmptyJsonNodeWhenJsonObjectsSetsRequestContextToNull()
        throws JsonProcessingException {
        checkForNonNullMap(NULL_VALUES_FOR_MAPS, RequestInfo::getRequestContext);
    }

    @Test
    @DisplayName("RequestInfo initializes queryParameters to empty map queryStringParameters is missing")
    public void requestInfoInitializesQueryParametesToEmptyMapWhenQueryStringParametersIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getQueryParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when header parameter is missing")
    public void requestInfoInitializesHeadersToEmptyMapWhenHeadersParameterIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getHeaders);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when header parameter is missing")
    public void requestInfoInitializesHeadersToEmptyMapWhenPathPrametersParameterIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getPathParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes requestContext to empty JsonNode requestContext is missing")
    public void requestInfoInitializesRequestContextToEmptyJsonNodeWhenRequestContextIsMissing()
        throws JsonProcessingException {
        checkForNonNullMap(MISSING_MAP_VALUES, RequestInfo::getRequestContext);
    }

    @Test
    public void requestInfoReturnsUsernameForRequestContextWithCredentials() {
        RequestInfo requestInfo = new RequestInfo();

        String expectedUsername = "orestis";
        updateRequestContext(requestInfo, expectedUsername, RequestInfo.FEIDE_ID);
        String actual = requestInfo.getUsername().orElseThrow();
        assertEquals(actual, expectedUsername);
    }

    @Test
    public void requestInfoReturnsCustomerIdForRequestContextWithCredentials() {
        RequestInfo requestInfo = new RequestInfo();

        String expectedCustomerId = "customerId";
        updateRequestContext(requestInfo, expectedCustomerId, RequestInfo.CUSTOMER_ID);

        String actual = requestInfo.getCustomerId().orElseThrow();
        assertEquals(actual, expectedCustomerId);
    }

    @Test
    public void requestInfoReturnsAssignedRolesForRequestContextWithCredentials() {
        RequestInfo requestInfo = new RequestInfo();

        String expectedRoles = "role1,role2";
        updateRequestContext(requestInfo, expectedRoles, RequestInfo.APPLICATION_ROLES);

        String actual = requestInfo.getAssignedRoles().orElseThrow();
        assertEquals(actual, expectedRoles);
    }

    @Test
    public void canGetValueFromRequestContext() throws JsonProcessingException {

        Map<String, Map<String, Map<String, Map<String, String>>>> map = Map.of(
            REQUEST_CONTEXT_FIELD, Map.of(
                AUTHORIZER, Map.of(
                    CLAIMS, Map.of(
                        KEY, VALUE
                    )
                )
            )
        );

        RequestInfo requestInfo = objectMapper.readValue(objectMapper.writeValueAsString(map), RequestInfo.class);

        JsonPointer jsonPointer = JsonPointer.compile(JSON_POINTER);
        JsonNode jsonNode = requestInfo.getRequestContext().at(jsonPointer);

        assertFalse(jsonNode.isMissingNode());
        assertEquals(VALUE, jsonNode.textValue());
    }

    private void updateRequestContext(RequestInfo requestInfo, String expectedCustomerId, JsonPointer customerId) {
        ObjectNode requestContext = createNestedNodesFromJsonPointer(customerId, expectedCustomerId);
        requestInfo.setRequestContext(requestContext);
    }

    private ObjectNode createNestedNodesFromJsonPointer(JsonPointer jsonPointer, String value) {
        var nodeList = createNodesForEachPathElement(jsonPointer);
        nestNodes(nodeList);
        var lastEntry = nodeList.get(lastItem(nodeList));
        insertValueToLeafNode(value, lastEntry);

        return nodeList.get(FIRST_NODE).getValue();
    }

    private List<SimpleEntry<String, ObjectNode>> createNodesForEachPathElement(JsonPointer jsonPointer) {
        var nodes = createListWithEmptyObjectNodes(jsonPointer);
        nodes.remove(UNECESSARY_ROOT_NODE);
        return nodes;
    }

    private void nestNodes(List<SimpleEntry<String, ObjectNode>> nodes) {
        for (int i = 0; i < lastItem(nodes); i++) {
            var currentEntry = nodes.get(i);
            var nextEntry = nodes.get(i + 1);
            addNextEntryAsChildToCurrentEntry(currentEntry, nextEntry);
        }
    }

    private void insertValueToLeafNode(String value, SimpleEntry<String, ObjectNode> lastEntry) {
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
            .map(nodeName -> new SimpleEntry<>(nodeName, objectMapper.createObjectNode()))
            .collect(Collectors.toList());
    }

    private int lastItem(List<SimpleEntry<String, ObjectNode>> nodes) {
        return nodes.size() - 1;
    }

    private void checkForNonNullMap(Path resourceFile, Function<RequestInfo, Object> getObject)
        throws JsonProcessingException {
        String apiGatewayEvent = IoUtils.stringFromResources(resourceFile);
        RequestInfo requestInfo = objectMapper.readValue(apiGatewayEvent, RequestInfo.class);
        assertNotNull(getObject.apply(requestInfo));
    }
}

