package no.unit.nva.events.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.events.EventsConfig;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;

public abstract class DestinationsEventBridgeEventHandler<InputType, OutputType>
    extends EventHandler<AwsEventBridgeDetail<InputType>, OutputType> {

    private final Class<InputType> iclass;

    protected DestinationsEventBridgeEventHandler(Class<InputType> iclass) {
        super(AwsEventBridgeDetail.class, EventsConfig.objectMapper);
        this.iclass = iclass;
    }

    protected DestinationsEventBridgeEventHandler(Class<InputType> iclass, ObjectMapper objectMapper) {
        super(AwsEventBridgeDetail.class, objectMapper);
        this.iclass = iclass;
    }

    @Override
    protected final OutputType processInput(AwsEventBridgeDetail<InputType> input,
                                            AwsEventBridgeEvent<AwsEventBridgeDetail<InputType>> event,
                                            Context context) {
        return processInputPayload(input.getResponsePayload(), event, context);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected AwsEventBridgeEvent<AwsEventBridgeDetail<InputType>> parseEvent(String input) {
        return new EventParser<AwsEventBridgeDetail<InputType>>(input, objectMapper)
            .parse(AwsEventBridgeDetail.class, iclass);
    }

    protected abstract OutputType processInputPayload(InputType input,
                                                      AwsEventBridgeEvent<AwsEventBridgeDetail<InputType>> event,
                                                      Context context);
}
