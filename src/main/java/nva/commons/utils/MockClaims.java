package nva.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Mocks the user claims. For usage when we are running a lambda locally through SAM. In the current version, the method
 * has to be called by the locally run handler, in the {@code handleRequest) method or the {@code processInput method}.
 * <p>
 * <p>
 * Example for the processInputMethod.
 *
 * <pre>
 *  public class MyHandler extends ApiGatewayHandler<RequestBody,String>{
 *
 *     &#64;Override
 *     protected String processInput(RequestBody input, RequestInfo requestInfo, Context context)
 *         throws ApiGatewayException {
 *         ... do the processing here...
 *     }
 *
 *     &#64;Override
 *     protected final String processInput(RequestBody input, String apiGatewayInputString, Context context)
 *     throws ApiGatewayException {
 *         JsonNode event = jsonParser.readTree(apiGatewayInputString);
 *         JsonNode eventWithClaims = MockClaims.apiGatewayEvent(event,jsonParser);
 *         String eventWithClaimsStr = jsonParser.writeValueAsString(eventWithClaims);
 *         RequestInfo requestInfo = inputParser.getRequestInfo(eventWithClaimsStr);
 *         return processInput(input, requestInfo, context);
 *     }
 * }
 * </pre>
 */
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
     * Mocks the user claims. For usage when we are running a lambda locally through SAM. In the current version, the
     * method has to be called by the locally run handler, in the  {@code handleRequest} method or the
     * {@code processInput} method.
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
