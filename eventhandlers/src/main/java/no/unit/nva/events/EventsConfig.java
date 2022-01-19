package no.unit.nva.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class EventsConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private EventsConfig() {
    }
}
