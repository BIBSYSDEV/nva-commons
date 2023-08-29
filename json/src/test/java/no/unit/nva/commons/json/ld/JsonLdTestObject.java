package no.unit.nva.commons.json.ld;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class JsonLdTestObject {

    @JsonProperty("@context")
    private JsonLdContext context;

    @JsonCreator
    public JsonLdTestObject(JsonLdContext context) {
        this.context = context;
    }

    public JsonLdContext getContext() {
        return context;
    }
}
