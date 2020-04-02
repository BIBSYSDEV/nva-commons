package nva.commons.utils;

import static nva.commons.utils.JsonUtils.jsonParser;
import static nva.commons.utils.MockClaims.AUTHORIZER_NODE;
import static nva.commons.utils.MockClaims.CLAIMS_NODE;
import static nva.commons.utils.MockClaims.CUSTOM_FEIDE_ID;
import static nva.commons.utils.MockClaims.CUSTOM_ORG_NUMBER;
import static nva.commons.utils.MockClaims.REQUEST_CONTEXT_ROOT_NODE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class MockClaimsTest {

    public static final String PATH_TEMPLATE = "/%s/%s/%s/%s";

    @Test
    public void mockClaimsContainsCustomFeideId(){
        ObjectNode root = mockContext();
        JsonNode mockEvent = MockClaims.apiGatewayEvent(root, jsonParser);
        String fieldIdClaimPath= String.format(PATH_TEMPLATE, REQUEST_CONTEXT_ROOT_NODE,
            AUTHORIZER_NODE,
            CLAIMS_NODE,
            CUSTOM_FEIDE_ID);
        String actualFeideId = mockEvent.at(fieldIdClaimPath).textValue();
        assertThat(actualFeideId,is(equalTo(MockClaims.MOCK_FEIDE_ID)));
    }

    @Test
    public void mockClaimsContainsCustomOrgNumber(){
        ObjectNode root = mockContext();
        JsonNode mockEvent = MockClaims.apiGatewayEvent(root, jsonParser);
        String fieldIdClaimPath= String.format(PATH_TEMPLATE, REQUEST_CONTEXT_ROOT_NODE,
            AUTHORIZER_NODE,
            CLAIMS_NODE,
            CUSTOM_ORG_NUMBER);
        String actualFeideId = mockEvent.at(fieldIdClaimPath).textValue();
        assertThat(actualFeideId,is(equalTo(MockClaims.UNIT_ORG_NUMBER)));
    }

    private ObjectNode mockContext() {
        ObjectNode root = jsonParser.createObjectNode();
        root.set(REQUEST_CONTEXT_ROOT_NODE,jsonParser.createObjectNode());
        return root;
    }
}
