package no.unit.nva.events.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EventReferenceTest {

    @Deprecated
    public static Stream<EventReference> deprecatedMethodProvider() {
        return Stream.of(
            new EventReference(randomString(), randomString(), randomUri(), randomInstant()),
            new EventReference(randomString(), randomUri())
        );
    }

    @Test
    void shouldSerializeToJsonObjectThatContainsTopicAndUri() {
        String expectedTopic = randomString();
        URI expectedUri = randomUri();
        EventReference eventReference = new EventReference(expectedTopic, expectedUri, UUID.randomUUID());
        String json = eventReference.toJsonString();
        assertThat(json, containsString(expectedTopic));
        assertThat(json, containsString(expectedUri.toString()));
    }
    
    @Test
    void shouldDeSerializeFromJsonObjectWithoutInformationLoss() {
        String expectedTopic = randomString();
        URI expectedUri = randomUri();
        EventReference eventReference = new EventReference(expectedTopic, expectedUri, UUID.randomUUID());
        EventReference deserializedEventReference = EventReference.fromJson(eventReference.toJsonString());
        assertThat(deserializedEventReference, is(equalTo(eventReference)));
    }
    
    @Test
    void shouldProvideBucketNameContainingTheEvent() {
        var s3uri = URI.create("s3://expected-bucket/path/to/file");
        var eventReference = new EventReference(randomString(), randomString(), s3uri,
                                                Instant.now(), UUID.randomUUID());
        assertThat(eventReference.extractBucketName(), is(equalTo("expected-bucket")));
    }

    @Deprecated
    @ParameterizedTest(name = "Should ensure deprecated constructors have unique Identifiers")
    @MethodSource("deprecatedMethodProvider")
    void shouldHaveIdentifierWhenDeprecatedMethodIsUsed(EventReference eventReference) {
        assertThat(eventReference.getIdentifier(), is(not(nullValue())));
    }
}