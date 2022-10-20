package no.unit.nva.stubs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBus;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class FakeEventBridgeClient implements EventBridgeClient {
    
    private final List<PutEventsRequestEntry> requestEntries = new ArrayList<>();
    private final List<String> eventBusNames;
    private final Integer mockFailedEntryCount;
    
    @JacocoGenerated
    public FakeEventBridgeClient(String... eventBusNames) {
        this.eventBusNames = Arrays.asList(eventBusNames);
        this.mockFailedEntryCount = 0;
    }
    
    @JacocoGenerated
    public FakeEventBridgeClient(int failCount, String... eventBusNames) {
        this.eventBusNames = Arrays.asList(eventBusNames);
        this.mockFailedEntryCount = failCount;
    }
    
    @JacocoGenerated
    public List<PutEventsRequestEntry> getRequestEntries() {
        return requestEntries;
    }
    
    @Override
    public ListEventBusesResponse listEventBuses(ListEventBusesRequest listEventBusesRequest) {
        var buses = eventBusNames.stream()
                        .map(busName -> EventBus.builder().name(busName).build())
                        .collect(Collectors.toList());
        return ListEventBusesResponse.builder().eventBuses(buses).build();
    }
    
    @JacocoGenerated
    @Override
    public PutEventsResponse putEvents(PutEventsRequest putEventsRequest) {
        requestEntries.addAll(putEventsRequest.entries());
        return PutEventsResponse.builder().failedEntryCount(mockFailedEntryCount).build();
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