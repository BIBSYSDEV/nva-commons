package no.unit.nva.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;
import java.net.URI;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

public class GetExternalClientResponse {
    @JsonProperty("clientId")
    public String clientId;

    @JsonProperty("customer")
    public URI customer;

    @JacocoGenerated
    public GetExternalClientResponse() {
    }

    public GetExternalClientResponse(String clientId, URI customer) {
        this.clientId = clientId;
        this.customer = customer;
    }

    public String getClientId() {
        return clientId;
    }

    public URI getCustomer() {
        return customer;
    }

    @Override
    public String toString() {
        return attempt(() -> dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
