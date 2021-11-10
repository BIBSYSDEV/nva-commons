package no.unit.nva.stubs;

import java.util.ArrayList;
import java.util.List;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class FakeEventBridgeClient implements EventBridgeClient {

    private final List<PutEventsRequestEntry> requestEntries = new ArrayList<>();

    @JacocoGenerated
    public List<PutEventsRequestEntry> getRequestEntries() {
        return requestEntries;
    }

    @Override
    public PutEventsResponse putEvents(PutEventsRequest putEventsRequest) {
        requestEntries.addAll(putEventsRequest.entries());
        return PutEventsResponse.builder().failedEntryCount(0).build();
    }

    @JacocoGenerated
    @Override
    public String serviceName() {
        return "FakeEventBridgeClient";
    }

    @JacocoGenerated
    @Override
    public void close() {
    }
}