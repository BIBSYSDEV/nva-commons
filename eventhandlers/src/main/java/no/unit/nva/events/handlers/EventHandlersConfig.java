package no.unit.nva.events.handlers;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class EventHandlersConfig {

    /* default */ static final ObjectMapper eventObjectMapper = dtoObjectMapper;

    private EventHandlersConfig() {
    }
}
