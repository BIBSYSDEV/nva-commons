package no.unit.commons.apigateway.authentication;

import static no.unit.commons.apigateway.authentication.RequestAuthorizerTest.METHOD_ARN_FIELD;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JsonUtils;
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
    private ByteArrayOutputStream outputStream;
    private String inputMethodArn;
    private String arnForAllowingAllActionsOnLambda;

    @BeforeEach
    public void init() {
        this.expectedApiKey = randomString();
        this.inputMethodArn = String.join(RequestAuthorizer.PATH_DELIMITER, randomString(), randomString());
        this.arnForAllowingAllActionsOnLambda =
            String.join(RequestAuthorizer.PATH_DELIMITER, inputMethodArn, WILDCARD, WILDCARD);
        this.handler = new DefaultRequestAuthorizer(setupSecretsClient(), randomString());
        this.outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnAuthPolicyWhenApiKeyIsValid() throws IOException, ForbiddenException {
        var input = requestWithValidApiKey();
        var returnedPolicy = sendRequestToHandler(input);
        var expectedPolicy = handler.createAllowAuthPolicy(arnForAllowingAllActionsOnLambda);
        assertThat(returnedPolicy, is(equalTo(expectedPolicy)));
    }

    @Test
    void shouldReturnForbiddenWhenApiKeyIsInvalid() throws IOException, ForbiddenException {
        final var appender = LogUtils.getTestingAppenderForRootLogger();
        var input = requestWithInvalidApiKey();
        var returnedPolicy = sendRequestToHandler(input);
        var expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(returnedPolicy, is(equalTo(expectedPolicy)));
        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    @Test
    void shouldReturnForbiddenWhenApiKeyIsMissing() throws IOException, ForbiddenException {
        final var appender = LogUtils.getTestingAppenderForRootLogger();
        var input = requestWithoutApiKey();
        var returnedPolicy = sendRequestToHandler(input);
        var expectedPolicy = handler.createDenyAuthPolicy();
        assertThat(returnedPolicy, is(equalTo(expectedPolicy)));

        assertThat(appender.getMessages(), containsString(ForbiddenException.DEFAULT_MESSAGE));
    }

    private InputStream requestWithoutApiKey() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withOtherProperties(Map.of(METHOD_ARN_FIELD, this.inputMethodArn))
            .build();
    }

    private AuthPolicy sendRequestToHandler(InputStream input) throws IOException {
        handler.handleRequest(input, outputStream, CONTEXT);
        var response = AuthorizerResponse.fromOutputStream(outputStream);
        return response.getPolicyDocument();
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

    private InputStream requestWithValidApiKey() throws JsonProcessingException {
        return requestWithApiKey(expectedApiKey);
    }

    private InputStream requestWithInvalidApiKey() throws JsonProcessingException {
        return requestWithApiKey(randomString());
    }

    private InputStream requestWithApiKey(String s) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withHeaders(Map.of(HttpHeaders.AUTHORIZATION, s))
            .withOtherProperties(Map.of(METHOD_ARN_FIELD, this.inputMethodArn))
            .build();
    }
}