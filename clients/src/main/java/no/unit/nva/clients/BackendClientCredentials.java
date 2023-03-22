package no.unit.nva.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

public class BackendClientCredentials {

    @JsonProperty("backendClientId")
    public String id;

    @JsonProperty("backendClientSecret")
    public String secret;

    @JacocoGenerated
    public BackendClientCredentials() {
    }

    public BackendClientCredentials(String id, String secret) {
        this.id = id;
        this.secret = secret;
    }

    public String getId() {
        return id;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public String toString() {
        return attempt(() -> dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}