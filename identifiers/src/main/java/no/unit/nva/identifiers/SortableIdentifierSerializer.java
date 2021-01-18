package no.unit.nva.identifiers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;

public class SortableIdentifierSerializer extends JsonSerializer<SortableIdentifier> {

    public static final String NULL_AS_STRING = "null";
    public static final String SERIALIZATION_EXCEPTION_ERROR = "Could not serialize SortableIdentifier with value: ";

    @JacocoGenerated
    public SortableIdentifierSerializer() {
        super();
    }

    @Override
    public void serialize(SortableIdentifier value, JsonGenerator gen, SerializerProvider serializers) {
        try {
            if (Objects.nonNull(value)) {
                gen.writeString(value.toString());
            } else {
                gen.writeNull();
            }
        } catch (Exception e) {
            throw new RuntimeException(SERIALIZATION_EXCEPTION_ERROR + printIdentifierValue(value), e);
        }
    }

    private String printIdentifierValue(SortableIdentifier value) {
        return Optional.ofNullable(value).map(SortableIdentifier::toString).orElse(NULL_AS_STRING);
    }
}
