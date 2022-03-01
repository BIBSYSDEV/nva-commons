package no.unit.commons.apigateway.authentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.google.common.net.HttpHeaders;
import java.util.Map;
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
    public static final String UNEXPECTED_EXCEPTION_MESSAGE = "UnexpectedExceptionMessage";
    private static final String WRONG_KEY = "WrongKey";
    private final Context context = mock(Context.class);
    private final RequestAuthorizer handler = sampleHandler();

    public static APIGatewayCustomAuthorizerEvent createRequestStream(String apiKey) {
        return APIGatewayCustomAuthorizerEvent.builder()
            .withHeaders(authHeaders(apiKey))
            .withMethodArn(DEFAULT_METHOD_ARN)
            .build();
    }

    public static Map<String, String> authHeaders(String apiKey) {
        return Map.of(HttpHeaders.AUTHORIZATION, apiKey);
    }

    @Test
    void authorizerReturnsAuthPolicyWhenApiKeyIsValid() {
        var input = requestWithValidApiKey();

        var response = handler.handleRequest(input, context);
        var expectedPolicy = handler.createAllowAuthPolicy(EXPECTED_RESOURCE);

        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));
    }

    @Test
    void authorizerReturnsForbiddenWhenApiKeyIsInvalid() {
        var input = requestWithInvalidApiKey();
        var response = handler.handleRequest(input, context);
        var expectedPolicy = handler.createDenyAuthPolicy();

        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));
    }

    @Test
    void authorizerReturnsForbiddenWhenApiKeyIsMissing() {
        var input = requestWithoutApiKey();
        var response = handler.handleRequest(input, context);
        var expectedPolicy = handler.createDenyAuthPolicy();

        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));
    }

    @Test
    void authorizerReturnsForbiddenWhenFetchingSecretThrowsExceptionAndLogsMessage() {
        final TestAppender appender = LogUtils.getTestingAppender(RequestAuthorizer.class);

        var handler = handlerThrowingExceptionWhenFetchingSecret();
        var response = processRequestWithHandlerThrowingException(handler);
        var expectedPolicy = handler.createDenyAuthPolicy();

        assertThat(response.getPolicyDocument(), is(equalTo(expectedPolicy)));
        assertThat(appender.getMessages(), containsString(UNEXPECTED_EXCEPTION_MESSAGE));
    }

    @Test
    void authorizerThrowsExceptionWhenFetchingPrincipalExceptionThrowsExceptionInFailureResponse() {
        var handler = handlerThrowingExceptionWhenFetchingPrincipalId();
        Executable action = () -> processRequestWithHandlerThrowingException(handler);

        var exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(RequestAuthorizer.COULD_NOT_READ_PRINCIPAL_ID_ERROR));
    }

    private AuthorizerResponse processRequestWithHandlerThrowingException(RequestAuthorizer handler) {
        var request = requestWithValidApiKey();
        return handler.handleRequest(request, context);
    }

    private APIGatewayCustomAuthorizerEvent requestWithValidApiKey() {
        return createRequestStream(CORRECT_KEY);
    }

    private APIGatewayCustomAuthorizerEvent requestWithInvalidApiKey() {
        return createRequestStream(WRONG_KEY);
    }

    private APIGatewayCustomAuthorizerEvent requestWithoutApiKey() {
        return APIGatewayCustomAuthorizerEvent.builder()
            .withMethodArn(DEFAULT_METHOD_ARN)
            .build();
    }

    private RequestAuthorizer sampleHandler() {
        return new RequestAuthorizer() {
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

    private RequestAuthorizer handlerThrowingExceptionWhenFetchingSecret() {
        return new RequestAuthorizer() {

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
        return new RequestAuthorizer() {

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
}