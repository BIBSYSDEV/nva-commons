package nva.commons.handlers.authentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class ServiceAuthorizerHandlerTest {

    public static final String SOME_PRINCIPAL_ID = "somePrincipalId";
    public static final String CORRECT_KEY = "SOME_KEY";
    public static final String DEFAULT_METHOD_ARN = "SomeMethodArn";
    public static final String METHOD_ARN_FIELD = "methodArn";
    public static final String UNEXPECTED_EXCEPTION_MESSAGE = "UnexpectedExceptionMessage";
    private static final String WRONG_KEY = "WrongKey";
    private final Context context = mock(Context.class);
    private final ServiceAuthorizerHandler handler = sampleHandler();

    @Test
    public void authorizerReturnsOkHeaderInSuccess() {
        assertThat(handler.getSuccessStatusCode(null, null), is(HttpStatus.SC_OK));
    }

    @Test
    public void authorizerReturnsAuthPolicyWhenApiKeyIsValid() throws IOException {

        InputStream input = requestWithValidApiKey();
        AuthorizerResponse response = sendRequestToHandler(input);

        AuthPolicy expectedPolicy = ServiceAuthorizerHandler.createAllowAuthPolicy(DEFAULT_METHOD_ARN);

        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));
    }

    @Test
    public void authorizerReturnsForbiddenWhenApiKeyIsInvalid() throws IOException {
        final TestAppender appender = LogUtils.getTestingAppender(ServiceAuthorizerHandler.class);
        InputStream input = requestWithInvalidApiKey();

        AuthorizerResponse response = sendRequestToHandler(input);

        AuthPolicy expectedPolicy = ServiceAuthorizerHandler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    @Test
    public void authorizerReturnsForbiddenWhenApiKeyIsMissing() throws IOException {
        final TestAppender appender = LogUtils.getTestingAppender(ServiceAuthorizerHandler.class);

        InputStream input = requestWithoutApiKey();

        AuthorizerResponse response = sendRequestToHandler(input);
        AuthPolicy expectedPolicy = ServiceAuthorizerHandler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    @Test
    public void authorizerReturnsForbiddenForUnexpectedExceptionAndLogsMessage() throws IOException {
        final TestAppender appender = LogUtils.getTestingAppender(ServiceAuthorizerHandler.class);

        ServiceAuthorizerHandler handler = handlerThrowingUnexpectedException();
        AuthorizerResponse response = processRequestWithHanlderThrowingException(handler);

        AuthPolicy expectedPolicy = ServiceAuthorizerHandler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(UNEXPECTED_EXCEPTION_MESSAGE));
    }

    @Test
    public void authorizerReturnsForbiddenWhenFetchingSecretThrowsExceptionAndLogsMessage() throws IOException {
        final TestAppender appender = LogUtils.getTestingAppender(ServiceAuthorizerHandler.class);

        ServiceAuthorizerHandler handler = handlerThrowingExceptionWhenFetchingSecret();
        AuthorizerResponse response = processRequestWithHanlderThrowingException(handler);

        AuthPolicy expectedPolicy = ServiceAuthorizerHandler.createDenyAuthPolicy();
        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(UNEXPECTED_EXCEPTION_MESSAGE));
    }

    private AuthorizerResponse processRequestWithHanlderThrowingException(ServiceAuthorizerHandler handler)
        throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream request = requestWithValidApiKey();

        handler.handleRequest(request, outputStream, context);

        AuthorizerResponse response = AuthorizerResponse.fromOutputStream(outputStream);
        return response;
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

    private ServiceAuthorizerHandler sampleHandler() {
        return new ServiceAuthorizerHandler() {
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

    private ServiceAuthorizerHandler handlerThrowingUnexpectedException() {
        return new ServiceAuthorizerHandler() {
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

    private ServiceAuthorizerHandler handlerThrowingExceptionWhenFetchingSecret() {
        return new ServiceAuthorizerHandler() {

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
}