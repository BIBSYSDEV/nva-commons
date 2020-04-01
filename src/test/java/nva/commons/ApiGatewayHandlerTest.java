package nva.commons;

import static nva.commons.utils.JsonUtils.jsonParser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import nva.commons.testutils.TestLogger;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApiGatewayHandlerTest {

    public static final String THE_PROXY = "theProxy";
    public static final String SOME_ENV_VALUE = "SomeEnvValue";
    private static final String PATH = "path1/path2/path3" ;
    public Environment environment;
    private Context context;
    private TestLogger logger = new TestLogger();



    @BeforeEach
    public void setup(){
        environment =  mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn(SOME_ENV_VALUE);
        context = mock(Context.class);
        when(context.getLogger()).thenReturn(logger);
    }


    @Test
    public void handleRequestShouldHaveAvailableTheRequestHeaders() throws IOException {
        Handler handler= new Handler(environment);
        ObjectNode request = requestWithHeaders();

        String requestString = jsonParser.writeValueAsString(request);
        InputStream input=IoUtils.stringToStream(requestString);
        ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
        handler.handleRequest(input,outputStream,context);

        Map<String, String> headers = handler.getHeaders();
        JsonNode expectedHeaders= createHeaders();
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader->{
            assertThat(headers.get(expectedHeader),is(equalTo(expectedHeaders.get(expectedHeader).textValue())));
        });
    }


    @Test
    public void handleRequestShouldHaveAvailableTheRequestPath() throws IOException {
        Handler handler= new Handler(environment);
        ObjectNode request = requestWithHeadersAndPath();

        String requestString = jsonParser.writeValueAsString(request);
        InputStream input=IoUtils.stringToStream(requestString);
        ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
        handler.handleRequest(input,outputStream,context);

        String actualPath = handler.getPath();
        JsonNode expectedHeaders= createHeaders();
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader->{
            assertThat(actualPath,is(equalTo(PATH)));
        });
    }

    @Test
    public void apiGatewayHandlerAllowsResponseHeadersDepndendedOnInput() throws IOException {
        Handler handler= new Handler(environment);
        ObjectNode request = requestWithHeadersAndPath();

        String requestString = jsonParser.writeValueAsString(request);
        InputStream input=IoUtils.stringToStream(requestString);
        ByteArrayOutputStream outputStream= new ByteArrayOutputStream();
        handler.handleRequest(input,outputStream,context);
        String outputString = outputStream.toString(StandardCharsets.UTF_8);
        GatewayResponse<String> response=jsonParser.readValue(outputString,GatewayResponse.class);
        assertTrue(response.getHeaders().containsKey(HttpHeaders.WARNING));
    }


    private ObjectNode requestWithHeadersAndPath() {
        ObjectNode request= jsonParser.createObjectNode();
        ObjectNode node = createBody();
        request.set("body",node);
        request.set("headers",createHeaders());
        request.put("path",PATH);
        return request;
    }


    private ObjectNode requestWithHeaders() {
        ObjectNode request= jsonParser.createObjectNode();
        ObjectNode node = createBody();
        request.set("body",node);
        request.set("headers",createHeaders());
        return request;
    }

    private JsonNode createHeaders() {
        Map<String,String> headers = new HashMap<String,String>();
        headers.put(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return jsonParser.convertValue(headers,JsonNode.class);
    }

    private ObjectNode createBody() {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("value1");
        requestBody.setField2("value2");
        return jsonParser.convertValue(requestBody, ObjectNode.class);
    }




}
