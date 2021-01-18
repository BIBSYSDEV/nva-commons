package no.unit.nva.events.examples;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DestinationsEventConsumer
    extends DestinationsEventBridgeEventHandler<DataciteDoiRequest, DataciteDoiRequest> {

    private static final Logger logger = LoggerFactory.getLogger(DestinationsEventConsumer.class);

    @JacocoGenerated
    public DestinationsEventConsumer() {
        super(DataciteDoiRequest.class);
    }

    @JacocoGenerated
    @Override
    protected DataciteDoiRequest processInputPayload(
        DataciteDoiRequest input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<DataciteDoiRequest>> event,
        Context context) {

        logger.info("The input");
        logger.info(input.toString());
        logger.info("The event");
        logger.info(event.toString());
        return input;
    }
}
