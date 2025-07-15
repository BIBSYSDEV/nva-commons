package no.unit.nva.commons.json.ld;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Record to allow representation of a JSON-LD inline context (see example).
 *
 * <pre>
 *     {
 *         "@vocab" : "https://example.org/vocab",
 *         "id" : "@id",
 *         "type" : "@type"
 *     }
 * </pre>
 * @param context The inline JSON-LD context object.
 */
public record JsonLdInlineContext(@JsonValue JsonNode context) implements JsonLdContext {

}
