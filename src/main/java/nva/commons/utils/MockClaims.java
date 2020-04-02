package nva.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MockClaims {

    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";

    public JsonNode apiGatewayEvent(JsonNode event, ObjectMapper objectMapper){
        JsonNode copy= event.deepCopy();
        ObjectNode requestContext =(ObjectNode) copy.at("/requestContext");
        requestContext.set("authorizer",objectMapper.createObjectNode());
        ObjectNode authorizer= (ObjectNode) requestContext.get("authorizer");
        authorizer.set("claims",objectMapper.createObjectNode());
        ObjectNode claims = (ObjectNode) authorizer.get("claims");
        claims.put(CUSTOM_FEIDE_ID,"none@unit.no");
        claims.put(CUSTOM_ORG_NUMBER,"NO919477822");
        return copy;
    }
}
