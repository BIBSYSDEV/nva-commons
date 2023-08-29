package no.unit.nva.commons.json.deserialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import no.unit.nva.commons.json.JsonLdContext;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class Thing {

    @JsonProperty("@context")
    @JsonUnwrapped
    private JsonLdContext context;

    @JsonCreator
    public Thing(JsonLdContext context) {
        this.context = context;
    }

    public JsonLdContext getContext() {
        return context;
    }
}
