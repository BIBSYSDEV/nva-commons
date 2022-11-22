package nva.commons.secrets;

import static java.util.Objects.isNull;
import static nva.commons.secrets.SecretsWriter.COULD_NOT_WRITE_SECRET_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.testutils.Credentials;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.InvalidParameterException;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse;

class SecretsWriterTest {
    static final String SECRET_NAME = "secret_id";
    static final String SECRET_VALUE = "secret_value";
    static final String JSON_SECRET_NAME = "json_secret_id";
    static final String JSON_SECRET_VALUE = "{\"username\":\"name\", \"password\":\"pass\"}";
    static final String JSON_SECRET_VALUE_USERNAME = "name";
    static final String JSON_SECRET_VALUE_PASSWORD = "pass";

    private final SecretsWriter secretsWriter;
    private final Credentials credentials;


    public SecretsWriterTest() {
        secretsWriter = createSecretsWriterMock();
        credentials = createCredentialsMock();
    }

    private Credentials createCredentialsMock() {
        return new Credentials(JSON_SECRET_VALUE_USERNAME,JSON_SECRET_VALUE_PASSWORD);
    }

    @Test
    @DisplayName("Update Secret String successfully")
    void assertUpdateSecretOK() {

        var putResponse = secretsWriter.updateSecret(SECRET_NAME, SECRET_VALUE);
        assertEquals(putResponse.name(), SECRET_NAME);
    }

    @Test
    @DisplayName("Update Secret JsonString successfully")
    void assertUpdateSecretJsonOK() {

        var putResponse = secretsWriter.updateSecret(JSON_SECRET_NAME, JSON_SECRET_VALUE);
        assertEquals(putResponse.name(), JSON_SECRET_NAME);
    }

    @Test
    @DisplayName("Update Secret Object successfully")
    void assertUpdateSecretObjectOK() {

        var putResponse = secretsWriter.updateClassSecret(JSON_SECRET_NAME, credentials);
        assertEquals(putResponse.name(), JSON_SECRET_NAME);
    }

    @Test
    @DisplayName("Update Secret Logs Error When Wrong Secret Name Is Given")
    void errorWhenWrongSecretNameIsGiven() {
        final TestAppender appender = LogUtils.getTestingAppender(SecretsWriter.class);
        Executable action = () -> secretsWriter.updateSecret(null, SECRET_VALUE);

        assertThrows(ErrorWritingSecretException.class, action);
        assertThat(appender.getMessages(), containsString(COULD_NOT_WRITE_SECRET_ERROR));
    }

    @Test
    @DisplayName("Update Secret Logs Error When Wrong Secret Value Is Given")
    void errorWhenWrongSecretValueIsGiven() {
        Executable action = () -> secretsWriter.updateSecret(SECRET_NAME, null);
        ErrorWritingSecretException exception = assertThrows(ErrorWritingSecretException.class, action);

        assertThat(exception.getMessage(), not(containsString(SECRET_NAME)));
        assertThat(exception.getMessage(), not(containsString(SECRET_VALUE)));
    }

    private SecretsWriter createSecretsWriterMock() {
        var secretsManager = mock(SecretsManagerClient.class);
        when(secretsManager.putSecretValue(any(PutSecretValueRequest.class))
        ).thenAnswer(this::providePutSecretValueResult);
        return new SecretsWriter(secretsManager);
    }

    private PutSecretValueResponse providePutSecretValueResult(InvocationOnMock invocation) {
        PutSecretValueRequest request = invocation.getArgument(0);
        if (isNull(request.secretId()))  {
            throw InvalidParameterException.create( COULD_NOT_WRITE_SECRET_ERROR,null);
        }
        if (isNull(request.secretString()))  {
            throw InvalidParameterException.create( COULD_NOT_WRITE_SECRET_ERROR,null);
        }

        return createPutSecretValueResult(request.secretId());
    }

    private PutSecretValueResponse createPutSecretValueResult(String secretName) {
        return PutSecretValueResponse.builder()
                   .arn("arn:aws:iam::123456789012:user/Development/product_1234/")
                   .name(secretName)
                   .build();
    }

}