package nva.commons.secrets;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
     * @param secretName   the user-friendly id of the secret or the secret ARN
     * @param secretKey    the user-friendly Key in the key-value map.
     * @param secretValue  the secretValue that you want to persist in the encrypted key-value map.
     * @return PutSecretValueResponse
     * @throws ErrorReadingSecretException when any error occurs.
     */
    public PutSecretValueResponse updateSecretKey(String secretName, String secretKey, String secretValue) {
        var valueResponse =
            awsSecretsManager.getSecretValue(
                GetSecretValueRequest.builder().secretId(secretName).build()
            );

        return attempt(() -> upsertValueResponse(valueResponse, secretKey, secretValue))
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
    public <T> PutSecretValueResponse updateSecretObject(String secretName, T secretClassInstance) {

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

    private PutSecretValueResponse upsertValueResponse(
        GetSecretValueResponse secretValueResponse,
        String secretKey,
        String secretValue) {

        return Try.of(secretValueResponse)
                   .map(GetSecretValueResponse::secretString)
                   .map(objectMapper::readTree)
                   .map(secretJson -> {

                       ((ObjectNode) secretJson).put(secretKey, secretValue);
                       return updateSecretJsonToAws(secretValueResponse.name(), secretJson);

                   }).orElseThrow(this::logErrorAndThrowException);
    }

    private <T> PutSecretValueResponse updateSecretJsonToAws(String secretVaultId, T node) {
        return awsSecretsManager.putSecretValue(
            PutSecretValueRequest.builder()
                .secretId(secretVaultId)
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