package nva.commons.apigateway;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.REQUEST_ID;
import static nva.commons.apigateway.MediaTypes.APPLICATION_PROBLEM_JSON;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.management.modelmbean.XMLParseException;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.GoneException;
import nva.commons.apigateway.exceptions.TestException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.apigateway.testutils.Base64Handler;
import nva.commons.apigateway.testutils.Handler;
import nva.commons.apigateway.testutils.RawStringResponseHandler;
import nva.commons.apigateway.testutils.RedirectHandler;
import nva.commons.apigateway.testutils.RequestBody;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

class ApiGatewayHandlerTest {

    public static final String TOP_EXCEPTION_MESSAGE = "TOP Exception";
    public static final String MIDDLE_EXCEPTION_MESSAGE = "MIDDLE Exception";
    public static final String BOTTOM_EXCEPTION_MESSAGE = "BOTTOM Exception";
    public static final int OVERRIDDEN_STATUS_CODE = 418;  //I'm a teapot
    public static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
                                                                       "eventWithUnknownRequestInfo.json");
    private static final String PATH = "path1/path2/path3";
    private Context context;
    private Handler handler;

    public static Stream<String> mediaTypeProvider() {
        return Stream.of(
            MediaTypes.APPLICATION_JSON_LD.toString(),
            MediaTypes.APPLICATION_DATACITE_XML.toString(),
            MediaTypes.SCHEMA_ORG.toString()
        );
    }

    @BeforeEach
    public void setup() {
        context = new FakeContext();
        handler = new Handler();
    }

    @Test
    @DisplayName("ApiGatewayHandler has a constructor with input class as only parameter")
    public void apiGatewayHandlerHasAConstructorWithInputClassAsOnlyParameter() {
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

    // TODO: Should return 415 when the Content-type header of request is unsupported (i.e., the one
    //  describing the content of the body of a post request)
    // TODO: Should return 406 when Accept header contains unsupported media type
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

    @ParameterizedTest(name = "handleRequest should return OK when input is {0}")
    @ValueSource(strings = {
        "*/*",
        "application/json",
        "application/json; charset=UTF-8",
        "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8",
        "*; q=.2" // java.net.HttpURLConnection uses this Accept header
    })
    public void handleRequestShouldReturnOkOnSupportedAcceptHeader(String mediaType) throws IOException {
        InputStream input = requestWithAcceptHeader(mediaType);

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void handleRequestReturnsContentTypeJsonOnAcceptWildcard()
        throws IOException {
        Handler handler = handlerThatOverridesListSupportedMediaTypes();

        InputStream input = requestWithAcceptHeader(MediaType.ANY_TYPE.toString());

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(CONTENT_TYPE), is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @ParameterizedTest(name = "Should return supported type {0} when it is requested")
    @MethodSource("mediaTypeProvider")
    void shouldReturnContentTypeMatchingSupportedMediaTypeWhenSupportedMediaTypeIsRequested(String mediaType)
        throws IOException {
        Handler handler = handlerThatOverridesListSupportedMediaTypes();

        InputStream input = requestWithAcceptHeader(mediaType);

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(CONTENT_TYPE), is(equalTo(mediaType)));
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
    public void shouldReturnProblemContainingCustomResource() throws IOException {
        var customProblemObject = new CustomObject(randomString(), randomString());
        var handler = handlerThatThrowsGoneExceptionsWithCustomObject(customProblemObject);
        var outputStream = outputStream();
        handler.handleRequest(requestWithHeaders(), outputStream, context);
        var problem = getProblemFromFailureResponse(outputStream);

        assertThat(problem.getParameters().get("resource"), is(equalTo(customProblemObject.toJsonString())));
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

        assertThat(problem.getParameters().get(REQUEST_ID), is(equalTo(context.getAwsRequestId())));
        assertThat(problem.getStatus(), is(Status.NOT_FOUND));
    }

    @Test
    @DisplayName("Failure message contains application/problem+json ContentType when an Exception is thrown")
    public void failureMessageContainsApplicationProblemJsonContentTypeWhenExceptionIsThrown()
        throws IOException {
        Handler handler = handlerThatThrowsExceptions();

        GatewayResponse<Problem> responseParsing = getProblemResponse(requestWithHeadersAndPath(), handler);

        assertThat(responseParsing.getHeaders().get(CONTENT_TYPE), is(equalTo(APPLICATION_PROBLEM_JSON.toString())));
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
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var output = outputStream();
        var expectedRequestId = context.getAwsRequestId();
        handler.handleRequest(requestWithHeadersAndPath(), output, context);
        assertThat(appender.getMessages(), containsString(expectedRequestId));
    }

    @Test
    public void itReturnsResponseWithPopulatedFieldsAndBothEmptyAndNonEmptyLists() throws IOException {
        var output = outputStream();
        InputStream input = requestWithBodyWithEmptyFields();
        handler.handleRequest(input, output, context);
        GatewayResponse<JsonNode> response = GatewayResponse.fromOutputStream(output, JsonNode.class);
        JsonNode jsonNode = response.getBodyObject(JsonNode.class);
        String nullField = RequestBody.FIELD2;
        String presentField = RequestBody.FIELD1;
        assertThat(jsonNode.has(presentField), is(true));
        assertThat(jsonNode.has(nullField), is(false));
        assertThat(jsonNode.has(RequestBody.EMPTY_LIST), is(true));
        assertThat(jsonNode.get(RequestBody.EMPTY_LIST), is(instanceOf(ArrayNode.class)));
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
        ObjectMapper spiedMapper = spy(defaultRestObjectMapper);
        var handler = new Handler(spiedMapper);
        var inputStream = requestWithHeaders();
        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        verify(spiedMapper, atLeast(1)).writeValueAsString(any());
    }

    @Test
    void handlerSerializesWithIsBase64Encoded() throws IOException {
        var output = outputStream();
        InputStream input = requestWithBodyWithEmptyFields();
        var isBase64EncodedHandler = new Base64Handler();
        isBase64EncodedHandler.handleRequest(input, output, context);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getIsBase64Encoded(), is(true));
    }

    @Test
    void handlerSendsRedirectionWhenItReceivesARedirectException() throws IOException {
        var expectedRedirectLocation = randomUri();
        var expectedRedirectStatusCode = HttpURLConnection.HTTP_SEE_OTHER;
        var handler = new RedirectHandler(expectedRedirectLocation, expectedRedirectStatusCode);
        var request = requestWithHeaders(Map.of(HttpHeaders.ACCEPT, MediaType.HTML_UTF_8.toString()));
        var response = getResponse(Void.class, request, handler);
        assertThat(response.getStatusCode(), is(equalTo(expectedRedirectStatusCode)));
        assertThat(response.getHeaders(), hasEntry(HttpHeaders.LOCATION, expectedRedirectLocation.toString()));
    }

    @Test
    void shouldReturnJsonObjectWhenClientAsksForJson() throws Exception {
        var handler = new RawStringResponseHandler(dtoObjectMapper);
        var inputStream = requestWithHeaders();
        var expected = objectMapper.convertValue(createBody(), RequestBody.class);
        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var actual = dtoObjectMapper.readValue(response.getBody(), RequestBody.class);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldReturnXmlObjectWhenClientAsksForXml() throws Exception {
        var handler = new RawStringResponseHandler(dtoObjectMapper);
        var inputStream = requestWithAcceptXmlHeader();
        var expected = objectMapper.convertValue(createBody(), RequestBody.class);
        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var xmlMapper = new XmlMapper();
        var actual = xmlMapper.readValue(response.getBody(), RequestBody.class);

        assertThat(actual, is(equalTo(expected)));
    }

    private String getUnsupportedMediaTypeErrorMessage(String mediaType) {
        return UnsupportedAcceptHeaderException.createMessage(
            List.of(MediaType.parse(mediaType)),
            handler.listSupportedMediaTypes());
    }

    private <T> GatewayResponse<T> getResponse(Class<T> type, InputStream input, Handler handler) throws IOException {
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(input, outputStream, context);
        return GatewayResponse.fromOutputStream(outputStream, type);
    }

    private GatewayResponse<String> getStringResponse(InputStream input, Handler handler) throws IOException {
        return getResponse(String.class, input, handler);
    }

    private GatewayResponse<Problem> getProblemResponse(InputStream input, Handler handler) throws IOException {
        return getResponse(Problem.class, input, handler);
    }

    private Handler handlerFailingWhenParsing(String expectedMessage) {
        return new Handler() {
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

        return new HandlerRequestBuilder<RequestBody>(defaultRestObjectMapper)
                   .withBody(requestBody)
                   .build();
    }

    private Problem getProblemFromFailureResponse(ByteArrayOutputStream outputStream) throws JsonProcessingException {
        JavaType javaType = defaultRestObjectMapper.getTypeFactory()
                                .constructParametricType(GatewayResponse.class, Problem.class);
        GatewayResponse<Problem> response = defaultRestObjectMapper.readValue(outputStream.toString(), javaType);
        return response.getBodyObject(Problem.class);
    }

    private Handler handlerThatThrowsUncheckedExceptions() {
        return new Handler() {
            @Override
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context) {
                throwUncheckedExceptions();
                return null;
            }
        };
    }

    private Handler handlerThatThrowsGoneExceptionsWithCustomObject(CustomObject customObject) {
        return new Handler() {
            @Override
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
                throws GoneException {
                throwGoneExceptionWithInstance();
                return null;
            }

            private void throwGoneExceptionWithInstance() throws GoneException {
                throw new GoneException("some message", customObject);
            }
        };
    }

    private Handler handlerThatOverridesListSupportedMediaTypes() {
        return new Handler() {
            @Override
            public List<MediaType> listSupportedMediaTypes() {
                return List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD,
                               MediaTypes.APPLICATION_DATACITE_XML, MediaTypes.SCHEMA_ORG);
            }
        };
    }

    private Handler handlerThatOverridesGetFailureStatusCode() {
        return new Handler() {
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
        ObjectNode request = defaultRestObjectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders());
        request.put("path", PATH);
        return jsonNodeToInputStream(request);
    }

    private InputStream jsonNodeToInputStream(JsonNode request) throws JsonProcessingException {
        String requestString = defaultRestObjectMapper.writeValueAsString(request);
        return IoUtils.stringToStream(requestString);
    }

    private InputStream requestWithAcceptHeader(String acceptHeader) throws JsonProcessingException {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, acceptHeader);
        return requestWithHeaders(headers);
    }

    private InputStream requestWithHeaders(Map<String, String> headers) throws JsonProcessingException {
        ObjectNode request = defaultRestObjectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders(headers));
        return jsonNodeToInputStream(request);
    }

    private InputStream requestWithHeaders() throws JsonProcessingException {
        ObjectNode request = defaultRestObjectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders());
        return jsonNodeToInputStream(request);
    }

    private InputStream requestWithAcceptXmlHeader() throws JsonProcessingException {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, MediaType.XML_UTF_8.toString());
        headers.put(CONTENT_TYPE, MediaType.JSON_UTF_8.toString());

        ObjectNode request = defaultRestObjectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeaders(headers));
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

    private Handler handlerThatThrowsExceptions() {
        return new Handler() {
            @Override
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
                throws ApiGatewayException {
                throwExceptions();
                return null;
            }
        };
    }

    private record CustomObject(String firstValue, String secondValue) implements JsonSerializable {
        public String toString() {
        return this.toJsonString();
        }
    }
}
