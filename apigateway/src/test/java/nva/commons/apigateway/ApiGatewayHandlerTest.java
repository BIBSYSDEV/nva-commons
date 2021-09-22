package nva.commons.apigateway;

import static nva.commons.apigateway.ApiGatewayHandler.APPLICATION_PROBLEM_JSON;
import static nva.commons.apigateway.ApiGatewayHandler.CONTENT_TYPE;
import static nva.commons.apigateway.ApiGatewayHandler.REQUEST_ID;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.in;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.modelmbean.XMLParseException;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.TestException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

public class ApiGatewayHandlerTest {

    public static final String SOME_ENV_VALUE = "SomeEnvValue";
    public static final String TOP_EXCEPTION_MESSAGE = "TOP Exception";
    public static final String MIDDLE_EXCEPTION_MESSAGE = "MIDDLE Exception";
    public static final String BOTTOM_EXCEPTION_MESSAGE = "BOTTOM Exception";
    public static final String SOME_REQUEST_ID = "RequestID:123456";
    public static final int OVERRIDDEN_STATUS_CODE = 418;  //I'm a teapot
    public static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
                                                                       "eventWithUnknownRequestInfo.json");
    private static final String PATH = "path1/path2/path3";
    public Environment environment;
    private Context context;
    private Handler handler;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn(SOME_ENV_VALUE);
        context = mock(Context.class);
        when(context.getAwsRequestId()).thenReturn(SOME_REQUEST_ID);
        handler = new Handler(environment);
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
        InputStream input = requestWithHeaders();

        getStringResponse(input, handler);

        Map<String, String> headers = handler.getHeaders();
        JsonNode expectedHeaders = createHeaders();
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader ->
                                                          assertThat(headers.get(expectedHeader), is(equalTo(
                                                              expectedHeaders.get(expectedHeader).textValue()))));
    }

    @ParameterizedTest(name = "handleRequest should return Unsupported media-type when input is {0}")
    @ValueSource(strings = {
            "application/xml",
            "text/plain; charset=UTF-8"
    })
    public void handleRequestShouldReturnUnsupportedMediaTypeOnUnsupportedAcceptHeader(String mediaType)
            throws IOException {
        InputStream input = requestWithAcceptHeader(mediaType);

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)));
        String expectedMessage = getUnsupportedMediaTypeErrorMessage(mediaType);
        assertThat(response.getBody(), containsString(expectedMessage));
    }

    private String getUnsupportedMediaTypeErrorMessage(String mediaType) {
        return UnsupportedAcceptHeaderException.createMessage(
                List.of(MediaType.parse(mediaType)),
                handler.listSupportedMediaTypes());
    }

    @ParameterizedTest(name = "handleRequest should return OK when input is {0}")
    @ValueSource(strings = {
            "*/*",
            "application/json",
            "application/json; charset=UTF-8",
            "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8"
    })
    public void handleRequestShouldReturnOkOnSupportedAcceptHeader(String mediaType) throws IOException {
        InputStream input = requestWithAcceptHeader(mediaType);

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void handleRequestWildcard()
            throws IOException {
        Handler handler = handlerThatOverridesListSupportedMediaTypes();

        InputStream input = requestWithAcceptHeader(MediaType.ANY_TYPE.toString());

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    public void handleRequestJsonLd()
            throws IOException {
        Handler handler = handlerThatOverridesListSupportedMediaTypes();

        InputStream input = requestWithAcceptHeader(MediaTypes.APPLICATION_JSON_LD.toString());

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    @DisplayName("handleRequest should have available the request path")
    public void handleRequestShouldHaveAvailableTheRequestPath() throws IOException {
        InputStream input = requestWithHeadersAndPath();

        getStringResponse(input, handler);

        String actualPath = handler.getPath();
        JsonNode expectedHeaders = createHeaders();
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader -> assertThat(actualPath, is(equalTo(PATH))));
    }

    @Test
    @DisplayName("handleRequest allows response headers depended on input")
    public void apiGatewayHandlerAllowsResponseHeadersDependedOnInput() throws IOException {
        InputStream input = requestWithHeadersAndPath();
        GatewayResponse<String> response = getStringResponse(input, handler);
        assertTrue(response.getHeaders().containsKey(HttpHeaders.WARNING));
    }

    @Test
    public void handleRequestDoesNotThrowExceptionOnUnknownFieldsInRequestInfo() throws IOException {
        InputStream input = IoUtils.inputStreamFromResources(EVENT_WITH_UNKNOWN_REQUEST_INFO);

        GatewayResponse<RequestBody> response = getResponse(RequestBody.class, input, handler);
        RequestBody body = response.getBodyObject(RequestBody.class);

        String expectedValueForField1 = "value1";
        String expectedValueForField2 = "value2";
        assertThat(body.getField1(), containsString(expectedValueForField1));
        assertThat(body.getField2(), containsString(expectedValueForField2));
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
    public void handlerDoesNotRevealInformationForRuntimeExceptions() throws IOException {
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

        GatewayResponse<Problem> responseParsing = getProblemResponse(requestWithHeadersAndPath(), handler);

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

        GatewayResponse<Problem> responseParsing = getProblemResponse(requestWithHeadersAndPath(), handler);

        assertThat(responseParsing.getHeaders().get(CONTENT_TYPE), is(equalTo(APPLICATION_PROBLEM_JSON)));
    }

    @Test
    @DisplayName("getFailureStatusCode sets the status code to the ApiGatewayResponse")
    public void getFailureStatusCodeSetsTheStatusCodeToTheApiGatewayResponse() throws IOException {
        Handler handler = handlerThatOverridesGetFailureStatusCode();

        GatewayResponse<Problem> response = getProblemResponse(anyRequest(), handler);

        assertThat(response.getStatusCode(), is(equalTo(OVERRIDDEN_STATUS_CODE)));
        assertThat(response.getBodyObject(Problem.class).getStatus().getStatusCode(),
                   is(equalTo(OVERRIDDEN_STATUS_CODE)));
    }

    @Test
    @DisplayName("getFailureStatusCode returns by default the status code of the ApiGatewayException")
    public void getFailureStatusCodeReturnsByDefaultTheStatusCodeOfTheApiGatewayException() throws IOException {
        Handler handler = handlerThatThrowsExceptions();

        GatewayResponse<Problem> response = getProblemResponse(anyRequest(), handler);

        assertThat(response.getStatusCode(), is(equalTo(TestException.ERROR_STATUS_CODE)));
        assertThat(response.getBodyObject(Problem.class).getStatus().getStatusCode(),
                   is(equalTo(TestException.ERROR_STATUS_CODE)));
    }

    @Test
    public void handlerLogsRequestIdForEveryRequest() throws IOException {
        var appender = LogUtils.getTestingAppender(RestRequestHandler.class);
        var output = outputStream();
        var contextWithRequestId = new FakeContext();
        var expectedRequestId = contextWithRequestId.getAwsRequestId();
        handler.handleRequest(requestWithHeadersAndPath(), output, contextWithRequestId);
        assertThat(appender.getMessages(), containsString(expectedRequestId));
    }

    @Test
    public void handlerReturnsResponseThatIncludesAllEmptyFields() throws IOException {
        var output = outputStream();
        InputStream input = requestWithBodyWithEmptyFields();
        handler.handleRequest(input, output, context);
        GatewayResponse<JsonNode> response = GatewayResponse.fromOutputStream(output);
        JsonNode jsonNode = response.getBodyObject(JsonNode.class);

        assertThat(jsonNode.has(RequestBody.FIELD1), is(true));
        assertThat(jsonNode.has(RequestBody.FIELD2), is(true));
        assertThat(jsonNode.get(RequestBody.FIELD2), is(equalTo(objectMapper.nullNode())));
    }

    @Test
    public void handlerReturnsBadRequestWhenInputParsingFails() throws IOException {
        String expectedMessage = "Expected error message when parsing fails";
        Handler handler = handlerFailingWhenParsing(expectedMessage);
        InputStream input = requestWithHeadersAndPath();
        GatewayResponse<Problem> response = getProblemResponse(input, handler);
        Problem problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(expectedMessage));
    }

    @Test
    void handlerSerializesBodyWithNonDefaultSerializationWhenDefaultSerializerIsOverridden() throws IOException {
        ObjectMapper spiedMapper = spy(JsonUtils.objectMapper);
        var handler = new Handler(environment, spiedMapper);
        var inputStream = requestWithHeaders();
        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        verify(spiedMapper, atLeast(1)).writeValueAsString(any());
    }

    private <T> GatewayResponse<T> getResponse(Class<T> type, InputStream input, Handler handler) throws IOException {
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream);
    }


    private GatewayResponse<String> getStringResponse(InputStream input, Handler handler) throws IOException {
        return getResponse(String.class, input, handler);
    }

    private GatewayResponse<Problem> getProblemResponse(InputStream input, Handler handler) throws IOException {
        return getResponse(Problem.class, input, handler);

    }

    private Handler handlerFailingWhenParsing(String expectedMessage) {
        return new Handler(environment) {
            @Override
            protected RequestBody parseInput(String inputString) {
                throw new RuntimeException(expectedMessage);
            }
        };
    }

    private InputStream requestWithBodyWithEmptyFields() throws JsonProcessingException {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("Some value");
        requestBody.setField2(null);

        return new HandlerRequestBuilder<RequestBody>(objectMapper)
            .withBody(requestBody)
            .build();
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
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context) {
                throwUncheckedExceptions();
                return null;
            }
        };
    }

    private Handler handlerThatOverridesListSupportedMediaTypes() {
        return new Handler(environment) {
            @Override
            public List<MediaType> listSupportedMediaTypes() {
                return List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD);
            }
        };
    }

    private Handler handlerThatOverridesGetFailureStatusCode() {
        return new Handler(environment) {
            @Override
            public int getFailureStatusCode(RequestBody input, ApiGatewayException exception) {
                return OVERRIDDEN_STATUS_CODE;
            }

            @Override
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
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

    private InputStream requestWithAcceptHeader(String acceptHeader) throws JsonProcessingException {
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, acceptHeader);
        return requestWithHeaders(headers);
    }

    private InputStream requestWithHeaders(Map<String,String> headers) throws JsonProcessingException {
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders(headers));
        return jsonNodeToInputStream(request);
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
        headers.put(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
        return createHeaders(headers);
    }

    private JsonNode createHeaders(Map<String, String> headers) {
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
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
                throws ApiGatewayException {
                throwExceptions();
                return null;
            }
        };
    }
}
