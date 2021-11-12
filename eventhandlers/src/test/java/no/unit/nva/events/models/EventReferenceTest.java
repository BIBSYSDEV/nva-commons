package no.unit.nva.events.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import java.net.URI;
import org.junit.jupiter.api.Test;

class EventReferenceTest {

    @Test
    void shouldSerializeToJsonObjectThatContainsTopicAndUri() {
        String expectedTopic = randomString();
        URI expectedUri = randomUri();
        EventReference eventReference = new EventReference(expectedTopic, expectedUri);
        String json = eventReference.toJsonString();
        assertThat(json, containsString(expectedTopic));
        assertThat(json, containsString(expectedUri.toString()));
    }

    @Test
    void shouldDeSerializeFromJsonObjectWithoutInformationLoss() {
        String expectedTopic = randomString();
        URI expectedUri = randomUri();
        EventReference eventReference = new EventReference(expectedTopic, expectedUri);
        EventReference deserializedEventReference = EventReference.fromJson(eventReference.toJsonString());
        assertThat(deserializedEventReference, is(equalTo(eventReference)));
    }
}