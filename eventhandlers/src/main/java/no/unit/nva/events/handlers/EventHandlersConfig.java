package no.unit.nva.events.handlers;

import static nva.commons.core.JsonUtils.dynamoObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class EventHandlersConfig {

    //Events are considered internal operations. Therefore, the default objectMapper is the one that saves
    // the most space. If other mapper is necessary, it can be set accordingly.
    /* default */ static final ObjectMapper defaultEventObjectMapper = dynamoObjectMapper;

    private EventHandlersConfig() {
    }
}
