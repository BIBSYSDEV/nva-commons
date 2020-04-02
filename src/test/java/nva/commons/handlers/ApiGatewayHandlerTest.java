package nva.commons.handlers;

import static nva.commons.utils.JsonUtils.jsonParser;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.modelmbean.XMLParseException;
import nva.commons.Handler;
import nva.commons.RequestBody;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.TestException;
import nva.commons.hanlders.GatewayResponse;
import nva.commons.hanlders.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.TestLogger;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ApiGatewayHandlerTest {

    public static final String THE_PROXY = "theProxy";
    public static final String SOME_ENV_VALUE = "SomeEnvValue";
    private static final String PATH = "path1/path2/path3";
    public static final String THIRD_EXCEPTION_MESSAGE = "Third Exception";
    public static final String SECOND_EXCEPTION_MESSAGE = "Second Exception";
    public static final String FIRST_EXCEPTION_MESSAGE = "First Exception";
    public Environment environment;
    private Context context;
    private TestLogger logger = new TestLogger();

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn(SOME_ENV_VALUE);
        context = mock(Context.class);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void handleRequestShouldHaveAvailableTheRequestHeaders() throws IOException {
        Handler handler = new Handler(environment);
        InputStream input = requestWithHeaders();

        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(input, outputStream, context);

        Map<String, String> headers = handler.getHeaders();
        JsonNode expectedHeaders = createHeaders();
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader -> {
            assertThat(headers.get(expectedHeader), is(equalTo(expectedHeaders.get(expectedHeader).textValue())));
        });
    }

    @Test
    @DisplayName("handleRequest should have available the request path")
    public void handleRequestShouldHaveAvailableTheRequestPath() throws IOException {
        Handler handler = new Handler(environment);
        InputStream input = requestWithHeadersAndPath();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(input, outputStream, context);

        String actualPath = handler.getPath();
        JsonNode expectedHeaders = createHeaders();
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader -> {
            assertThat(actualPath, is(equalTo(PATH)));
        });
    }

    @Test
    @DisplayName("handleRequest allows response headers depended on input")
    public void apiGatewayHandlerAllowsResponseHeadersDependedOnInput() throws IOException {
        Handler handler = new Handler(environment);
        InputStream input = requestWithHeadersAndPath();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(input, outputStream, context);
        String outputString = outputStream.toString(StandardCharsets.UTF_8);
        GatewayResponse<String> response = jsonParser.readValue(outputString, GatewayResponse.class);
        assertTrue(response.getHeaders().containsKey(HttpHeaders.WARNING));
    }

    @Test
    @DisplayName("Logger logs the whole exception stacktrace when an exception has occurred")
    public void loggerLogsTheWholeExceptionStackTraceWhenAnExceptionHasOccurred() throws IOException {
        Handler handler = handlerThatThrowsExceptions();

        handler.handleRequest(requestWithHeadersAndPath(), outputStream(), context);
        TestLogger logger = (TestLogger) context.getLogger();
        String logs = logger.getLogs();

        assertThat(logs, containsString(URISyntaxException.class.getName()));
        assertThat(logs, containsString(XMLParseException.class.getName()));
        assertThat(logs, containsString(TestException.class.getName()));
        assertThat(logs, containsString(FIRST_EXCEPTION_MESSAGE));
        assertThat(logs, containsString(SECOND_EXCEPTION_MESSAGE));
        assertThat(logs, containsString(THIRD_EXCEPTION_MESSAGE));
    }

    @Test
    @DisplayName("Logger logs the whole exception stacktrace when an exception has occurred")
    public void loggerLogsTheWholeExceptionStackTraceWhenAnUncheckedExceptionHasOccurred() throws IOException {
        Handler handler = handlerThatThrowsUncheckedExceptions();

        handler.handleRequest(requestWithHeadersAndPath(), outputStream(), context);
        TestLogger logger = (TestLogger) context.getLogger();
        String logs = logger.getLogs();

        assertThat(logs, containsString(IllegalStateException.class.getName()));
        assertThat(logs, containsString(IllegalArgumentException.class.getName()));
        assertThat(logs, containsString(IllegalCallerException.class.getName()));
        assertThat(logs, containsString(FIRST_EXCEPTION_MESSAGE));
    }

    private Handler handlerThatThrowsUncheckedExceptions() {
        return new Handler(environment) {
            @Override
            protected String processInput(RequestBody input, RequestInfo requestInfo, Context context)
                throws ApiGatewayException {
                throwUncheckedExceptions();
                return null;
            }
        };
    }

    private Handler handlerThatThrowsExceptions() {
        return new Handler(environment) {
            @Override
            protected String processInput(RequestBody input, RequestInfo requestInfo, Context context)
                throws ApiGatewayException {
                throwExceptions();
                return null;
            }
        };
    }

    private void throwUncheckedExceptions() {
        try {
            throw new IllegalStateException(FIRST_EXCEPTION_MESSAGE);
        } catch (IllegalStateException e) {
            try {
                throw new IllegalArgumentException(e);
            } catch (IllegalArgumentException ex) {
                throw new IllegalCallerException(ex);
            }
        }
    }

    private void throwExceptions() throws ApiGatewayException {
        try {
            throw new URISyntaxException("", FIRST_EXCEPTION_MESSAGE);
        } catch (URISyntaxException e) {
            try {
                throw new XMLParseException(e, SECOND_EXCEPTION_MESSAGE);
            } catch (XMLParseException ex) {
                throw new TestException(ex, THIRD_EXCEPTION_MESSAGE);
            }
        }
    }

    private InputStream requestWithHeadersAndPath() throws JsonProcessingException {
        ObjectNode request = jsonParser.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders());
        request.put("path", PATH);
        return jsonNodeToInputStream(request);
    }

    private InputStream jsonNodeToInputStream(JsonNode request) throws JsonProcessingException {
        String requestString = jsonParser.writeValueAsString(request);
        return IoUtils.stringToStream(requestString);
    }

    private InputStream requestWithHeaders() throws JsonProcessingException {
        ObjectNode request = jsonParser.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders());
        return jsonNodeToInputStream(request);
    }

    private JsonNode createHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return jsonParser.convertValue(headers, JsonNode.class);
    }

    private ObjectNode createBody() {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("value1");
        requestBody.setField2("value2");
        return jsonParser.convertValue(requestBody, ObjectNode.class);
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }
}
