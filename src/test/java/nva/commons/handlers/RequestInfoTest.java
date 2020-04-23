package nva.commons.handlers;

<<<<<<< HEAD
import static nva.commons.utils.JsonUtils.jsonParser;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RequestInfoTest {

    private static final String API_GATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    private static final Path NULL_VALUES_FOR_MAPS = Path.of(API_GATEWAY_MESSAGES_FOLDER,
        "queryStringParametersIsNull.json");

    @Test
    @DisplayName("RequestInfo initializes queryParameters to empty map when JSON object sets "
        + "queryStringParameters to null")
    public void requestInfoInitializesQueryParametesToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(RequestInfo::getQueryParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when JSON object sets "
        + "Headers to null")
    public void requestInfoInitializesHeadersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(RequestInfo::getHeaders);
    }

    @Test
    @DisplayName("RequestInfo initializes pathParameters to empty map when JSON object sets "
        + "pathParameters to null")
    public void requestInfoInitializesPathParametersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(RequestInfo::getPathParameters);
    }

    private void checkForNonNullMap(Function<RequestInfo, Map<String, String>> getMap) throws JsonProcessingException {
        String apiGatewayEvent = IoUtils.stringFromResources(NULL_VALUES_FOR_MAPS);
        RequestInfo requestInfo = jsonParser.readValue(apiGatewayEvent, RequestInfo.class);
        assertNotNull(getMap.apply(requestInfo));
    }
}
=======
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RequestInfoTest {

    public static final String REQUEST_CONTEXT = "requestContext";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String JSON_POINTER = "/authorizer/claims/key";

    private ObjectMapper objectMapper = JsonUtils.jsonParser;

    @Test
    public void canGetValueFromRequestContext() throws JsonProcessingException {

        Map<String, Map<String, Map<String, Map<String, String>>>> map = Map.of(
            REQUEST_CONTEXT, Map.of(
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

}
>>>>>>> 17f8802c9e585c2e86de06889437908da4cf6d30
