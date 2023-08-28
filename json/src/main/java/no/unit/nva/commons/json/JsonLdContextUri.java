package no.unit.nva.commons.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public class JsonLdContextUri implements JsonLdContext {

    @JsonIgnore
    private String type;

    @JsonProperty("@context")
    private final URI context;

    @JsonCreator
    public JsonLdContextUri(URI context) {
        this.context = context;
    }

    public URI getContext() {
        return context;
    }
}
