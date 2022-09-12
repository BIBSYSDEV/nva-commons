package nva.commons.secrets;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SecretsReader {

    public static final String COULD_NOT_READ_SECRET_ERROR = "Could not read secret: ";
    private static final Logger logger = LoggerFactory.getLogger(SecretsReader.class);
    private static final String AWS_REGION = new Environment().readEnvOpt("AWS_REGION")
        .orElse(Region.EU_WEST_1.id());

    private final SecretsManagerClient awsSecretsManager;

    @JacocoGenerated
    public SecretsReader() {
        this(defaultSecretsManagerClient());
    }

    public SecretsReader(SecretsManagerClient awsSecretsManager) {
        this.awsSecretsManager = awsSecretsManager;
    }

    /**
     * Fetches a secret String from AWS Secrets Manager.
     *
     * @param secretName the user-friendly id of the secret or the secret ARN
     * @param secretKey  the key in the encrypted key-value map.
     * @return the value for the specified key
     * @throws ErrorReadingSecretException when any error occurs.
     */
    public String fetchSecret(String secretName, String secretKey) {

        return attempt(() -> fetchSecretFromAws(secretName))
            .map(fetchResult -> extractApiKey(fetchResult, secretKey, secretName))
            .orElseThrow(this::logErrorAndThrowException);
    }

    /**
     * Fetches a plain-text as json secret from AWS Secrets Manager.
     *
     * @param secretName the user-friendly id of the secret or the secret ARN
     * @param clazz  the clazz to deserialize json to.
     * @param <T> the parameterized type
     * @return the value for the specified key
     * @throws ErrorReadingSecretException when any error occurs.
     */
    public <T> T fetchPlainTextJsonSecret(String secretName, Class<T> clazz) {

        return attempt(() -> fetchSecretFromAws(secretName))
                   .map(GetSecretValueResponse::secretString)
                   .flatMap(secretString -> deserializeJson(secretString, clazz))
                   .orElseThrow(this::logErrorAndThrowException);
    }

    public String errorReadingSecretMessage(String secretName) {
        return COULD_NOT_READ_SECRET_ERROR + secretName;
    }

    @JacocoGenerated
    public static SecretsManagerClient defaultSecretsManagerClient() {
        return SecretsManagerClient.builder()
            .region(Region.of(AWS_REGION))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.create())
            .build();
    }

    private GetSecretValueResponse fetchSecretFromAws(String secretName) {
        return awsSecretsManager
            .getSecretValue(GetSecretValueRequest.builder().secretId(secretName).build());
    }

    private String extractApiKey(GetSecretValueResponse getSecretResult, String secretKey, String secretName) {

        return Try.of(getSecretResult)
            .map(GetSecretValueResponse::secretString)
            .flatMap(this::readStringAsJsonObject)
            .map(secretJson -> secretJson.get(secretKey))
            .map(JsonNode::textValue)
            .orElseThrow((Failure<String> fail) -> errorReadingSecret(fail, secretName));
    }

    private ErrorReadingSecretException errorReadingSecret(Failure<String> fail, String secretName) {
        logger.error(errorReadingSecretMessage(secretName), fail.getException());
        return new ErrorReadingSecretException();
    }

    private Try<JsonNode> readStringAsJsonObject(String secretString) {
        return attempt(() -> dtoObjectMapper.readTree(secretString));
    }

    private <T> Try<T> deserializeJson(String secretString, Class<T> clazz) {
        return attempt(() -> dtoObjectMapper.readValue(secretString, clazz));
    }

    private <I> ErrorReadingSecretException logErrorAndThrowException(Failure<I> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new ErrorReadingSecretException();
    }
}
