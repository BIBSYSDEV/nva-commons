package nva.commons.utils.aws;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;

public class SecretsReaderTest {

    public static final String SECRET_VALUE = "SECRET_VALUE";
    public static final String SECRET_KEY = "SECRET_KEY";
    public static final String SECRET_NAME = "SECRET_NAME";
    public static final String WRONG_SECRET_NAME = "WRONG_SECRET_NAME";
    public static final String WRONG_SECRET_KEY = "WRONG_KEY";
    public static final String ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER = "Secret not found";
    private final SecretsReader secretsReader;

    public SecretsReaderTest() throws JsonProcessingException {
        secretsReader = createSecretsReaderMock();
    }

    @Test
    public void fetchSecretLogsErrorWhenWrongSecretNameIsGiven() {
        final TestAppender appender = LogUtils.getTestingAppender(SecretsReader.class);
        Executable action = () -> secretsReader.fetchSecret(WRONG_SECRET_NAME, SECRET_KEY);
        ForbiddenException exception = assertThrows(ForbiddenException.class, action);

        assertThat(appender.getMessages(), containsString(ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER));
    }

    @Test
    public void fetchSecretLogsErrorWhenWrongSecretKeyIsGiven() {
        final TestAppender appender = LogUtils.getTestingAppender(SecretsReader.class);
        Executable action = () -> secretsReader.fetchSecret(SECRET_NAME, WRONG_SECRET_KEY);
        ForbiddenException exception = assertThrows(ForbiddenException.class, action);

        assertThat(appender.getMessages(), containsString(SecretsReader.COULD_NOT_READ_SECRET_ERROR));
    }

    @Test
    public void fetchSecretLogsErrorLogsErrorCauseButMasksErrorCauseToCaller() {
        Executable action = () -> secretsReader.fetchSecret(SECRET_NAME, WRONG_SECRET_KEY);
        ForbiddenException exception = assertThrows(ForbiddenException.class, action);

        assertThat(exception.getMessage(), is(equalTo(ForbiddenException.DEFAULT_MESSAGE)));
    }

    @Test
    public void fetchSecretReturnsSecretValueWhenSecretNameAndSecretKeyAreCorrect() throws ForbiddenException {
        String value = secretsReader.fetchSecret(SECRET_NAME, SECRET_KEY);
        assertThat(value, is(equalTo(SECRET_VALUE)));
    }

    private SecretsReader createSecretsReaderMock() {
        AWSSecretsManager secretsManager = mock(AWSSecretsManager.class);
        when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
            .thenAnswer(this::provideGetSecretValueResult);
        return new SecretsReader(secretsManager);
    }

    private GetSecretValueResult provideGetSecretValueResult(InvocationOnMock invocation)
        throws JsonProcessingException {
        String providedSecretName = getSecretNameFromRequest(invocation);
        if (providedSecretName.equals(SECRET_NAME)) {
            String secretString = createSecretJsonObject();
            return provideGetSecretValueResult(secretString);
        } else {
            throw new ResourceNotFoundException(ERROR_MESSAGE_FROM_AWS_SECRET_MANAGER);
        }
    }

    private String getSecretNameFromRequest(InvocationOnMock invocation) {
        GetSecretValueRequest request = invocation.getArgument(0);
        return request.getSecretId();
    }

    private GetSecretValueResult provideGetSecretValueResult(String secretString) {
        return new GetSecretValueResult()
            .withSecretString(secretString)
            .withName(SECRET_NAME);
    }

    private String createSecretJsonObject() throws JsonProcessingException {
        Map<String, String> secret = Map.of(SECRET_KEY, SECRET_VALUE);
        return JsonUtils.objectMapper.writeValueAsString(secret);
    }
}