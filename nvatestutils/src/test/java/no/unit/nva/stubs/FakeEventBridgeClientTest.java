package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class FakeEventBridgeClientTest {

    @Test
    void shouldStoreEventsInternally() {
        FakeEventBridgeClient fakeEventBridgeClient = new FakeEventBridgeClient();
        PutEventsRequestEntry entry = sampleRequestEntry();
        PutEventsRequest request = PutEventsRequest.builder()
            .entries(entry).build();
        fakeEventBridgeClient.putEvents(request);
        assertThat(fakeEventBridgeClient.getRequestEntries(), contains(entry));
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
