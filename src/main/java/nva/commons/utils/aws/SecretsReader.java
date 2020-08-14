package nva.commons.utils.aws;

import static nva.commons.utils.attempt.Try.attempt;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.exceptions.ErrorReadingSecretException;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.attempt.Failure;
import nva.commons.utils.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretsReader {

    private static final Logger logger = LoggerFactory.getLogger(SecretsReader.class);
    public static final String COULD_NOT_READ_SECRET_ERROR = "Could not read secret: ";

    private final AWSSecretsManager awsSecretsManager;

    @JacocoGenerated
    public SecretsReader() {
        this(AWSSecretsManagerClientBuilder.defaultClient());
    }

    public SecretsReader(AWSSecretsManager awsSecretsManager) {
        this.awsSecretsManager = awsSecretsManager;
    }

    /**
     * Fetches a secret String from AWS Secrets Manager.
     *
     * @param secretName the user-friendly id of the secret or the secret ARN
     * @param secretKey  the key in the encrypted key-value map.
     * @return the value for the specified key
     * @throws ForbiddenException when any error occurs.
     */
    public String fetchSecret(String secretName, String secretKey) throws ForbiddenException {

        return attempt(() -> fetchSecretFromAws(secretName))
            .map(fetchResult -> extractApiKey(fetchResult, secretKey, secretName))
            .orElseThrow(this::logErrorAndThrowException);
    }

    private GetSecretValueResult fetchSecretFromAws(String secretName) {
        return awsSecretsManager
            .getSecretValue(new GetSecretValueRequest().withSecretId(secretName));
    }

    private String extractApiKey(GetSecretValueResult getSecretResult, String secretKey, String secretName)
        throws ErrorReadingSecretException {

        return Try.of(getSecretResult)
            .map(GetSecretValueResult::getSecretString)
            .flatMap(this::readStringAsJsonObject)
            .map(secretJson -> secretJson.get(secretKey))
            .map(JsonNode::textValue)
            .orElseThrow((Failure<String> fail) -> errorReadingSecret(fail, secretName));
    }

    private ErrorReadingSecretException errorReadingSecret(Failure<String> fail, String secretName) {
        logger.error(errorReadingSecretMessage(secretName), fail.getException());
        return new ErrorReadingSecretException();
    }

    public String errorReadingSecretMessage(String secretName) {
        return COULD_NOT_READ_SECRET_ERROR + secretName;
    }

    private Try<JsonNode> readStringAsJsonObject(String secretString) {
        return attempt(() -> JsonUtils.objectMapper.readTree(secretString));
    }

    private <I> ForbiddenException logErrorAndThrowException(Failure<I> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new ForbiddenException();
    }
}
