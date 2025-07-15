package no.unit.nva.commons.json.ld.deserialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.URI;
import no.unit.nva.commons.json.ld.JsonLdContext;
import no.unit.nva.commons.json.ld.JsonLdContextUri;
import no.unit.nva.commons.json.ld.JsonLdInlineContext;

/**
 * This deserializer allows the deserialization of JSON-LD contexts, which occur
 * either as an inline (an object) or remote context (a string, which is a URI).
 * This is necessary because the context object cannot contain information that
 * enables Jackson to correctly distinguish the two cases.
 */
public class JsonLdContextDeserializer extends StdDeserializer<JsonLdContext> {

    public JsonLdContextDeserializer() {
        this(null);
    }

    public JsonLdContextDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public JsonLdContext deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        var node = (JsonNode) jp.getCodec().readTree(jp).get("@context");
        return node.isObject()
                   ? new JsonLdInlineContext(node)
                   : new JsonLdContextUri(URI.create(node.textValue()));
    }
}
