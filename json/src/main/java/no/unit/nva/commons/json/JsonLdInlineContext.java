package no.unit.nva.commons.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.JacocoGenerated;

public class JsonLdInlineContext implements JsonLdContext {

    @JsonValue
    private final JsonNode context;

    @JsonCreator
    public JsonLdInlineContext(JsonNode context) {
        this.context = context;
    }

    @JacocoGenerated
    public JsonNode getContext() {
        return context;
    }
}
