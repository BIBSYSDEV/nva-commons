package nva.commons.apigateway;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static nva.commons.apigateway.testutils.ProxyHandler.HTTP_STATUS_CODE_TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.testutils.ProxyHandler;
import nva.commons.apigateway.testutils.RequestBody;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiGatewayProxyHandlerTest {
    private Context context;
    private ProxyHandler handler;

    @BeforeEach
    public void setup() {
        context = new FakeContext();
        handler = new ProxyHandler();
    }

    @Test
    @DisplayName("ApiGatewayProxyHandlerTest has a constructor with input class as only parameter")
    public void apiGatewayHandlerHasACostructorWithInputClassAsOnlyParameter() {
        RestRequestHandler<String, String> handler = new ApiGatewayProxyHandler<>(String.class) {
    
            @Override
            protected ProxyResponse<String> processProxyInput(String input, RequestInfo requestInfo, Context context) {
                return null;
            }
        };
    }

    @Test
    @DisplayName("handleRequest should get the status and body")
    public void handleRequestShouldGetTheStatusCodeAndBody() throws IOException {
        var input = createBody();
        var expectedOutput = objectMapper.convertValue(input, RequestBody.class);
        var inputStream = requestWithHeaders(input);
        var outputStream = outputStream();

        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var actual = dtoObjectMapper.readValue(response.getBody(), RequestBody.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_STATUS_CODE_TEST)));
        assertThat(actual, is(equalTo(expectedOutput)));

    }

    private InputStream jsonNodeToInputStream(JsonNode request) throws JsonProcessingException {
        String requestString = defaultRestObjectMapper.writeValueAsString(request);
        return IoUtils.stringToStream(requestString);
    }

    private InputStream requestWithHeaders(ObjectNode input) throws JsonProcessingException {
        ObjectNode request = defaultRestObjectMapper.createObjectNode();
        request.set("body", input);
        request.set("headers", createHeaders());
        return jsonNodeToInputStream(request);
    }

    private JsonNode createHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
        headers.put(CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
        return createHeaders(headers);
    }

    private JsonNode createHeaders(Map<String, String> headers) {
        return defaultRestObjectMapper.convertValue(headers, JsonNode.class);
    }

    private ObjectNode createBody() {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("value1");
        requestBody.setField2("value2");
        return defaultRestObjectMapper.convertValue(requestBody, ObjectNode.class);
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

}
