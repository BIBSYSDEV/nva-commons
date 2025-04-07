package no.unit.nva.stubs;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class FakeSecretsManagerClient implements SecretsManagerClient {

    private final Map<SecretName, String> plainTextSecrets = new ConcurrentHashMap<>();
    public Map<SecretName, Map<SecretKey, String>> secrets = new ConcurrentHashMap<>();

    public FakeSecretsManagerClient putSecret(String name, String key, String value) {
        var secretName = new SecretName(name);
        if (plainTextSecrets.containsKey(secretName)) {
            throw new IllegalArgumentException(
                String.format("Secret already present as a plain text secret: %s", name));
        }

        if (secrets.containsKey(secretName)) {
            addSecretValueToExistingSecret(key, value, secretName);
        } else {
            createNewSecret(key, value, secretName);
        }
        return this;
    }

    public FakeSecretsManagerClient putPlainTextSecret(String name, String value) {
        var secretName = new SecretName(name);
        if (secrets.containsKey(secretName)) {
            throw new IllegalArgumentException(String.format("Secret already present as a key/value secret: %s", name));
        }

        plainTextSecrets.put(secretName, value);
        return this;
    }

    @Override
    public GetSecretValueResponse getSecretValue(GetSecretValueRequest getSecretValueRequest) {
        return Optional.ofNullable(getSecretValueRequest.secretId())
                   .map(SecretName::new)
                   .flatMap(this::resolveSecret)
                   .map(secretContents -> addSecretContents(secretContents, getSecretValueRequest))
                   .orElseThrow();
    }

    @JacocoGenerated
    @Override
    public String serviceName() {
        return null;
    }

    @JacocoGenerated
    @Override
    public void close() {

    }

    private static GetSecretValueResponse addSecretContents(String secretContents,
                                                            GetSecretValueRequest getSecretValueRequest) {
        return GetSecretValueResponse.builder()
                   .secretString(secretContents)
                   .name(getSecretValueRequest.secretId())
                   .build();
    }

    private static String serializeSecretContents(Map<SecretKey, String> secretContents) {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(secretContents)).orElseThrow();
    }

    private Optional<String> resolveSecret(SecretName secretName) {
        if (secrets.containsKey(secretName)) {
            return Optional.of(serializeSecretContents(secrets.get(secretName)));
        } else if (plainTextSecrets.containsKey(secretName)) {
            return Optional.of(plainTextSecrets.get(secretName));
        } else {
            return Optional.empty();
        }
    }

    private void createNewSecret(String key, String value, SecretName secretName) {
        var secretContents = new ConcurrentHashMap<SecretKey, String>();
        secretContents.put(new SecretKey(key), value);
        secrets.put(secretName, secretContents);
    }

    private void addSecretValueToExistingSecret(String key, String value, SecretName secretName) {
        secrets.get(secretName).put(new SecretKey(key), value);
    }

    private static final class SecretName {

        private final String value;

        @JsonCreator
        private SecretName(String value) {
            this.value = value;
        }

        @JacocoGenerated
        @JsonValue
        public String getValue() {
            return value;
        }

        @JacocoGenerated
        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @JacocoGenerated
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SecretName)) {
                return false;
            }

            SecretName that = (SecretName) o;

            return Objects.equals(value, that.value);
        }
    }

    private static class SecretKey {

        private final String value;

        @JsonCreator
        public SecretKey(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @JacocoGenerated
        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @JacocoGenerated
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SecretKey)) {
                return false;
            }

            SecretKey secretKey = (SecretKey) o;

            return Objects.equals(value, secretKey.value);
        }
    }
}
