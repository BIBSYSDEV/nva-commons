package nva.commons.secrets;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse;

public class SecretsWriter {

    public static final String COULD_NOT_WRITE_SECRET_ERROR = "Could not write secret: ";
    private static final Logger logger = LoggerFactory.getLogger(SecretsWriter.class);
    private static final String AWS_REGION =
        new Environment().readEnvOpt("AWS_REGION")
            .orElse(Region.EU_WEST_1.id());
    private static final String EMPTY_STRING = "";

    private final SecretsManagerClient awsSecretsManager;

    private final ObjectMapper objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @JacocoGenerated
    public SecretsWriter() {
        this(defaultSecretsManagerClient());
    }

    public SecretsWriter(SecretsManagerClient awsSecretsManager) {
        this.awsSecretsManager = awsSecretsManager;
    }

    /**
     * Updates a secret object (json) in AWS Secrets Manager as tObjectJsonSecretValue.
     *
     * @param secretName      the user-friendly id of the secret or the secret ARN
     * @param jsonSecretValue the secretValue that you want to persist in the encrypted key-value map.
     * @return PutSecretValueResponse
     * @throws ErrorReadingSecretException when any error occurs.
     */
    public PutSecretValueResponse updateSecret(String secretName, String jsonSecretValue) {

        return attempt(() -> updateSecretToAws(secretName, jsonSecretValue))
                   .map(response -> response)
                   .orElseThrow(this::logErrorAndThrowException);
    }

    /**
     * Updates a secret object (json) in AWS Secrets Manager as tObjectJsonSecretValue.
     *
     * @param secretName          the user-friendly id of the secret or the secret ARN
     * @param secretClassInstance the class or interface of the class to be persisted
     * @param <T>                 the type of the class or interface of the class to be persisted
     * @return PutSecretValueResponse
     * @throws ErrorReadingSecretException when any error occurs.
     */
    public <T> PutSecretValueResponse updateClassSecret(String secretName, T secretClassInstance) {

        return attempt(() -> updateSecretJsonToAws(secretName, secretClassInstance))
                   .map(response -> response)
                   .orElseThrow(this::logErrorAndThrowException);
    }

    @JacocoGenerated
    public static SecretsManagerClient defaultSecretsManagerClient() {
        return SecretsManagerClient.builder()
                   .region(Region.of(AWS_REGION))
                   .credentialsProvider(DefaultCredentialsProvider.create())
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    private PutSecretValueResponse updateSecretToAws(String secretName, String value) {
        return awsSecretsManager.putSecretValue(
            PutSecretValueRequest.builder()
                .secretId(secretName)
                .secretString(value)
                .build());
    }

    private <T> PutSecretValueResponse updateSecretJsonToAws(String secretName, T node) {
        return awsSecretsManager.putSecretValue(
            PutSecretValueRequest.builder()
                .secretId(secretName)
                .secretString(toJsonCompact(node))
                .build());
    }

    private DefaultPrettyPrinter getPrettyPrinterCompact() {
        var prettyPrinter = new DefaultPrettyPrinter();
        var indenter = new DefaultIndenter(EMPTY_STRING, EMPTY_STRING);
        prettyPrinter.indentArraysWith(indenter);
        prettyPrinter.indentObjectsWith(indenter);
        return prettyPrinter;
    }

    public <T> String toJsonCompact(T toJsonObject) {
        return
            attempt(() -> objectMapper
                              .writer(getPrettyPrinterCompact())
                              .writeValueAsString(toJsonObject)
            ).orElseThrow();
    }

    private <I> ErrorWritingSecretException logErrorAndThrowException(Failure<I> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new ErrorWritingSecretException();
    }
}