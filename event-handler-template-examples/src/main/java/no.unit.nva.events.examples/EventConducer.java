package no.unit.nva.events.examples;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an event Consumer and Producer. It consumes an event that was not created by Lambda destinations and it
 * produces an event through Lambda Destinations.
 *
 */
public class EventConducer extends EventHandler<DataciteDoiRequest, DataciteDoiRequest> {

    private final Logger logger = LoggerFactory.getLogger(EventConducer.class);

    @JacocoGenerated
    public EventConducer() {
        super(DataciteDoiRequest.class);
    }

    @Override
    @JacocoGenerated
    protected DataciteDoiRequest processInput(DataciteDoiRequest input,
                                              AwsEventBridgeEvent<DataciteDoiRequest> event,
                                              Context context) {
        logger.info("Lambda input");
        logger.info(input.toString());
        logger.info("Event");
        logger.info(event.toString());
        if (input.getSomeData().contains("failure")) {
            throw new RuntimeException("Hard coded exception");
        } else {
            return input;
        }
    }
}
