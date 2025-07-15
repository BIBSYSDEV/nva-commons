package no.unit.nva.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;
import java.net.URI;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

public class GetExternalClientResponse {
    @JsonProperty("clientId")
    public String clientId;

    @JsonProperty("actingUser")
    public String actingUser;

    @JsonProperty("customerUri")
    public URI customerUri;

    @JsonProperty("cristinOrgUri")
    public URI cristinUrgUri;

    @JacocoGenerated
    public GetExternalClientResponse() {
    }

    public GetExternalClientResponse(String clientId, String actingUser, URI customerUri, URI cristinUrgUri) {
        this.clientId = clientId;
        this.actingUser = actingUser;
        this.customerUri = customerUri;
        this.cristinUrgUri = cristinUrgUri;
    }

    public String getClientId() {
        return clientId;
    }

    public String getActingUser() {
        return actingUser;
    }

    public URI getCustomerUri() {
        return customerUri;
    }

    public URI getCristinUrgUri() {
        return cristinUrgUri;
    }

    @Override
    public String toString() {
        return attempt(() -> dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
