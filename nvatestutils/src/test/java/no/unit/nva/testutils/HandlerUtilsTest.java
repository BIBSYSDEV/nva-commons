package no.unit.nva.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HandlerUtilsTest {

    public static final String VALUE = "value";
    public static final String SOME_KEY = "SomeKey";
    public static final String SOME_HEADER_VALUE = "SomeHeaderValue";
    public static final String SOME_QUERY_VALUE = "SomeQueryValue";
    public static final String SOME_PATH_VALUE = "SomePathValue";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void requestObjectToApiGatewayRequestStringReturnsValidJsonObjectForNullHeaders()
        throws JsonProcessingException {
        String requestString = gatewayRequestWithNullHeaders();
        assertNotNull(requestString);
        RequestBody actual = extractBodyFromSerializedRequest(requestString);
        assertThat(actual.getMyField(), is(equalTo(VALUE)));
    }

    @Test
    public void requestObjectToApiGatewayRequestStringReturnsValidJsonObjectForNonPathParameters()
        throws JsonProcessingException {
        String requestString = gatewayRequestWithPathAndQueryParameters();
        assertNotNull(requestString);
        RequestBody actual = extractBodyFromSerializedRequest(requestString);
        JsonNode json = OBJECT_MAPPER.readTree(requestString);
        TypeReference<Map<String, String>> type = new TypeReference<>() {
        };
        Map<String, String> headers = OBJECT_MAPPER.convertValue(json.get(HandlerUtils.HEADERS_FIELD), type);
        Map<String, String> pathParameters = OBJECT_MAPPER.convertValue(json.get(HandlerUtils.PATH_PARAMETERS), type);
        Map<String, String> queryParameters = OBJECT_MAPPER.convertValue(json.get(HandlerUtils.QUERY_PARAMETERS), type);

        assertThat(actual.getMyField(), is(equalTo(VALUE)));
        assertThat(headers, hasEntry(SOME_KEY, SOME_HEADER_VALUE));
        assertThat(pathParameters, hasEntry(SOME_KEY, SOME_PATH_VALUE));
        assertThat(queryParameters, hasEntry(SOME_KEY, SOME_QUERY_VALUE));
    }

    private String gatewayRequestWithPathAndQueryParameters() throws JsonProcessingException {
        RequestBody requestBody = new RequestBody();
        requestBody.setMyField(VALUE);
        Map<String, String> headers = new HashMap<>();
        Map<String, String> pathParams = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        headers.put(SOME_KEY, SOME_HEADER_VALUE);
        pathParams.put(SOME_KEY, SOME_PATH_VALUE);
        queryParams.put(SOME_KEY, SOME_QUERY_VALUE);
        return new HandlerUtils().requestObjectToApiGatewayRequestString(requestBody, headers, pathParams, queryParams);
    }

    private String gatewayRequestWithNullHeaders() throws JsonProcessingException {
        RequestBody requestBody = new RequestBody();
        requestBody.setMyField(VALUE);
        return new HandlerUtils().requestObjectToApiGatewayRequestString(requestBody, null, null, null);
    }

    private RequestBody extractBodyFromSerializedRequest(String requestString) throws JsonProcessingException {
        JsonNode json = OBJECT_MAPPER.readTree(requestString);
        String body = json.get(HandlerUtils.BODY_FIELD).textValue();
        return OBJECT_MAPPER.readValue(body, RequestBody.class);
    }

    private static class RequestBody {

        private String myField;

        public String getMyField() {
            return myField;
        }

        public void setMyField(String myField) {
            this.myField = myField;
        }
    }
}