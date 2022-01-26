package no.unit.nva.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class EventsConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private EventsConfig() {
    }
}
