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
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse;

public class SecretsWriter {
    public static final String COULD_NOT_WRITE_SECRET = "Could not write secret: ";
    private static final Logger logger = LoggerFactory.getLogger(SecretsWriter.class);
    private static final String AWS_REGION = new Environment().readEnvOpt("AWS_REGION")
                                                 .orElse(Region.EU_WEST_1.id());

    private final SecretsManagerClient awsSecretsManager;

    @JacocoGenerated
    public SecretsWriter() {
        this(defaultSecretsManagerClient());
    }

    public SecretsWriter(SecretsManagerClient awsSecretsManager) {
        this.awsSecretsManager = awsSecretsManager;
    }

    /**
     * Updates a secret String from AWS Secrets Manager.
     *
     * @param secretName the user-friendly id of the secret or the secret ARN
     * @param secretKey  the key in the encrypted key-value map.
     * @return the value for the specified key
     * @throws ErrorReadingSecretException when any error occurs.
     */
    public String updateSecret(String secretName, String secretKey) {

        return attempt(() -> updateSecretFromAws(secretName,secretKey))
                   .map(response -> response.getValueForField(secretName,String.class).get())
                   .orElseThrow(this::logErrorAndThrowException);
    }


    /**
     * Fetches a json secret from AWS Secrets Manager as a class.
     *
     * @param secretName the user-friendly id of the secret or the secret ARN
     * @param tclass the class or interface of the class to be returned
     * @param <T> the type of the class or interface of the class to be returned
     * @return Class of the object we want to extract the secret to
     * @throws ErrorReadingSecretException when any error occurs.
     */
    public <T> T updateClassSecret(String secretName, Class<T> tclass) {

        return attempt(() -> updateSecretFromAws(secretName, tclass.toString()))
                   .map( response -> response.getValueForField(secretName,tclass).get())
                   .orElseThrow((Failure<T> fail) -> errorWritingSecret(fail, secretName));
    }

    @JacocoGenerated
    public static SecretsManagerClient defaultSecretsManagerClient() {
        return SecretsManagerClient.builder()
                   .region(Region.of(AWS_REGION))
                   .credentialsProvider(DefaultCredentialsProvider.create())
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    private PutSecretValueResponse updateSecretFromAws(String secretName, String value){
        return  awsSecretsManager.putSecretValue(
            PutSecretValueRequest.builder()
                .secretId(secretName)
                .secretString(value)
                .build());
    }

    private <T> ErrorWritingSecretException errorWritingSecret(Failure<T> fail, String secretName) {
        logger.error(errorWritingSecretMessage(secretName), fail.getException());
        return new ErrorWritingSecretException();
    }


    private String errorWritingSecretMessage(String secretName) {
        return COULD_NOT_WRITE_SECRET + secretName;
    }

    private Try<JsonNode> readStringAsJsonObject(String secretString) {
        return attempt(() -> dtoObjectMapper.readTree(secretString));
    }

    private <I> ErrorWritingSecretException logErrorAndThrowException(Failure<I> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new ErrorWritingSecretException();
    }
}
