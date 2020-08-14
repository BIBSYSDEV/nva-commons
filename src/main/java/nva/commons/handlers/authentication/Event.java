package nva.commons.handlers.authentication;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;
import nva.commons.utils.JacocoGenerated;

public class Event {

    @JsonAnySetter
    public Map<String, Object> properties;

    @JacocoGenerated
    public Event() {
        this.properties = new LinkedHashMap<>();
    }

    @JsonAnyGetter
    @JacocoGenerated
    public Map<String, Object> getProperties() {
        return properties;
    }

    @JacocoGenerated
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
