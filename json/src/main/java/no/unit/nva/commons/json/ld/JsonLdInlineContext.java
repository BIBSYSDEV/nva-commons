package no.unit.nva.commons.json.ld;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

public record JsonLdInlineContext(@JsonValue JsonNode context) implements JsonLdContext {

}
