package nva.commons.apigateway;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandlerV2.INTERNAL_ERROR_MESSAGE;
import static nva.commons.apigateway.ApiGatewayHandlerV2.REQUEST_ID;
import static nva.commons.apigateway.MediaTypes.APPLICATION_PROBLEM_JSON;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.modelmbean.XMLParseException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.TestException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.apigateway.testutils.HandlerV2;
import nva.commons.apigateway.testutils.RedirectHandlerV2;
import nva.commons.apigateway.testutils.RequestBody;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

public class ApiGatewayHandlerV2Test {

    public static final String TOP_EXCEPTION_MESSAGE = "TOP Exception";
    public static final String MIDDLE_EXCEPTION_MESSAGE = "MIDDLE Exception";
    public static final String BOTTOM_EXCEPTION_MESSAGE = "BOTTOM Exception";
    public static final String SOME_REQUEST_ID = "RequestID:123456";
    public static final int OVERRIDDEN_STATUS_CODE = 418;  //I'm a teapot
    public static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
                                                                       "eventWithUnknownRequestInfo.json");
    private static final String PATH = "path1/path2/path3";
    private Context context;
    private HandlerV2 handler;

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        context = mock(Context.class);
        when(context.getAwsRequestId()).thenReturn(SOME_REQUEST_ID);
        handler = new HandlerV2();
    }

    @Test
    void handleRequestReturnsContentTypeJsonOnAcceptWildcard()
        throws IOException {
        var handler = handlerThatOverridesListSupportedMediaTypes();
        var input = requestWithAcceptHeader(MediaType.ANY_TYPE.toString());
        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(CONTENT_TYPE), is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    @DisplayName("ApiGatewayHandler has a constructor with input class as only parameter")
    void apiGatewayHandlerHasAConstructorWithInputClassAsOnlyParameter() {
        ApiGatewayHandlerV2<String, String> handler = new ApiGatewayHandlerV2<>(String.class) {
            @Override
            protected Integer getSuccessStatusCode(String input, String output) {
                return HttpURLConnection.HTTP_OK;
            }

            @Override
            protected String processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context) {
                return null;
            }
        };
    }

    @Test
    @DisplayName("handleRequest should have available the request headers")
    void handleRequestShouldHaveAvailableTheRequestHeaders() throws IOException {
        var input = requestWithHeaders();
        handler.handleRequest(input, context);
        Map<String, String> headers = handler.getHeaders();
        var expectedHeaders = input.getHeaders();
        expectedHeaders.keySet()
            .forEach(expectedHeader ->
                         assertThat(headers.get(expectedHeader), is(equalTo(expectedHeaders.get(expectedHeader)))));
    }

    @ParameterizedTest(name = "handleRequest should return Unsupported media-type when input is {0}")
    @ValueSource(strings = {
        "application/xml",
        "text/plain; charset=UTF-8"
    })
    void handleRequestShouldReturnUnsupportedMediaTypeOnUnsupportedAcceptHeader(String mediaType)
        throws IOException {
        var input = requestWithAcceptHeader(mediaType);
        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)));
        var expectedMessage = getUnsupportedMediaTypeErrorMessage(mediaType);
        assertThat(response.getBody(), containsString(expectedMessage));
    }

    @ParameterizedTest(name = "handleRequest should return OK when input is {0}")
    @ValueSource(strings = {
        "*/*",
        "application/json",
        "application/json; charset=UTF-8",
        "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8"
    })
    void handleRequestShouldReturnOkOnSupportedAcceptHeader(String mediaType) throws IOException {
        var input = requestWithAcceptHeader(mediaType);

        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void handleRequestReturnsContentTypeJsonLdOnAcceptJsonLd()
        throws IOException {
        HandlerV2 handler = handlerThatOverridesListSupportedMediaTypes();

        var input = requestWithAcceptHeader(MediaTypes.APPLICATION_JSON_LD.toString());

        var response = handler.handleRequest(input, context);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(CONTENT_TYPE), is(equalTo(MediaTypes.APPLICATION_JSON_LD.toString())));
    }

    @Test
    @DisplayName("handleRequest should have available the request path")
    void handleRequestShouldHaveAvailableTheRequestPath() {
        var input = requestWithHeadersAndPath();

        var response = handler.handleRequest(input, context);

        String actualPath = handler.getPath();
        var expectedHeaders = createHeaders();
        expectedHeaders.keySet().forEach(expectedHeader -> assertThat(actualPath, is(equalTo(PATH))));
    }

    @Test
    @DisplayName("handleRequest allows response headers depended on input")
    void apiGatewayHandlerAllowsResponseHeadersDependedOnInput() {
        var input = requestWithHeadersAndPath();
        var response = handler.handleRequest(input, context);
        assertThat(response.getHeaders(), hasKey(HttpHeaders.WARNING));
    }

    @Test
    @DisplayName("Logger logs the whole exception stacktrace when an exception has occurred 2")
    void loggerLogsTheWholeExceptionStackTraceWhenAnExceptionHasOccurred() {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        HandlerV2 handler = handlerThatThrowsExceptions();
        handler.handleRequest(requestWithHeadersAndPath(), context);

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
    void loggerLogsTheWholeExceptionStackTraceWhenAnUncheckedExceptionHasOccurred() {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        HandlerV2 handler = handlerThatThrowsUncheckedExceptions();

        handler.handleRequest(requestWithHeadersAndPath(), context);

        String logs = appender.getMessages();

        assertThat(logs, containsString(IllegalStateException.class.getName()));
        assertThat(logs, containsString(IllegalArgumentException.class.getName()));
        assertThat(logs, containsString(IllegalCallerException.class.getName()));
        assertThat(logs, containsString(BOTTOM_EXCEPTION_MESSAGE));
    }

    @Test
    @DisplayName("Handler does not reveal information for runtime exceptions")
    void handlerDoesNotRevealInformationForRuntimeExceptions() {
        HandlerV2 handler = handlerThatThrowsUncheckedExceptions();

        var response = handler.handleRequest(requestWithHeaders(), context);
        Problem problem = extractProblemFromFailureResponse(response);
        String details = problem.getDetail();
        assertThat(details, not(containsString(BOTTOM_EXCEPTION_MESSAGE)));
        assertThat(details, not(containsString(MIDDLE_EXCEPTION_MESSAGE)));
        assertThat(details, not(containsString(TOP_EXCEPTION_MESSAGE)));
        assertThat(details, containsString(INTERNAL_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("Failure message contains exception message and status code when an Exception is thrown")
    void failureMessageContainsExceptionMessageAndStatusCodeWhenAnExceptionIsThrown()
        throws IOException {
        var handler = handlerThatThrowsExceptions();

        var response = handler.handleRequest(requestWithHeadersAndPath(), context);

        Problem problem = extractProblemFromFailureResponse(response);
        assertThat(problem.getDetail(), containsString(TOP_EXCEPTION_MESSAGE));
        assertThat(problem.getTitle(), containsString(Status.NOT_FOUND.getReasonPhrase()));

        assertThat(problem.getParameters().get(REQUEST_ID), is(equalTo(SOME_REQUEST_ID)));
        assertThat(problem.getStatus(), is(Status.NOT_FOUND));
    }

    @Test
    @DisplayName("Failure message contains application/problem+json ContentType when an Exception is thrown")
    void failureMessageContainsApplicationProblemJsonContentTypeWhenExceptionIsThrown()
        throws IOException {
        var handler = handlerThatThrowsExceptions();
        var response = handler.handleRequest(requestWithHeadersAndPath(), context);
        assertThat(response.getHeaders().get(CONTENT_TYPE), is(equalTo(APPLICATION_PROBLEM_JSON.toString())));
    }

    @Test
    @DisplayName("getFailureStatusCode returns by default the status code of the ApiGatewayException")
    void getFailureStatusCodeReturnsTheStatusCodeOfTheApiGatewayException() {
        HandlerV2 handler = handlerThatThrowsExceptions();

        var response = handler.handleRequest(requestWithHeaders(), context);
        var problem = extractProblemFromFailureResponse(response);
        assertThat(response.getStatusCode(), is(equalTo(TestException.ERROR_STATUS_CODE)));
        assertThat(problem.getStatus().getStatusCode(), is(equalTo(TestException.ERROR_STATUS_CODE)));
    }

    @Test
    void handlerLogsRequestIdForEveryRequest() {
        var appender = LogUtils.getTestingAppender(ApiGatewayHandlerV2.class);
        var contextWithRequestId = new FakeContext();
        var expectedRequestId = contextWithRequestId.getAwsRequestId();
        handler.handleRequest(requestWithHeadersAndPath(), contextWithRequestId);
        assertThat(appender.getMessages(), containsString(expectedRequestId));
    }

    @Test
    void itReturnsResponseWithPopulatedFieldsAndBothEmptyAndNonEmptyLists() throws IOException {

        var input = requestWithBodyWithEmptyFields();
        var response = handler.handleRequest(input, context);
        JsonNode responseObject = JsonUtils.dtoObjectMapper.readTree(response.getBody());
        String nullField = RequestBody.FIELD2;
        String presentField = RequestBody.FIELD1;
        assertThat(responseObject.has(presentField), is(true));
        assertThat(responseObject.has(nullField), is(false));
        assertThat(responseObject.has(RequestBody.EMPTY_LIST), is(true));
        assertThat(responseObject.get(RequestBody.EMPTY_LIST), is(instanceOf(ArrayNode.class)));
    }

    @Test
    void handlerReturnsBadRequestWhenInputParsingFails() {
        String expectedMessage = "Expected error message when parsing fails";
        HandlerV2 handler = handlerFailingWhenParsing(expectedMessage);
        var input = requestWithHeadersAndPath();
        var response = handler.handleRequest(input, context);
        Problem problem = extractProblemFromFailureResponse(response);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(expectedMessage));
    }

    @Test
    void handlerSendsRedirectionWhenItReceivesARedirectException() {
        var expectedRedirectLocation = randomUri();
        var expectedRedirectStatusCode = HttpURLConnection.HTTP_SEE_OTHER;
        var handler = new RedirectHandlerV2(expectedRedirectLocation, expectedRedirectStatusCode);
        var request = requestWithHeaders(Map.of(HttpHeaders.ACCEPT, MediaType.HTML_UTF_8.toString()));
        var response = handler.handleRequest(request, context);
        assertThat(response.getStatusCode(), is(equalTo(expectedRedirectStatusCode)));
        assertThat(response.getHeaders(), hasEntry(HttpHeaders.LOCATION, expectedRedirectLocation.toString()));
    }

    private String getUnsupportedMediaTypeErrorMessage(String mediaType) {
        return UnsupportedAcceptHeaderException.createMessage(
            List.of(MediaType.parse(mediaType)),
            handler.listSupportedMediaTypes());
    }

    private HandlerV2 handlerFailingWhenParsing(String expectedMessage) {
        return new HandlerV2() {
            @Override
            protected RequestBody parseInput(String inputString) {
                throw new RuntimeException(expectedMessage);
            }
        };
    }

    private APIGatewayProxyRequestEvent requestWithBodyWithEmptyFields() {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("Some value");
        requestBody.setField2(null);

        String json = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(requestBody)).orElseThrow();
        return new APIGatewayProxyRequestEvent()
            .withBody(json);
    }

    private Problem extractProblemFromFailureResponse(APIGatewayProxyResponseEvent responseEvent) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(responseEvent.getBody(), Problem.class)).orElseThrow();
    }

    private HandlerV2 handlerThatThrowsUncheckedExceptions() {
        return new HandlerV2() {
            @Override
            protected RequestBody processInput(RequestBody input, APIGatewayProxyRequestEvent requestInfo,
                                               Context context) {
                throwUncheckedExceptions();
                return null;
            }
        };
    }

    //
    private HandlerV2 handlerThatOverridesListSupportedMediaTypes() {
        return new HandlerV2() {
            @Override
            public List<MediaType> listSupportedMediaTypes() {
                return List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD);
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

    //
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

    private APIGatewayProxyRequestEvent requestWithHeadersAndPath() {
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
        request.setBody(createBody());
        request.setHeaders(createHeaders());
        request.setPath(PATH);
        return request;
    }

    private APIGatewayProxyRequestEvent requestWithAcceptHeader(String acceptHeader) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, acceptHeader);
        return requestWithHeaders(headers);
    }

    private APIGatewayProxyRequestEvent requestWithHeaders(Map<String, String> headers) {
        return new APIGatewayProxyRequestEvent()
            .withBody(randomRequestBody())
            .withHeaders(headers)
            .withPathParameters(Collections.emptyMap());
    }

    private APIGatewayProxyRequestEvent requestWithHeaders() {
        Map<String, String> randomHeaders = Map.of(randomString(), randomString(), randomString(), randomString());
        return new APIGatewayProxyRequestEvent().withBody(createBody())
            .withHeaders(randomHeaders);
    }

    private String randomRequestBody() {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1(randomString());
        requestBody.setField2(randomString());
        return requestBody.toString();
    }

    private Map<String, String> createHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
        headers.put(CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
        return headers;
    }

    private String createBody() {
        RequestBody requestBody = new RequestBody();
        requestBody.setField1("value1");
        requestBody.setField2("value2");
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(requestBody)).orElseThrow();
    }

    private HandlerV2 handlerThatThrowsExceptions() {
        return new HandlerV2() {
            @Override
            protected RequestBody processInput(RequestBody input, APIGatewayProxyRequestEvent requestInfo,
                                               Context context)
                throws ApiGatewayException {
                throwExceptions();
                return null;
            }
        };
    }
}
