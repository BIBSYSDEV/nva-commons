package no.unit.commons.apigateway.authentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.HttpHeaders;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class RequestAuthorizerTest {

    public static final String SOME_PRINCIPAL_ID = "somePrincipalId";
    public static final String CORRECT_KEY = "SOME_KEY";
    public static final String DEFAULT_METHOD_ARN = "arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/GET"
        + "/service/users/orestis@unit.no";
    public static final String EXPECTED_RESOURCE = "arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/*/*";
    public static final String METHOD_ARN_FIELD = "methodArn";
    public static final String UNEXPECTED_EXCEPTION_MESSAGE = "UnexpectedExceptionMessage";
    public static final String DEFAULT_ENV_VALUE = "*";
    private static final String WRONG_KEY = "WrongKey";
    private final Context context = mock(Context.class);
    private final RequestAuthorizer handler = sampleHandler();

    @Test
    public void authorizerReturnsOkHeaderInSuccess() {
        assertThat(handler.getSuccessStatusCode(null, null), is(HttpURLConnection.HTTP_OK));
    }

    @Test
    public void authorizerReturnsAuthPolicyWhenApiKeyIsValid() throws IOException, ForbiddenException {

        InputStream input = requestWithValidApiKey();
        AuthorizerResponse response = sendRequestToHandler(input);

        AuthPolicy expectedPolicy = handler.createAllowAuthPolicy(EXPECTED_RESOURCE);

        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));
    }

    @Test
    public void authorizerReturnsForbiddenWhenApiKeyIsInvalid() throws IOException, ForbiddenException {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        InputStream input = requestWithInvalidApiKey();

        AuthorizerResponse response = sendRequestToHandler(input);

        AuthPolicy expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    @Test
    public void authorizerReturnsForbiddenWhenApiKeyIsMissing() throws IOException, ForbiddenException {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();

        InputStream input = requestWithoutApiKey();

        AuthorizerResponse response = sendRequestToHandler(input);
        AuthPolicy expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    @Test
    public void authorizerReturnsForbiddenForUnexpectedExceptionAndLogsMessage() throws IOException,
                                                                                        ForbiddenException {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();

        RequestAuthorizer handler = handlerThrowingUnexpectedException();
        AuthorizerResponse response = processRequestWithHandlerThrowingException(handler);

        AuthPolicy expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(UNEXPECTED_EXCEPTION_MESSAGE));
    }

    @Test
    public void authorizerReturnsForbiddenWhenFetchingSecretThrowsExceptionAndLogsMessage()
        throws IOException, ForbiddenException {
        final TestAppender appender = LogUtils.getTestingAppender(RequestAuthorizer.class);

        RequestAuthorizer handler = handlerThrowingExceptionWhenFetchingSecret();
        AuthorizerResponse response = processRequestWithHandlerThrowingException(handler);

        AuthPolicy expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(UNEXPECTED_EXCEPTION_MESSAGE));
    }

    @Test
    public void authorizerThrowsExceptionWhenFetchingPrincipalExceptionThrowsExceptionInFailureResponse() {
        final TestAppender appender = LogUtils.getTestingAppender(RequestAuthorizer.class);

        RequestAuthorizer handler = handlerThrowingExceptionWhenFetchingPrincipalId();
        Executable action = () -> processRequestWithHandlerThrowingException(handler);

        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(UNEXPECTED_EXCEPTION_MESSAGE));

        assertThat(appender.getMessages(), containsString(UNEXPECTED_EXCEPTION_MESSAGE));
    }

    private AuthorizerResponse processRequestWithHandlerThrowingException(RequestAuthorizer handler)
        throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream request = requestWithValidApiKey();

        handler.handleRequest(request, outputStream, context);

        return AuthorizerResponse.fromOutputStream(outputStream);
    }

    private AuthorizerResponse sendRequestToHandler(InputStream input) throws IOException {
        ByteArrayOutputStream output = outputStream();
        handler.handleRequest(input, output, context);
        return AuthorizerResponse.fromOutputStream(output);
    }

    private InputStream requestWithValidApiKey() throws com.fasterxml.jackson.core.JsonProcessingException {
        return createRequestStream(CORRECT_KEY);
    }

    private InputStream requestWithInvalidApiKey() throws com.fasterxml.jackson.core.JsonProcessingException {
        return createRequestStream(WRONG_KEY);
    }

    private InputStream requestWithoutApiKey() throws com.fasterxml.jackson.core.JsonProcessingException {
        Map<String, Object> methodArn = Map.of(METHOD_ARN_FIELD, DEFAULT_METHOD_ARN);

        return new HandlerRequestBuilder<Map<String, String>>(JsonUtils.objectMapper)
            .withOtherProperties(methodArn)
            .build();
    }

    private InputStream createRequestStream(String apiKey)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        Map<String, Object> methodArn = Map.of(METHOD_ARN_FIELD, DEFAULT_METHOD_ARN);

        return new HandlerRequestBuilder<Map<String, String>>(JsonUtils.objectMapper)
            .withHeaders(authHeaders(apiKey))
            .withOtherProperties(methodArn)
            .build();
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

    private Map<String, String> authHeaders(String apiKey) {
        return Map.of(HttpHeaders.AUTHORIZATION, apiKey);
    }

    private RequestAuthorizer sampleHandler() {
        return new RequestAuthorizer(mockEnvironment()) {
            @Override
            protected String principalId() {
                return SOME_PRINCIPAL_ID;
            }

            @Override
            protected String fetchSecret() {
                return CORRECT_KEY;
            }
        };
    }

    private RequestAuthorizer handlerThrowingUnexpectedException() {
        return new RequestAuthorizer(mockEnvironment()) {
            @Override
            public AuthorizerResponse processInput(Void input, RequestInfo requestInfo, Context context) {
                throw new RuntimeException(UNEXPECTED_EXCEPTION_MESSAGE);
            }

            @Override
            protected String principalId() {
                return null;
            }

            @Override
            protected String fetchSecret() {
                return null;
            }
        };
    }

    private RequestAuthorizer handlerThrowingExceptionWhenFetchingSecret() {
        return new RequestAuthorizer(mockEnvironment()) {

            @Override
            protected String principalId() {
                return null;
            }

            @Override
            protected String fetchSecret() {
                throw new RuntimeException(UNEXPECTED_EXCEPTION_MESSAGE);
            }
        };
    }

    private RequestAuthorizer handlerThrowingExceptionWhenFetchingPrincipalId() {
        return new RequestAuthorizer(mockEnvironment()) {

            @Override
            protected String principalId() {
                throw new RuntimeException(UNEXPECTED_EXCEPTION_MESSAGE);
            }

            @Override
            protected String fetchSecret() {
                return WRONG_KEY;
            }
        };
    }

    private Environment mockEnvironment() {
        return new Environment() {
            @Override
            public Optional<String> readEnvOpt(String envVariable) {
                return Optional.ofNullable(readEnv(envVariable));
            }

            @Override
            public String readEnv(String envVariable) {
                return DEFAULT_ENV_VALUE;
            }
        };
    }
}