package no.unit.commons.apigateway.authentication;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import com.google.common.net.HttpHeaders;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class DefaultRequestAuthorizerTest {

    public static final String WILDCARD = "*";
    public static final Context CONTEXT = mock(Context.class);
    private static final String SECRET_NAME_SET_IN_TEST = "secretName-1234";
    private static final String SECRET_KEY_SET_IN_TEST = "secretKey-1234";
    private String expectedApiKey;
    private DefaultRequestAuthorizer handler;
    private String inputMethodArn;
    private String arnForAllowingAllActionsOnLambda;

    @BeforeEach
    public void init() {
        this.expectedApiKey = randomString();
        this.inputMethodArn = String.join(RequestAuthorizer.PATH_DELIMITER, randomString(), randomString());
        this.arnForAllowingAllActionsOnLambda =
            String.join(RequestAuthorizer.PATH_DELIMITER, inputMethodArn, WILDCARD, WILDCARD);
        this.handler = new DefaultRequestAuthorizer(setupSecretsClient(), randomString());
    }

    @Test
    void shouldReturnAuthPolicyWhenApiKeyIsValid() {
        var input = requestWithValidApiKey();
        var returnedPolicy = sendRequestToHandler(input);
        var expectedPolicy = handler.createAllowAuthPolicy(arnForAllowingAllActionsOnLambda);
        assertThat(returnedPolicy, is(equalTo(expectedPolicy)));
    }

    @Test
    void shouldReturnForbiddenWhenApiKeyIsInvalid() {
        final var appender = LogUtils.getTestingAppenderForRootLogger();
        var input = requestWithInvalidApiKey();
        var returnedPolicy = sendRequestToHandler(input);
        var expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(returnedPolicy, is(equalTo(expectedPolicy)));
        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    @Test
    void shouldReturnForbiddenWhenApiKeyIsMissing() {
        final var appender = LogUtils.getTestingAppenderForRootLogger();
        var input = requestWithoutApiKey();
        var returnedPolicy = sendRequestToHandler(input);
        var expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(returnedPolicy, is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    private APIGatewayCustomAuthorizerEvent requestWithoutApiKey() {
        return APIGatewayCustomAuthorizerEvent.builder().withMethodArn(this.inputMethodArn).build();
    }

    private AuthPolicy sendRequestToHandler(APIGatewayCustomAuthorizerEvent input) {
        return handler.handleRequest(input, CONTEXT).getPolicyDocument();
    }

    private SecretsManagerClient setupSecretsClient() {
        var secretsClient = mock(SecretsManagerClient.class);
        when(secretsClient.getSecretValue(any(GetSecretValueRequest.class))).thenAnswer(invocation -> {
            GetSecretValueRequest request = invocation.getArgument(0);
            return constructSecretsClientResponse(request);
        });
        return secretsClient;
    }

    private GetSecretValueResponse constructSecretsClientResponse(GetSecretValueRequest request) {

        if (request.secretId().equals(SECRET_NAME_SET_IN_TEST)) {
            var secret = JsonUtils.dtoObjectMapper.createObjectNode();
            secret.put(SECRET_KEY_SET_IN_TEST, expectedApiKey);
            return GetSecretValueResponse.builder().secretString(secret.toString()).build();
        }
        return
            GetSecretValueResponse.builder().build();
    }

    private APIGatewayCustomAuthorizerEvent requestWithValidApiKey() {
        return requestWithApiKey(expectedApiKey);
    }

    private APIGatewayCustomAuthorizerEvent requestWithInvalidApiKey() {
        return requestWithApiKey(randomString());
    }

    private APIGatewayCustomAuthorizerEvent requestWithApiKey(String authorizationHeader) {
        return APIGatewayCustomAuthorizerEvent.builder()
            .withMethodArn(this.inputMethodArn)
            .withHeaders(Map.of(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .build();
    }
}