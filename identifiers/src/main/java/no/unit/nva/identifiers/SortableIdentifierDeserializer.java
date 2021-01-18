package no.unit.nva.identifiers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import nva.commons.core.JacocoGenerated;

public class SortableIdentifierDeserializer extends JsonDeserializer<SortableIdentifier> {

    @JacocoGenerated
    public SortableIdentifierDeserializer() {
        super();
    }

    @JacocoGenerated
    @Override
    public SortableIdentifier deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
        String value = p.getValueAsString();
        return new SortableIdentifier(value);
    }
}
