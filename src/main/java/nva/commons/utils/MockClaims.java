package nva.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MockClaims {

    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";
    public static final String REQUEST_CONTEXT_ROOT_NODE = "/requestContext";
    public static final String AUTHORIZER_NODE = "authorizer";
    public static final String CLAIMS_NODE = "claims";

    /**
     *  Mocks the user claims. For usage when we running it a lambda locally through SAM.
     * @param event the ApiGateway event
     * @param objectMapper a jsonParser
     * @return an event with mocked claims
     */
    public JsonNode apiGatewayEvent(JsonNode event, ObjectMapper objectMapper) {
        JsonNode copy = event.deepCopy();
        ObjectNode requestContext = (ObjectNode) copy.at(REQUEST_CONTEXT_ROOT_NODE);
        requestContext.set(AUTHORIZER_NODE, objectMapper.createObjectNode());
        ObjectNode authorizer = (ObjectNode) requestContext.get(AUTHORIZER_NODE);
        authorizer.set(CLAIMS_NODE, objectMapper.createObjectNode());
        ObjectNode claims = (ObjectNode) authorizer.get(CLAIMS_NODE);
        claims.put(CUSTOM_FEIDE_ID, "none@unit.no");
        claims.put(CUSTOM_ORG_NUMBER, "NO919477822");
        return copy;
    }
}
