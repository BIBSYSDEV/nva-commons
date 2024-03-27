package no.unit.nva.events.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.events.EventsConfig;

/*
  Implemented as RequestStreamHandler because RequestHandler has problem with java.time.Instant class.
  Probably the class RequestHandler does not include the java-8-module.
 */

/**
 * Handler for handling EventBridge Events.
 * @param <I> the input type.
 * @param <O> the output type.
 *
 * Deprecated: Use the {@link com.github.awsjavakit.eventbridge.handlers.EventHandler} instead.
 */
@Deprecated
public abstract class EventHandler<I, O> extends com.github.awsjavakit.eventbridge.handlers.EventHandler<I, O> {


    protected EventHandler(Class iclass, ObjectMapper objectMapper) {
        super(iclass, objectMapper);
    }

    protected EventHandler(Class iclass) {
        this(iclass, EventsConfig.objectMapper);
    }

}
