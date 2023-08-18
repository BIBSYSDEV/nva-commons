package no.unit.nva.events.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.events.EventsConfig;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;

public abstract class DestinationsEventBridgeEventHandler<I, O>
    extends EventHandler<AwsEventBridgeDetail<I>, O> {

    private final Class<I> iclass;

    protected DestinationsEventBridgeEventHandler(Class<I> iclass) {
        super(AwsEventBridgeDetail.class, EventsConfig.objectMapper);
        this.iclass = iclass;
    }

    protected DestinationsEventBridgeEventHandler(Class<I> iclass, ObjectMapper objectMapper) {
        super(AwsEventBridgeDetail.class, objectMapper);
        this.iclass = iclass;
    }

    @Override
    protected final O processInput(AwsEventBridgeDetail<I> input,
                                   AwsEventBridgeEvent<AwsEventBridgeDetail<I>> event,
                                   Context context) {
        return processInputPayload(input.getResponsePayload(), event, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected AwsEventBridgeEvent<AwsEventBridgeDetail<I>> parseEvent(String input) {
        return new EventParser<AwsEventBridgeDetail<I>>(input, objectMapper)
            .parse(AwsEventBridgeDetail.class, iclass);
    }

    protected abstract O processInputPayload(I input,
                                             AwsEventBridgeEvent<AwsEventBridgeDetail<I>> event,
                                             Context context);
}
