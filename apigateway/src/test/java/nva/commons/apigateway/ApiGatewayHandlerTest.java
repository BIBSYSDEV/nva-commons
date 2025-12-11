package nva.commons.apigateway;

import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.STRICT_TRANSPORT_SECURITY;
import static com.google.common.net.HttpHeaders.VARY;
import static com.google.common.net.HttpHeaders.X_CONTENT_TYPE_OPTIONS;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static no.unit.nva.testutils.TestHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.testutils.TestHeaders.WILDCARD;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.apigateway.ApiGatewayHandler.ALL_ORIGINS_ALLOWED;
import static nva.commons.apigateway.ApiGatewayHandler.CONFLICTING_KEYS;
import static nva.commons.apigateway.ApiGatewayHandler.FALLBACK_ORIGIN;
import static nva.commons.apigateway.ApiGatewayHandler.REQUEST_ID;
import static nva.commons.apigateway.MediaTypes.APPLICATION_PROBLEM_JSON;
import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.GoneException;
import nva.commons.apigateway.exceptions.TestException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.apigateway.testutils.Base64Handler;
import nva.commons.apigateway.testutils.Handler;
import nva.commons.apigateway.testutils.RawStringResponseHandler;
import nva.commons.apigateway.testutils.RedirectHandler;
import nva.commons.apigateway.testutils.RequestBody;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

class ApiGatewayHandlerTest {

    private static final String TOP_EXCEPTION_MESSAGE = "TOP Exception";
    private static final String MIDDLE_EXCEPTION_MESSAGE = "MIDDLE Exception";
    private static final String BOTTOM_EXCEPTION_MESSAGE = "BOTTOM Exception";
    private static final int OVERRIDDEN_STATUS_CODE = 418;  //I'm a teapot
    private static final Path EVENT_WITH_UNKNOWN_REQUEST_INFO = Path.of("apiGatewayMessages",
                                                                       "eventWithUnknownRequestInfo.json");
    private static final Path EVENT_WITH_ACCESS_RIGHTS_CLAIMS = Path.of("apiGatewayMessages",
                                                                       "event_with_access_rights_claim.json");
    private static final Path EVENT_WITH_MANIPULATED_TOKEN = Path.of("apiGatewayMessages",
                                                                    "event_with_manipulated_token.json");
    private static final Path EVENT_WITH_NO_AUTHORIZER = Path.of("apiGatewayMessages",
                                                                "event_without_authorizer_but_auth_header.json");
    private static final Path EVENT_WITH_WRONG_ISSUER = Path.of("apiGatewayMessages",
                                                               "event_without_authorizer_wrong_issuer.json");
    private static final Path EVENT_SIGNED_UNSUPPORTED = Path.of("apiGatewayMessages",
                                                               "event_signed_unsupported.json");
    private static final Path EVENT_UNSIGNED = Path.of("apiGatewayMessages",
                                                       "event_signed_none.json");
    private static final Path EVENT_EXTERNAL_CLIENT = Path.of("apiGatewayMessages",
                                                         "event_external_client.json");
    private static final Path TEST_JWKS = Path.of("apiGatewayMessages",
                                                 "test-jwks.json");
    private static final String PATH = "path1/path2/path3";
    private static final int PORT_NUMBER = 3000;
    private Context context;
    private Handler handler;
    private Environment environment;
    private WireMockServer wireMockServer;

    public static Stream<String> mediaTypeProvider() {
        return Stream.of(MediaTypes.APPLICATION_JSON_LD.toString(), MediaTypes.APPLICATION_DATACITE_XML.toString(),
                         MediaTypes.SCHEMA_ORG.toString());
    }

    @BeforeEach
    void setup() {
        context = new FakeContext();
        environment = mock(Environment.class);
        when(environment.readEnv("ALLOWED_ORIGIN")).thenReturn("*");
        when(environment.readEnv("COGNITO_AUTHORIZER_URLS")).thenReturn("http://localhost:3000");
        handler = new Handler(environment);
        setupWireMockServer();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    private void setupWireMockServer() {
        wireMockServer =
            new WireMockServer(WireMockConfiguration.options().port(PORT_NUMBER).asynchronousResponseEnabled(true));
        wireMockServer.start();
        WireMock.configureFor("localhost", PORT_NUMBER);
    }

    private static void stubJwks(Path jwks) {
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/.well-known/jwks.json"))
                             .willReturn(WireMock.aResponse()
                                             .withHeader("Content-Type", "application/json")
                                             .withBody(IoUtils.stringFromResources(jwks))));
    }

    @Test
    @DisplayName("ApiGatewayHandler has a constructor with input class as only parameter")
    void apiGatewayHandlerHasAConstructorWithInputClassAsOnlyParameter() {
        RestRequestHandler<String, String> handler = new ApiGatewayHandler<>(String.class, environment) {
            @Override
            protected void validateRequest(String input, RequestInfo requestInfo, Context context)
                throws ApiGatewayException {
                //no-op
            }

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
    void handleRequestShouldHaveAvailableTheRequestHeaders() throws IOException {
        InputStream input = requestWithHeaders();

        getStringResponse(input, handler);

        Map<String, String> headers = handler.getHeaders();
        JsonNode expectedHeaders = createHeaders();
        expectedHeaders.fieldNames()
            .forEachRemaining(expectedHeader -> assertThat(headers.get(expectedHeader), is(equalTo(
                expectedHeaders.get(expectedHeader).textValue()))));
    }

    // TODO: Should return 415 when the Content-type header of request is unsupported (i.e., the one
    //  describing the content of the body of a post request)
    // TODO: Should return 406 when Accept header contains unsupported media type
    @ParameterizedTest(name = "handleRequest should return Unsupported media-type when input is {0}")
    @ValueSource(strings = {"application/xml", "text/plain; charset=UTF-8"})
    void handleRequestShouldReturnUnsupportedMediaTypeOnUnsupportedAcceptHeader(String mediaType)
        throws IOException {
        InputStream input = requestWithAcceptHeader(mediaType);

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)));
        String expectedMessage = getUnsupportedMediaTypeErrorMessage(mediaType);
        assertThat(response.getBody(), containsString(expectedMessage));
    }

    @ParameterizedTest(name = "handleRequest should return OK when input is {0}")
    @ValueSource(strings = {"*/*", "application/json", "application/json; charset=UTF-8",
        "text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8", "*; q=.2"
        // java.net.HttpURLConnection uses this Accept header
    })
    void handleRequestShouldReturnOkOnSupportedAcceptHeader(String mediaType) throws IOException {
        InputStream input = requestWithAcceptHeader(mediaType);

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void handleRequestReturnsContentTypeJsonOnAcceptWildcard() throws IOException {
        Handler handler = handlerThatOverridesListSupportedMediaTypes();

        InputStream input = requestWithAcceptHeader(MediaType.ANY_TYPE.toString());

        GatewayResponse<String> response = getStringResponse(input, handler);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getHeaders().get(CONTENT_TYPE), is(equalTo(MediaType.JSON_UTF_8.toString())));
    }

    @Test
    @DisplayName("handleRequest should have available the request path")
    void handleRequestShouldHaveAvailableTheRequestPath() throws IOException {
        InputStream input = requestWithHeadersAndPath();

        getStringResponse(input, handler);

        String actualPath = handler.getPath();
        JsonNode expectedHeaders = createHeaders();
        expectedHeaders.fieldNames().forEachRemaining(expectedHeader -> assertThat(actualPath, is(equalTo(PATH))));
    }

    @Test
    @DisplayName("handleRequest allows response headers depended on input")
    void apiGatewayHandlerAllowsResponseHeadersDependedOnInput() throws IOException {
        InputStream input = requestWithHeadersAndPath();
        GatewayResponse<String> response = getStringResponse(input, handler);
        assertTrue(response.getHeaders().containsKey(HttpHeaders.WARNING));
    }

    @Test
    void handleRequestDoesNotThrowExceptionOnUnknownFieldsInRequestInfo() throws IOException {
        InputStream input = IoUtils.inputStreamFromResources(EVENT_WITH_UNKNOWN_REQUEST_INFO);

        GatewayResponse<RequestBody> response = getResponse(RequestBody.class, input, handler);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        RequestBody body = response.getBodyObject(RequestBody.class);

        String expectedValueForField1 = "value1";
        String expectedValueForField2 = "value2";
        assertThat(body.getField1(), containsString(expectedValueForField1));
        assertThat(body.getField2(), containsString(expectedValueForField2));
    }

    @Test
    @DisplayName("Logger logs the whole exception stacktrace when an exception has occurred 2")
    void loggerLogsTheWholeExceptionStackTraceWhenAnExceptionHasOccurred() throws IOException {
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
    void loggerLogsTheWholeExceptionStackTraceWhenAnUncheckedExceptionHasOccurred() throws IOException {
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
    void handlerDoesNotRevealInformationForRuntimeExceptions() throws IOException {
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
    void shouldReturnProblemContainingCustomResource() throws IOException {
        var customProblemObject = new CustomObject(randomString(), randomString());
        var handler = handlerThatThrowsGoneExceptionsWithCustomObject(customProblemObject);
        var outputStream = outputStream();
        handler.handleRequest(requestWithHeaders(), outputStream, context);
        var problem = getProblemFromFailureResponse(outputStream);

        var resource = problem.getParameters().get("resource");
        assertThat(dtoObjectMapper.convertValue(resource, CustomObject.class), is(equalTo(customProblemObject)));
    }

    @Test
    void problemShouldNotContainCustomResourceWhenResourceIsNull() throws IOException {
        var handler = handlerThatThrowsGoneExceptionsWithCustomObject(null);
        var outputStream = outputStream();
        handler.handleRequest(requestWithHeaders(), outputStream, context);
        var problem = getProblemFromFailureResponse(outputStream);

        assertThat(problem.getParameters().get("resource"), is(nullValue()));
    }

    @Test
    void shouldReturnProblemContainingConflictingKeysWhenConflictExceptionHasKeys() throws IOException {
        var conflictingKeys = Map.of("sourceId", "12345", "organizationId", "org-789");
        var handler = handlerThatThrowsConflictExceptionWithKeys(conflictingKeys);
        var outputStream = outputStream();
        handler.handleRequest(requestWithHeaders(), outputStream, context);
        var problem = getProblemFromFailureResponse(outputStream);

        assertThat(problem.getStatus(), is(Status.CONFLICT));
        @SuppressWarnings("unchecked")
        var returnedKeys = (Map<String, String>) problem.getParameters().get(CONFLICTING_KEYS);
        assertThat(returnedKeys, hasEntry("sourceId", "12345"));
        assertThat(returnedKeys, hasEntry("organizationId", "org-789"));
    }

    @Test
    void problemShouldNotContainConflictingKeysWhenConflictExceptionHasNoKeys() throws IOException {
        var handler = handlerThatThrowsConflictExceptionWithoutKeys();
        var outputStream = outputStream();
        handler.handleRequest(requestWithHeaders(), outputStream, context);
        var problem = getProblemFromFailureResponse(outputStream);

        assertThat(problem.getStatus(), is(Status.CONFLICT));
        assertThat(problem.getParameters().containsKey(CONFLICTING_KEYS), is(false));
    }

    @Test
    @DisplayName("Failure message contains exception message and status code when an Exception is thrown")
    void failureMessageContainsExceptionMessageAndStatusCodeWhenAnExceptionIsThrown() throws IOException {
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
    void failureMessageContainsApplicationProblemJsonContentTypeWhenExceptionIsThrown() throws IOException {
        Handler handler = handlerThatThrowsExceptions();

        GatewayResponse<Problem> responseParsing = getProblemResponse(requestWithHeadersAndPath(), handler);

        assertThat(responseParsing.getHeaders().get(CONTENT_TYPE), is(equalTo(APPLICATION_PROBLEM_JSON.toString())));
    }

    @Test
    @DisplayName("getFailureStatusCode sets the status code to the ApiGatewayResponse")
    void getFailureStatusCodeSetsTheStatusCodeToTheApiGatewayResponse() throws IOException {
        Handler handler = handlerThatOverridesGetFailureStatusCode();

        GatewayResponse<Problem> response = getProblemResponse(anyRequest(), handler);

        assertThat(response.getStatusCode(), is(equalTo(OVERRIDDEN_STATUS_CODE)));
        assertThat(response.getBodyObject(Problem.class).getStatus().getStatusCode(),
                   is(equalTo(OVERRIDDEN_STATUS_CODE)));
    }

    @Test
    @DisplayName("getFailureStatusCode returns by default the status code of the ApiGatewayException")
    void getFailureStatusCodeReturnsByDefaultTheStatusCodeOfTheApiGatewayException() throws IOException {
        Handler handler = handlerThatThrowsExceptions();

        GatewayResponse<Problem> response = getProblemResponse(anyRequest(), handler);

        assertThat(response.getStatusCode(), is(equalTo(TestException.ERROR_STATUS_CODE)));
        assertThat(response.getBodyObject(Problem.class).getStatus().getStatusCode(),
                   is(equalTo(TestException.ERROR_STATUS_CODE)));
    }

    @Test
    void shouldReturnFailureHeadersWhenHandlerThrowsException() throws IOException {
        var handler = handlerThatThrowsExceptions();

        var response = getProblemResponse(anyRequest(), handler);
        var headers = response.getHeaders();

        assertThat(headers.get(CONTENT_TYPE), is(equalTo(APPLICATION_PROBLEM_JSON.toString())));
        assertThat(headers.get(CACHE_CONTROL), is(equalTo("no-cache")));
    }

    @Test
    void handlerLogsRequestIdForEveryRequest() throws IOException {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var output = outputStream();
        var expectedRequestId = context.getAwsRequestId();
        handler.handleRequest(requestWithHeadersAndPath(), output, context);
        assertThat(appender.getMessages(), containsString(expectedRequestId));
    }

    @Test
    void itReturnsResponseWithPopulatedFieldsAndBothEmptyAndNonEmptyLists() throws IOException {
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
    void handlerReturnsBadRequestWhenInputParsingFails() throws IOException {
        String expectedMessage = "Expected error message when parsing fails";
        Handler handler = handlerFailingWhenParsing(expectedMessage);
        InputStream input = requestWithHeadersAndPath();
        GatewayResponse<Problem> response = getProblemResponse(input, handler);
        Problem problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(expectedMessage));
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
    void handlerSerializesWithIsBase64Encoded() throws IOException {
        var output = outputStream();
        InputStream input = requestWithBodyWithEmptyFields();
        var isBase64EncodedHandler = new Base64Handler(environment);
        isBase64EncodedHandler.handleRequest(input, output, context);
        GatewayResponse<Void> response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getIsBase64Encoded(), is(true));
    }

    @Test
    void handlerSendsRedirectionWhenItReceivesARedirectException() throws IOException {
        var expectedRedirectLocation = randomUri();
        var expectedRedirectStatusCode = HttpURLConnection.HTTP_SEE_OTHER;
        var handler = new RedirectHandler(expectedRedirectLocation, expectedRedirectStatusCode, environment);
        var request = requestWithHeaders(Map.of(HttpHeaders.ACCEPT, MediaType.HTML_UTF_8.toString()));
        var response = getResponse(Void.class, request, handler);
        assertThat(response.getStatusCode(), is(equalTo(expectedRedirectStatusCode)));
        assertThat(response.getHeaders(), hasEntry(HttpHeaders.LOCATION, expectedRedirectLocation.toString()));
        assertThat(response.getHeaders(), hasEntry(ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD));
    }

    @Test
    void shouldReturnJsonObjectWhenClientAsksForJson() throws Exception {
        var handler = getRawStringResponseHandler();
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
        var handler = getRawStringResponseHandler();
        var inputStream = requestWithAcceptXmlHeader();
        var expected = objectMapper.convertValue(createBody(), RequestBody.class);
        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var xmlMapper = new XmlMapper();
        var actual = xmlMapper.readValue(response.getBody(), RequestBody.class);

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void shouldReturnAllOriginsWhenEnvironmentAllowsAllOrigins() throws IOException {
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");

        var handler = getRawStringResponseHandler();
        var inputStream = requestWithHeaders();

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var responseHeaders = response.getHeaders();
        assertThat(responseHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN), is(equalTo(ALL_ORIGINS_ALLOWED)));
        assertThat(responseHeaders.get(VARY), containsString("Origin"));
    }

    @Test
    void shouldReturnNvaFrontendProdWhenEnvironmentIsEmpty() throws IOException {
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("");

        var handler = getRawStringResponseHandler();
        var inputStream = requestWithHeaders();

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var responseHeaders = response.getHeaders();
        assertThat(responseHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN), is(equalTo(FALLBACK_ORIGIN)));
        assertThat(responseHeaders.get(VARY), containsString("Origin"));
    }

    @Test
    void shouldEchoAllowedOriginWhenEnvironmentContainsOrigin() throws IOException {
        var originInHeader = "https://example.com";
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("localhost, " + originInHeader + ", some-place-else");

        var handler = getRawStringResponseHandler();
        var inputStream = requestWithHeaders();

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var responseHeaders = response.getHeaders();
        assertThat(responseHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN), is(equalTo(originInHeader)));
        assertThat(responseHeaders.get(VARY), containsString("Origin"));
    }

    @Test
    void shouldRejectFakeJwt() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_WITH_MANIPULATED_TOKEN.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldAcceptValidJwt() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_WITH_NO_AUTHORIZER.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldLetNonAuthRequestsPass() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_WITH_UNKNOWN_REQUEST_INFO.toString());

        // No jwks stub, test would fail if requested

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldSkipCheckIfAuthenticatedWithApiGateway() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_WITH_ACCESS_RIGHTS_CLAIMS.toString());

        // No jwks stub, test would fail if requested

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldBeAbleToReadClaimsFromToken() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_WITH_NO_AUTHORIZER.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();

        createRequestInfoHandler().handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, RequestInfo.class);
        var requestInfo = response.getBodyObject(RequestInfo.class);

        assertThat(requestInfo.getAccessRights(), containsInAnyOrder(
            AccessRight.MANAGE_OWN_AFFILIATION,
            AccessRight.MANAGE_CUSTOMERS,
            AccessRight.ACT_AS,
            AccessRight.MANAGE_IMPORT,
            AccessRight.MANAGE_NVI,
            AccessRight.MANAGE_OWN_RESOURCES,
            AccessRight.MANAGE_EXTERNAL_CLIENTS
        ));
    }

    private ApiGatewayHandler<String, RequestInfo> createRequestInfoHandler() {
        return new ApiGatewayHandler<>(String.class, environment) {
            @Override
            protected void validateRequest(String input, RequestInfo requestInfo, Context context) {
                //no-op
            }

            @Override
            protected RequestInfo processInput(String input, RequestInfo requestInfo, Context context) {
                return requestInfo;
            }

            @Override
            protected Integer getSuccessStatusCode(String input, RequestInfo output) {
                return HttpURLConnection.HTTP_OK;
            }
        };
    }

    @Test
    void shouldRejectUnSignedAlgorithm() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_UNSIGNED.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "rs256", "rs384", "rs512", "es256", "es384", "es512"
    })
    void shouldAcceptAlgorithms(String algorithm) throws IOException {
        var path = Path.of("apiGatewayMessages",
                String.format("event_signed_with_%s.json", algorithm));
        var inputStream = IoUtils.inputStreamFromResources(path.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldRejectUnsupportedAlgorithm() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_SIGNED_UNSUPPORTED.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldAcceptValidExternalClient() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_EXTERNAL_CLIENT.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldBeAbleToReadClaimsFromExternalClientToken() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_EXTERNAL_CLIENT.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();

        createRequestInfoHandler().handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, RequestInfo.class);
        var requestInfo = response.getBodyObject(RequestInfo.class);

        assertThat(requestInfo.clientIsThirdParty(), is(equalTo(true)));
        assertThat(requestInfo.getClientId().orElseThrow(), is(equalTo("abc123")));
    }

    @Test
    void shouldRejectRequestWhenThereIsNoMatchOnJwkIssuer() throws IOException {
        var inputStream = IoUtils.inputStreamFromResources(EVENT_WITH_WRONG_ISSUER.toString());

        stubJwks(TEST_JWKS);

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    private RawStringResponseHandler getRawStringResponseHandler() {
        return new RawStringResponseHandler(environment);
    }

    @Test
    void shouldReturnOneOfTheAllowedOriginsInEnvironmentWhenRequestOriginIsNotWhiteListed()
        throws IOException {
        var originInHeader = "https://example.com";
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("localhost, some-place-else");

        var handler = getRawStringResponseHandler();
        var inputStream = requestWithHeaders();

        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var responseHeaders = response.getHeaders();
        assertThat(responseHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN), not(equalTo(originInHeader)));
        assertThat(responseHeaders.get(VARY), containsString("Origin"));
    }

    @Test
    void shouldReturnFirstElementInAllowedOriginsListWhenOriginIsMissing() throws IOException {
        var header1 = "https://example1.com";
        var header2 = "https://example2.com";
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(header1 + ", " + header2);
        var handler = getRawStringResponseHandler();
        var inputStream = requestWithMissingOriginHeader();
        var outputStream = outputStream();
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        var responseHeaders = response.getHeaders();
        assertThat(responseHeaders.get(ACCESS_CONTROL_ALLOW_ORIGIN), equalTo(header1));
        assertThat(responseHeaders.get(VARY), containsString("Origin"));
    }

    private String getUnsupportedMediaTypeErrorMessage(String mediaType) {
        return UnsupportedAcceptHeaderException.createMessage(List.of(MediaType.parse(mediaType)),
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

        return new HandlerRequestBuilder<RequestBody>(defaultRestObjectMapper).withBody(requestBody).build();
    }

    private Problem getProblemFromFailureResponse(ByteArrayOutputStream outputStream) throws JsonProcessingException {
        JavaType javaType = defaultRestObjectMapper.getTypeFactory()
                                .constructParametricType(GatewayResponse.class, Problem.class);
        GatewayResponse<Problem> response = defaultRestObjectMapper.readValue(outputStream.toString(), javaType);
        return response.getBodyObject(Problem.class);
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

    private Handler handlerThatThrowsGoneExceptionsWithCustomObject(CustomObject customObject) {
        return new Handler(environment) {
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

    private Handler handlerThatThrowsConflictExceptionWithKeys(Map<String, String> conflictingKeys) {
        return new Handler(environment) {
            @Override
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
                throws ConflictException {
                throw new ConflictException("Resource already exists", conflictingKeys);
            }
        };
    }

    private Handler handlerThatThrowsConflictExceptionWithoutKeys() {
        return new Handler(environment) {
            @Override
            protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
                throws ConflictException {
                throw new ConflictException("Resource already exists");
            }
        };
    }

    private Handler handlerThatOverridesListSupportedMediaTypes() {
        return new Handler(environment) {
            @Override
            public List<MediaType> listSupportedMediaTypes() {
                return List.of(MediaType.JSON_UTF_8, MediaTypes.APPLICATION_JSON_LD,
                               MediaTypes.APPLICATION_DATACITE_XML, MediaTypes.SCHEMA_ORG);
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

    private InputStream requestWithMissingOriginHeader() throws JsonProcessingException {
        ObjectNode request = defaultRestObjectMapper.createObjectNode();
        ObjectNode node = createBody();
        request.set("body", node);
        request.set("headers", createHeadersWithMissingOrigin());
        return jsonNodeToInputStream(request);
    }

    private JsonNode createHeadersWithMissingOrigin() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
        headers.put(CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
        headers.put(X_CONTENT_TYPE_OPTIONS, "nosniff");
        headers.put(STRICT_TRANSPORT_SECURITY, "max-age=63072000; includeSubDomains; preload");
        return createHeaders(headers);
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
        headers.put("origin", "https://example.com");
        headers.put(X_CONTENT_TYPE_OPTIONS, "nosniff");
        headers.put(STRICT_TRANSPORT_SECURITY, "max-age=63072000; includeSubDomains; preload");
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
        return new Handler(environment) {
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
