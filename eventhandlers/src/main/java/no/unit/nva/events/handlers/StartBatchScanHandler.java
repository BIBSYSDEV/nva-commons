package no.unit.nva.events.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import no.unit.nva.events.models.ScanDatabaseRequest;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public abstract class StartBatchScanHandler implements RequestStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(StartBatchScanHandler.class);
    private static final String EVENT_BUS = new Environment().readEnv("EVENT_BUS");
    private static final String SCAN_REQUEST_EVENTS_DETAIL_TYPE = "topicInDetailType";
    private final EventBridgeClient eventClient;

    protected StartBatchScanHandler(EventBridgeClient eventClient) {
        this.eventClient = eventClient;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        var requestSentByUser = parseUserInput(input);
        var requestWithTopic = createEventAsExpectedByEventListener(requestSentByUser);
        emitEvent(context, requestWithTopic);
        logger.info("Emitted request {}", requestWithTopic.toJsonString());
    }

    protected abstract String getScanEventTopic();

    private ScanDatabaseRequest parseUserInput(InputStream input) throws IOException {
        var json = IoUtils.streamToString(input);
        return ScanDatabaseRequest.fromJson(json);
    }

    private ScanDatabaseRequest createEventAsExpectedByEventListener(ScanDatabaseRequest input) {
        return new ScanDatabaseRequest(getScanEventTopic(),
                                       input.getPageSize(),
                                       input.getStartMarker());
    }

    private void emitEvent(Context context, ScanDatabaseRequest requestWithTopic) {
        eventClient.putEvents(createEvent(context, requestWithTopic));
    }

    private PutEventsRequest createEvent(Context context, ScanDatabaseRequest request) {
        return PutEventsRequest.builder().entries(createNewEventEntry(context, request)).build();
    }

    private PutEventsRequestEntry createNewEventEntry(Context context, ScanDatabaseRequest request) {
        return request.createNewEventEntry(EVENT_BUS,
                                           SCAN_REQUEST_EVENTS_DETAIL_TYPE,
                                           context.getInvokedFunctionArn());
    }
}

