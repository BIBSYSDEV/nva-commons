package nva.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class MockClaims {

    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";
    public static final String REQUEST_CONTEXT_NODE = "requestContext";
    public static final String AUTHORIZER_NODE = "authorizer";
    public static final String CLAIMS_NODE = "claims";
    public static final String MOCK_FEIDE_ID = "none@unit.no";
    public static final String UNIT_ORG_NUMBER = "NO919477822";

    private MockClaims() {
    }

    /**
     * Mocks the user claims. For usage when we running it a lambda locally through SAM.
     *
     * @param event      the ApiGateway event
     * @param jsonParser a jsonParser
     * @return an event with mocked claims
     */
    public static JsonNode apiGatewayEvent(JsonNode event, ObjectMapper jsonParser) {
        ObjectNode copy = event.deepCopy();

        if (!copy.has(REQUEST_CONTEXT_NODE)) {
            copy.set(REQUEST_CONTEXT_NODE, jsonParser.createObjectNode());
        }

        ObjectNode requestContext = (ObjectNode) copy.get(REQUEST_CONTEXT_NODE);
        requestContext.set(AUTHORIZER_NODE, jsonParser.createObjectNode());
        ObjectNode authorizer = (ObjectNode) requestContext.get(AUTHORIZER_NODE);
        authorizer.set(CLAIMS_NODE, jsonParser.createObjectNode());
        ObjectNode claims = (ObjectNode) authorizer.get(CLAIMS_NODE);
        claims.put(CUSTOM_FEIDE_ID, MOCK_FEIDE_ID);
        claims.put(CUSTOM_ORG_NUMBER, UNIT_ORG_NUMBER);
        return copy;
    }
}
