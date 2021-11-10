package no.unit.nva.stubs;

import java.util.ArrayList;
import java.util.List;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
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
    public PutEventsResponse putEvents(PutEventsRequest putEventsRequest)
        throws AwsServiceException, SdkClientException {
        requestEntries.addAll(putEventsRequest.entries());
        return PutEventsResponse.builder().failedEntryCount(0).build();
    }

    @Override
    public String serviceName() {
        return "FakeEventBridgeClient";
    }

    @Override
    public void close() {
    }
}