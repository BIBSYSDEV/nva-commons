package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import java.time.Instant;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.EventBus;
import software.amazon.awssdk.services.eventbridge.model.ListEventBusesRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

class FakeEventBridgeClientTest {

    @Test
    void shouldStoreEventsInternally() {
        FakeEventBridgeClient fakeEventBridgeClient = new FakeEventBridgeClient();
        PutEventsRequestEntry entry = sampleRequestEntry();
        PutEventsRequest request = PutEventsRequest.builder()
                                       .entries(entry).build();
        fakeEventBridgeClient.putEvents(request);
        assertThat(fakeEventBridgeClient.getRequestEntries(), contains(entry));
    }

    @Test
    void shouldListSuppliedEventBuses() {
        var busNames = new String[]{randomString(), randomString()};
        var client = new FakeEventBridgeClient(busNames);
        var actualBusNames = client.listEventBuses(ListEventBusesRequest.builder().build())
                                 .eventBuses()
                                 .stream()
                                 .map(EventBus::name)
                                 .collect(Collectors.toList());
        assertThat(actualBusNames, containsInAnyOrder(busNames));
    }

    @Test
    void shouldReturnNonZeroNumberOfFailuresWhenNonZeroNumberOfFailuresIsSupplied() {
        var busNames = new String[]{randomString(), randomString()};
        int failures = 5;
        var client = new FakeEventBridgeClient(failures, busNames);
        var putEventsRequest = PutEventsRequest.builder()
                                   .entries(randomEvent(), randomEvent())
                                   .build();
        var putEventsResponse = client.putEvents(putEventsRequest);
        assertThat(putEventsResponse.failedEntryCount(), is(equalTo(failures)));
    }

    private PutEventsRequestEntry randomEvent() {
        return PutEventsRequestEntry.builder()
                   .detail(randomString())
                   .detailType(randomString())
                   .eventBusName(randomString())
                   .resources(randomString())
                   .time(randomInstant())
                   .build();
    }

    private PutEventsRequestEntry sampleRequestEntry() {
        return PutEventsRequestEntry.builder()
                   .resources(randomString())
                   .detail(randomString())
                   .detailType(randomString())
                   .time(Instant.now())
                   .eventBusName(randomString())
                   .source(randomString())
                   .build();
    }
}
