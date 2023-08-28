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
        var node = jp.getCodec().readTree(jp);
        return jp.isExpectedStartObjectToken()
                   ? new JsonLdContextUri(jp.readValueAs(URI.class))
                   : new JsonLdInlineContext((JsonNode) node);
    }
}
