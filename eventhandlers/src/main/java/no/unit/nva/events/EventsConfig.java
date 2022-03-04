package no.unit.nva.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import no.unit.nva.commons.json.JsonUtils;

public final class EventsConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    public static final JSON objectMapperLight = JSON.builder().register(JacksonAnnotationExtension.std).build();

    private EventsConfig() {
    }
}
