package no.unit.nva.commons.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.net.URI;

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
