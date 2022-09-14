package nva.commons.secrets;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SecretsReaderTest {

    public static final String SECRET_VALUE = "SECRET_VALUE";
    public static final String SECRET_KEY = "SECRET_KEY";
    public static final String SECRET_NAME = "SECRET_NAME";
    public static final String PLAIN_TEXT_SECRET_NAME = "PLAIN_TEXT_SECRET_NAME";
    public static final String PLAIN_TEXT_SECRET_VALUE = "PLAIN_TEXT_SECRET_VALUE";
    public static final String WRONG_SECRET_NAME = "WRONG_SECRET_NAME";
    public static final String WRONG_SECRET_KEY = "WRONG_KEY";
    public static final String ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER = "Secret not found";
    private final SecretsReader secretsReader;

    public SecretsReaderTest() {
        secretsReader = createSecretsReaderMock();
    }

    @Test
    public void fetchSecretLogsErrorWhenWrongSecretNameIsGiven() {
        final TestAppender appender = LogUtils.getTestingAppender(SecretsReader.class);
        Executable action = () -> secretsReader.fetchSecret(WRONG_SECRET_NAME, SECRET_KEY);
        assertThrows(ErrorReadingSecretException.class, action);

        assertThat(appender.getMessages(), containsString(ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER));
    }

    @Test
    public void fetchSecretLogsErrorWhenWrongSecretKeyIsGiven() {
        final TestAppender appender = LogUtils.getTestingAppender(SecretsReader.class);
        Executable action = () -> secretsReader.fetchSecret(SECRET_NAME, WRONG_SECRET_KEY);

        assertThrows(ErrorReadingSecretException.class, action);

        assertThat(appender.getMessages(), containsString(SecretsReader.COULD_NOT_READ_SECRET_ERROR));
    }

    @Test
    public void fetchSecretLogsErrorLogsErrorCauseButMasksErrorCauseToCaller() {
        Executable action = () -> secretsReader.fetchSecret(SECRET_NAME, WRONG_SECRET_KEY);
        ErrorReadingSecretException exception = assertThrows(ErrorReadingSecretException.class, action);

        assertThat(exception.getMessage(), not(containsString(SECRET_NAME)));
        assertThat(exception.getMessage(), not(containsString(SECRET_KEY)));
    }

    @Test
    public void fetchSecretReturnsSecretValueWhenSecretNameAndSecretKeyAreCorrect() throws ErrorReadingSecretException {
        String value = secretsReader.fetchSecret(SECRET_NAME, SECRET_KEY);
        assertThat(value, is(equalTo(SECRET_VALUE)));
    }

    @Test
    public void fetchPlainTextSecretWhenSecretNameIsCorrect() {
        String value = secretsReader.fetchPlainTextSecret(PLAIN_TEXT_SECRET_NAME);
        assertThat(value, is(equalTo(PLAIN_TEXT_SECRET_VALUE)));
    }

    private SecretsReader createSecretsReaderMock() {
        var secretsManager = mock(SecretsManagerClient.class);
        when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
            .thenAnswer(this::provideGetSecretValueResult);
        return new SecretsReader(secretsManager);
    }

    private GetSecretValueResponse provideGetSecretValueResult(InvocationOnMock invocation)
        throws JsonProcessingException {
        String providedSecretName = getSecretNameFromRequest(invocation);
        if (providedSecretName.equals(SECRET_NAME)) {
            String secretString = createSecretJsonObject();
            return createGetSecretValueResult(secretString);
        } else if (providedSecretName.equals(PLAIN_TEXT_SECRET_NAME)) {
            return createGetSecretValueResult(PLAIN_TEXT_SECRET_VALUE);
        } else {
            throw new RuntimeException(ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER);
        }
    }

    private String getSecretNameFromRequest(InvocationOnMock invocation) {
        GetSecretValueRequest request = invocation.getArgument(0);
        return request.secretId();
    }

    private GetSecretValueResponse createGetSecretValueResult(String secretString) {
        return GetSecretValueResponse.builder()
                   .secretString(secretString)
                   .name(SECRET_NAME)
                   .build();
    }

    private String createSecretJsonObject() throws JsonProcessingException {
        Map<String, String> secret = Map.of(SECRET_KEY, SECRET_VALUE);
        return dtoObjectMapper.writeValueAsString(secret);
    }
}