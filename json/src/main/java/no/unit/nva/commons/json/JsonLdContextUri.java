package no.unit.nva.commons.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;

public class JsonLdContextUri implements JsonLdContext {

    private final URI context;

    @JsonCreator
    public JsonLdContextUri(URI context) {
        this.context = context;
    }

    @JsonValue
    public URI getContext() {
        return context;
    }
}
