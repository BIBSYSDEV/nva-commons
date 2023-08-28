package no.unit.nva.commons.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonLdInlineContext implements JsonLdContext {

    @JsonProperty("@context")
    private final JsonNode context;

    @JsonCreator
    public JsonLdInlineContext(JsonNode context) {
        this.context = context;
    }

    public JsonNode getContext() {
        return context;
    }
}
