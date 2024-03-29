package nva.commons.secrets;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.secrets.SecretsWriter.COULD_NOT_WRITE_SECRET_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.testutils.Credentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.InvalidParameterException;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse;

class SecretsWriterTest {

    static final String SECRET_NAME = "secret_key";
    static final String SECRET_VALUE = "secret_value";
    static final String JSON_SECRET_NAME = "json_secret_key";
    static final String JSON_SECRET_VALUE = "{\"username\":\"name\", \"password\":\"pass\"}";
    static final String JSON_SECRET_VALUE_USERNAME = "name";
    static final String JSON_SECRET_VALUE_PASSWORD = "pass";
    private static final String SECRET_VAULT_ID = "test_app_secret_vault_id";
    private final SecretsWriter secretsWriter;
    private final Credentials credentials;
    private final ObjectMapper objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(
            Arguments.of(null, SECRET_VALUE),
            Arguments.of(null, null)
        );
    }

    private static Stream<Arguments> argumentProvider() {
        return Stream.of(
            Arguments.of(SECRET_NAME, SECRET_VALUE),
            Arguments.of(JSON_SECRET_NAME, JSON_SECRET_VALUE)
        );
    }

    public SecretsWriterTest() {
        secretsWriter = createSecretsWriterMock();
        credentials = createCredentialsMock();
    }

    private Credentials createCredentialsMock() {
        return new Credentials(JSON_SECRET_VALUE_USERNAME, JSON_SECRET_VALUE_PASSWORD);
    }

    @Test
    @DisplayName("Update Secret Object successfully")
    void assertUpdateSecretObjectOK() {

        var putResponse = secretsWriter.updateSecretObject(SECRET_VAULT_ID, credentials);
        assertThat(putResponse, is(equalTo(SECRET_VAULT_ID)));
    }

    @DisplayName("Update Secret String successfully")
    @MethodSource("argumentProvider")
    @ParameterizedTest(name = "Good {index} -> k:{0}, v:{1}")
    void assertUpdateSecretOK(String name, String value) {

        var putResponse = secretsWriter.updateSecretKey(SECRET_VAULT_ID,name, value);
        assertThat(putResponse, is(equalTo(SECRET_VAULT_ID)));
    }


    @DisplayName("Update Secret, logs error when wrong input is given")
    @MethodSource("invalidArgumentProvider")
    @ParameterizedTest(name = "Bad {index} -> k:{0}, v:{1}")
    void errorWhenWrongSecretNameIsGiven(String name, String value) {

        final TestAppender appender = LogUtils.getTestingAppender(SecretsWriter.class);
        Executable action = () -> secretsWriter.updateSecretKey(SECRET_VAULT_ID,name, value);

        assertThrows(ErrorWritingSecretException.class, action);
        assertThat(appender.getMessages(), containsString(COULD_NOT_WRITE_SECRET_ERROR));
    }

    private SecretsWriter createSecretsWriterMock() {

        var secretsManager = mock(SecretsManagerClient.class);

        when(secretsManager.putSecretValue(any(PutSecretValueRequest.class))
        ).thenAnswer(this::providePutSecretValueResult);

        when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))
        ).thenAnswer(this::provideGetSecretValueResult);

        return new SecretsWriter(secretsManager);
    }

    private GetSecretValueResponse provideGetSecretValueResult(InvocationOnMock invocationOnMock)   {

        var result = (GetSecretValueRequest)invocationOnMock.getArgument(0);
        var secret = attempt(() -> objectMapper.writeValueAsString(credentials)).get();

        return GetSecretValueResponse.builder()
                   .name(result.secretId())
                   .secretString(secret)
                   .build();
    }

    private PutSecretValueResponse providePutSecretValueResult(InvocationOnMock invocation) {

        PutSecretValueRequest request = invocation.getArgument(0);
        if (isNull(request.secretId())) {
            throw InvalidParameterException.create(COULD_NOT_WRITE_SECRET_ERROR, null);
        }
        if (isNull(request.secretString())) {
            throw InvalidParameterException.create(COULD_NOT_WRITE_SECRET_ERROR, null);
        }

        return createPutSecretValueResult(request.secretId());
    }

    private PutSecretValueResponse createPutSecretValueResult(String secretName) {
        return PutSecretValueResponse.builder()
                   .arn("arn:aws:iam::123456789012:user/product_1234/")
                   .name(secretName)
                   .build();
    }
}