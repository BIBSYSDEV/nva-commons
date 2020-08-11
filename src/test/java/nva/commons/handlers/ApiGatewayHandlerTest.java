package nva.commons.handlers;

import static nva.commons.handlers.RestRequestHandler.APPLICATION_PROBLEM_JSON;
import static nva.commons.handlers.RestRequestHandler.CONTENT_TYPE;
import static nva.commons.handlers.RestRequestHandler.REQUEST_ID;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.modelmbean.XMLParseException;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.Handler;
import nva.commons.RequestBody;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.InvalidOrMissingTypeException;
import nva.commons.exceptions.TestException;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        Logger logger = LoggerFactory.getLogger(RestRequestHandler.class);
        RestRequestHandler<String, String> handler = new ApiGatewayHandler<>(String.class, logger) {
            @Override
            protected String processInput(String input, RequestInfo requestInfo, Context context) {
                return null;
            }

            @Override
            protected Integer getSuccessStatusCode(String input, String output) {
                return HttpStatus.SC_OK;
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
            assertThat(headers.get(expectedHeader), is(equalTo(expectedHeaders.get(expectedHeader).textValue()))));
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
        TestAppender appender = LogUtils.getTestingAppender(Handler.class);
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
        TestAppender appender = LogUtils.getTestingAppender(Handler.class);
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
            RestRequestHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
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
    public void handlerReturnsInternalServerErrorWhenLoggerHasNotBeenExplicitlySet() throws IOException {
        Handler handler = handlerThatHasNotSetLogger();
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(anyRequest(), outputStream, context);

        GatewayResponse<Problem> response = getApiGatewayResponse(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
        assertThat(response.getBodyObject(Problem.class).getDetail(), containsString(handler.getClass().getName()));
    }

    @Test
    public void handlerReturnsBadRequestForJsonInvalidTypeError() throws IOException {
        Handler handler = new Handler(environment);

        InputStream inputStream = requestWithBodyWithoutType();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, outputStream, context);

        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(outputStream);
        assertThat(response.getStatusCode(), is(equalTo(HttpStatus.SC_BAD_REQUEST)));

        Problem details = response.getBodyObject(Problem.class);
        assertThat(details.getDetail(), containsString(InvalidOrMissingTypeException.MESSAGE));
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

    private Handler handlerThatHasNotSetLogger() {
        return new Handler(environment) {

            @Override
            public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
                throws IOException {
                logger = null;
                super.handleRequest(inputStream, outputStream, context);
            }
        };
    }

    private GatewayResponse<Problem> getApiGatewayResponse(ByteArrayOutputStream outputStream)
        throws JsonProcessingException {
        TypeReference<GatewayResponse<Problem>> tr = new TypeReference<>() {};
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
        headers.put(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
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
