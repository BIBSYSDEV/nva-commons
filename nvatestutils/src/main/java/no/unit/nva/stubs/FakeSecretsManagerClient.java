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

    public Map<SecretName, Map<SecretKey, String>> secrets = new ConcurrentHashMap<>();

    public FakeSecretsManagerClient putSecret(String name, String key, String value) {
        var secretName = new SecretName(name);
        if (secrets.containsKey(secretName)) {
            addSecretValueToExistingSecret(key, value, secretName);
        } else {
            createNewSecret(key, value, secretName);
        }
        return this;
    }

    @Override
    public GetSecretValueResponse getSecretValue(GetSecretValueRequest getSecretValueRequest) {
        return Optional.ofNullable(getSecretValueRequest.secretId())
                   .map(SecretName::new)
                   .map(secretName -> secrets.get(secretName))
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

    private static GetSecretValueResponse addSecretContents(Map<SecretKey, String> secretContents,
                                                            GetSecretValueRequest getSecretValueRequest) {
        return GetSecretValueResponse.builder()
                   .secretString(serializeSecretContents(secretContents))
                   .name(getSecretValueRequest.secretId())
                   .build();
    }

    private static String serializeSecretContents(Map<SecretKey, String> secretContents) {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(secretContents)).orElseThrow();
    }

    private void createNewSecret(String key, String value, SecretName secretName) {
        ConcurrentHashMap<SecretKey, String> secretContents = new ConcurrentHashMap<>();
        secretContents.put(new SecretKey(key), value);
        secrets.put(secretName, secretContents);
    }

    private void addSecretValueToExistingSecret(String key, String value, SecretName secretName) {
        secrets.get(secretName).put(new SecretKey(key), value);
    }

    private static class SecretName {

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
            if (!(o instanceof SecretName that)) {
                return false;
            }

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
            if (!(o instanceof SecretKey secretKey)) {
                return false;
            }

            return Objects.equals(value, secretKey.value);
        }
    }
}
