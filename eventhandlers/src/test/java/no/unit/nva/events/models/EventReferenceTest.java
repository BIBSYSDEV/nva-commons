package no.unit.nva.events.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import java.net.URI;
import java.time.Instant;
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
    
    @Test
    void shouldProvideBucketNameContainingTheEvent() {
        var s3uri = URI.create("s3://expected-bucket/path/to/file");
        var eventReference = new EventReference(randomString(), randomString(), s3uri, Instant.now());
        assertThat(eventReference.extractBucketName(), is(equalTo("expected-bucket")));
    }
}