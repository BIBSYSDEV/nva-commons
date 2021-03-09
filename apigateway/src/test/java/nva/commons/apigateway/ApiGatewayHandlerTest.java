package nva.commons.apigateway;

import static nva.commons.apigateway.ApiGatewayHandler.APPLICATION_PROBLEM_JSON;
import static nva.commons.apigateway.ApiGatewayHandler.CONTENT_TYPE;
import static nva.commons.apigateway.ApiGatewayHandler.REQUEST_ID;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.modelmbean.XMLParseException;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.InvalidOrMissingTypeException;
import nva.commons.apigateway.exceptions.TestException;
import nva.commons.apigateway.testutils.Handler;
import nva.commons.apigateway.testutils.RequestBody;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

public class ApiGatewayHandlerTest {

    public static final String SOME_ENV_VALUE = "SomeEnvValue";
    public static final String TOP_EXCEPTION_MESSAGE = "TOP Exception";
    public static final String MIDDLE_EXCEPTION_MESSAGE = "MIDDLE Exception";
    public static final String BOTTOM_EXCEPTION_MESSAGE = "BOTTOM Exception";
    public static final String SOME_REQUEST_ID = "RequestID:123456";
    public static final int OVERRIDEN_STATUS_CODE = 418;  //I'm a teapot
    public static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
                                                                       "eventWithUnknownRequestInfo.json");
    public Environment environment;
    private static final String PATH = "path1/path2/path3";
    private Context context;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn(SOME_ENV_VALUE);
        context = mock(Context.class);
        when(context.getAwsRequestId()).thenReturn(SOME_REQUEST_ID);
    }

    @Test
    @DisplayName("ApiGatewayHandler has a constructor with input class as only parameter")
    public void apiGatewayHandlerHasACostructorWithInputClassAsOnlyParameter() {
        RestRequestHandler<String, String> handler = new ApiGatewayHandler<>(String.class) {
            @Override
            protected String processInput(String input, RequestInfo requestInfo, Context context) {
                return null;
            }

            @Override
            protected Integer getSuccessStatusCode(String input, String output) {
                return HttpURLConnection.HTTP_OK;
            }
        };
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
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader ->
                                                          assertThat(headers.get(expectedHeader), is(equalTo(
                                                              expectedHeaders.get(expectedHeader).textValue()))));
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
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader -> assertThat(actualPath, is(equalTo(PATH))));
    }

    @Test
    @DisplayName("handleRequest allows response headers depended on input")
    public void apiGatewayHandlerAllowsResponseHeadersDependedOnInput() throws IOException {
        Handler handler = new Handler(environment);
        InputStream input = requestWithHeadersAndPath();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(input, outputStream, context);
        GatewayResponse<String> response = GatewayResponse.fromOutputStream(outputStream);
        assertTrue(response.getHeaders().containsKey(HttpHeaders.WARNING));
    }

    @Test
    public void handleRequestDoesNotThrowExceptionOnUnknownFieldsInRequestInfo() throws IOException {
        Handler handler = new Handler(environment);
        InputStream input = IoUtils.inputStreamFromResources(EVENT_WITH_UNKNOWN_REQUEST_INFO);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(input, output, context);

        GatewayResponse<String> response = GatewayResponse.fromOutputStream(output);
        String body = response.getBodyObject(String.class);

        String expectedValueForField1 = "value1";
        String expectedValueForField2 = "value2";
        assertThat(body, containsString(expectedValueForField1));
        assertThat(body, containsString(expectedValueForField2));
    }

    @Test
    @DisplayName("Logger logs the whole exception stacktrace when an exception has occurred 2")
    public void loggerLogsTheWholeExceptionStackTraceWhenAnExceptionHasOccurred() throws IOException {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        Handler handler = handlerThatThrowsExceptions();
        handler.handleRequest(requestWithHeadersAndPath(), outputStream(), context);

        String logs = appender.getMessages();

        assertThat(logs, containsString(URISyntaxException.class.getName()));
        assertThat(logs, containsString(XMLParseException.class.getName()));
        assertThat(logs, containsString(TestException.class.getName()));
        assertThat(logs, containsString(BOTTOM_EXCEPTION_MESSAGE));
        assertThat(logs, containsString(MIDDLE_EXCEPTION_MESSAGE));
        assertThat(logs, containsString(TOP_EXCEPTION_MESSAGE));
    }

    @Test
    @DisplayName("Logger logs the whole exception stacktrace when an exception has occurred")
    public void loggerLogsTheWholeExceptionStackTraceWhenAnUncheckedExceptionHasOccurred() throws IOException {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        Handler handler = handlerThatThrowsUncheckedExceptions();

        handler.handleRequest(requestWithHeadersAndPath(), outputStream(), context);

        String logs = appender.getMessages();

        assertThat(logs, containsString(IllegalStateException.class.getName()));
        assertThat(logs, containsString(IllegalArgumentException.class.getName()));
        assertThat(logs, containsString(IllegalCallerException.class.getName()));
        assertThat(logs, containsString(BOTTOM_EXCEPTION_MESSAGE));
    }

    @Test
    @DisplayName("Handler does not reveal information for runtime exceptions")
    public void handlerDoesnRevealInformationForRuntimeExceptions() throws IOException {
        Handler handler = handlerThatThrowsUncheckedExceptions();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(requestWithHeaders(), outputStream, context);
        Problem problem = getProblemFromFailureResponse(outputStream);
        String details = problem.getDetail();
        assertThat(details, not(containsString(BOTTOM_EXCEPTION_MESSAGE)));
        assertThat(details, not(containsString(MIDDLE_EXCEPTION_MESSAGE)));
        assertThat(details, not(containsString(TOP_EXCEPTION_MESSAGE)));
        assertThat(details, containsString(
            ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("Failure message contains exception message and status code when an Exception is thrown")
    public void failureMessageContainsExceptionMessageAndStatusCodeWhenAnExceptionIsThrown()
        throws IOException {
        Handler handler = handlerThatThrowsExceptions();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(requestWithHeadersAndPath(), outputStream, context);

        GatewayResponse<Problem> responseParsing = getApiGatewayResponse(outputStream);

        Problem problem = responseParsing.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(TOP_EXCEPTION_MESSAGE));
        assertThat(problem.getTitle(), containsString(Status.NOT_FOUND.getReasonPhrase()));

        assertThat(problem.getParameters().get(REQUEST_ID), is(equalTo(SOME_REQUEST_ID)));
        assertThat(problem.getStatus(), is(Status.NOT_FOUND));
    }

    @Test
    @DisplayName("Failure message contains application/problem+json ContentType when an Exception is thrown")
    public void failureMessageContainsApplicationProblemJsonContentTypeWhenExceptionIsThrown()
        throws IOException {
        Handler handler = handlerThatThrowsExceptions();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(requestWithHeadersAndPath(), outputStream, context);

        GatewayResponse<Problem> responseParsing = getApiGatewayResponse(outputStream);

        assertThat(responseParsing.getHeaders().get(CONTENT_TYPE), is(equalTo(APPLICATION_PROBLEM_JSON)));
    }

    @Test
    @DisplayName("getFailureStatusCode sets the status code to the ApiGatewayResponse")
    public void getFailureStatusCodeSetsTheStatusCodeToTheApiGatewayResponse() throws IOException {
        Handler handler = handlerThatOverridesGetFailureStatusCode();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(anyRequest(), outputStream, context);
        GatewayResponse<Problem> response = getApiGatewayResponse(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(OVERRIDEN_STATUS_CODE)));
        assertThat(response.getBodyObject(Problem.class).getStatus().getStatusCode(),
                   is(equalTo(OVERRIDEN_STATUS_CODE)));
    }

    @Test
    @DisplayName("getFailureStatusCode returns by default the status code of the ApiGatewayException")
    public void getFailureStatusCodeReturnsByDefaultTheStatusCodeOfTheApiGatewayException() throws IOException {
        Handler handler = handlerThatThrowsExceptions();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(anyRequest(), outputStream, context);
        GatewayResponse<Problem> response = getApiGatewayResponse(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(TestException.ERROR_STATUS_CODE)));
        assertThat(response.getBodyObject(Problem.class).getStatus().getStatusCode(),
                   is(equalTo(TestException.ERROR_STATUS_CODE)));
    }

    @Test
    public void handlerReturnsBadRequestForJsonInvalidTypeError() throws IOException {
        Handler handler = new Handler(environment);

        InputStream inputStream = requestWithBodyWithoutType();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        Problem details = response.getBodyObject(Problem.class);
        assertThat(details.getDetail(), containsString(InvalidOrMissingTypeException.MESSAGE));
    }

    @Test
    void handlerSerializesBodyWithNonDefaultSerializationWhenDefaultSerializerIsOverridden() throws IOException {
        ObjectMapper spiedMapper = spy(JsonUtils.objectMapperWithEmpty);
        var handler = new Handler(environment, spiedMapper);
        var inputStream = requestWithHeaders();
        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        verify(spiedMapper, atLeast(1)).writeValueAsString(any());
    }

    @Test
    public void handlerLogsRequestIdForEveryRequest() throws IOException {
        var appender = LogUtils.getTestingAppender(RestRequestHandler.class);
        var handler = new Handler(environment);
        var output = outputStream();
        var contextWithRequestId = new FakeContext();
        var expectedRequestId = contextWithRequestId.getAwsRequestId();
        handler.handleRequest(requestWithHeadersAndPath(), output, contextWithRequestId);
        assertThat(appender.getMessages(), containsString(expectedRequestId));
    }

    private InputStream requestWithBodyWithoutType() throws JsonProcessingException {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("Some value");
        requestBody.setField2("Some value");
        ObjectNode objectWithoutType = objectMapper.convertValue(requestBody, ObjectNode.class);
        objectWithoutType.remove(RequestBody.TYPE_ATTRIBUTE);

        return new HandlerRequestBuilder<ObjectNode>(objectMapper)
                   .withBody(objectWithoutType)
                   .build();
    }

    private Problem getProblemFromFailureResponse(ByteArrayOutputStream outputStream) throws JsonProcessingException {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(GatewayResponse.class, Problem.class);
        GatewayResponse<Problem> response = objectMapper.readValue(outputStream.toString(), javaType);
        return response.getBodyObject(Problem.class);
    }

    private GatewayResponse<Problem> getApiGatewayResponse(ByteArrayOutputStream outputStream)
        throws JsonProcessingException {
        TypeReference<GatewayResponse<Problem>> tr = new TypeReference<>() {
        };
        return objectMapper.readValue(outputStream.toString(StandardCharsets.UTF_8), tr);
    }

    private Handler handlerThatThrowsUncheckedExceptions() {
        return new Handler(environment) {
            @Override
            protected String processInput(RequestBody input, RequestInfo requestInfo, Context context) {
                throwUncheckedExceptions();
                return null;
            }
        };
    }

    private Handler handlerThatOverridesGetFailureStatusCode() {
        return new Handler(environment) {
            @Override
            public int getFailureStatusCode(RequestBody input, ApiGatewayException exception) {
                return OVERRIDEN_STATUS_CODE;
            }

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

    private InputStream anyRequest() throws JsonProcessingException {
        return requestWithHeaders();
    }

    private InputStream requestWithHeadersAndPath() throws JsonProcessingException {
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders());
        request.put("path", PATH);
        return jsonNodeToInputStream(request);
    }

    private InputStream jsonNodeToInputStream(JsonNode request) throws JsonProcessingException {
        String requestString = objectMapper.writeValueAsString(request);
        return IoUtils.stringToStream(requestString);
    }

    private InputStream requestWithHeaders() throws JsonProcessingException {
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders());
        return jsonNodeToInputStream(request);
    }

    private JsonNode createHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, ContentTypes.APPLICATION_JSON);
        headers.put(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_JSON);
        return objectMapper.convertValue(headers, JsonNode.class);
    }

    private ObjectNode createBody() {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("value1");
        requestBody.setField2("value2");
        return objectMapper.convertValue(requestBody, ObjectNode.class);
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
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
}
