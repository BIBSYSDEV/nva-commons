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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.modelmbean.XMLParseException;
import nva.commons.Handler;
import nva.commons.RequestBody;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.TestException;
import nva.commons.hanlders.ApiGatewayHandler;
import nva.commons.hanlders.GatewayResponse;
import nva.commons.hanlders.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.TestLogger;
import nva.commons.utils.attempt.Try;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

public class ApiGatewayHandlerTest {

    public static final String SOME_ENV_VALUE = "SomeEnvValue";
    private static final String PATH = "path1/path2/path3";
    public static final String TOP_EXCEPTION_MESSAGE = "Third Exception";
    public static final String MIDDLE_EXCEPTION_MESSAGE = "Second Exception";
    public static final String BOTTOM_EXCEPTION_MESSAGE = "First Exception";
    public static final String SOME_REQUEST_ID = "RequestID:123456";
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
        when(context.getAwsRequestId()).thenReturn(SOME_REQUEST_ID);
    }

    @Test
    @DisplayName("handleRequest should have available the request headers")
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
        assertThat(logs, containsString(BOTTOM_EXCEPTION_MESSAGE));
        assertThat(logs, containsString(MIDDLE_EXCEPTION_MESSAGE));
        assertThat(logs, containsString(TOP_EXCEPTION_MESSAGE));
        assertThat(logs, containsString(context.getAwsRequestId()));
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
        assertThat(logs, containsString(BOTTOM_EXCEPTION_MESSAGE));
    }

    @Test
    @DisplayName("Failure message contains exception message and status code when an Exception is thrown")
    public void failureMessageContainsExceptionMessageAndStatusCodeWhenAnExceptionIsThrown()
        throws IOException {
        Handler handler = handlerThatThrowsExceptions();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(requestWithHeadersAndPath(), outputStream, context);

        String output = outputStream.toString(StandardCharsets.UTF_8);

        Try<ThrowableProblem> responseParsing = tryParsingResponse(output);
        assertThat(responseParsing.isSuccess(), is(true));

        ThrowableProblem problem = responseParsing.get();

        assertThat(problem.getMessage(), containsString(TOP_EXCEPTION_MESSAGE));
        assertThat(problem.getMessage(), containsString(Status.NOT_FOUND.getReasonPhrase()));

        String requestId = extractRequestId(problem);
        assertThat(requestId, is(equalTo(SOME_REQUEST_ID)));
        assertThat(problem.getStatus(), is(Status.NOT_FOUND));
        assertThat(output, containsString(new TestException("").getStatusCode().toString()));
    }

    private String extractRequestId(ThrowableProblem problem) {
        return Optional.ofNullable(problem.getParameters().get(ApiGatewayHandler.REQUEST_ID))
                       .map(Object::toString).orElse(null);
    }

    private Try<ThrowableProblem> tryParsingResponse(String output) {
        return Try.of(output)
                  .map(str -> jsonParser.readValue(str, JsonNode.class))
                  .map(node -> node.get("body"))
                  .map(body -> jsonParser.convertValue(body, ThrowableProblem.class));
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
            throw new IllegalStateException(BOTTOM_EXCEPTION_MESSAGE);
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
            throw new URISyntaxException("", BOTTOM_EXCEPTION_MESSAGE);
        } catch (URISyntaxException e) {
            try {
                throw new XMLParseException(e, MIDDLE_EXCEPTION_MESSAGE);
            } catch (XMLParseException ex) {
                throw new TestException(ex, TOP_EXCEPTION_MESSAGE);
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
