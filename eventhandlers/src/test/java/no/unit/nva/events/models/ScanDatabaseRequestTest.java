package no.unit.nva.events.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import no.unit.nva.events.EventsConfig;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

class ScanDatabaseRequestTest {

    public static final String EVENT_TOPIC = "topic";

    @Test
    void shouldReturnAnEventBridgeEventWhereTheTopicIsSetInTheDetailBody() throws JsonProcessingException {
        String expectedTopic = randomString();
        var request = new ScanDatabaseRequest(expectedTopic, 100, null);
        var event = request.createNewEventEntry(randomString(), randomString(), randomString());
        var eventAsJson = EventsConfig.objectMapper.readTree(event.detail());
        assertThat(eventAsJson.get(EVENT_TOPIC).textValue(), is(equalTo(expectedTopic)));
    }

    @Test
    void shouldReturnScanRequestFromEventDetail() throws JsonProcessingException {
        var originalRequest = new ScanDatabaseRequest(randomString(), randomInteger(), null);
        var event = originalRequest.createNewEventEntry(randomString(), randomString(), randomString());
        var reconstructedRequest = ScanDatabaseRequest.fromJson(event.detail());
        assertThat(reconstructedRequest, is(equalTo(originalRequest)));
    }

    @Test
    void shouldGenerateNewEventWithSameTopicAndPageSizer() {
        var originalRequest = new ScanDatabaseRequest(randomString(), randomInteger(), null);
        var expectedStartMarker = Map.of(randomString(), new AttributeValue(randomString()),
                                         randomString(), new AttributeValue(randomString()));
        var newRequest = originalRequest.newScanDatabaseRequest(expectedStartMarker);
        assertThat(newRequest.getStartMarker(), is(equalTo(expectedStartMarker)));
    }

    @Test
    void shouldDeserializeEmptyObject() throws JsonProcessingException {
        var deserializedFromEmptyJson = objectMapperWithoutSpecialConfig()
            .readValue(emptyJson(), ScanDatabaseRequest.class);
        var expectedDeserializedObject =
            new ScanDatabaseRequest(null, ScanDatabaseRequest.DEFAULT_PAGE_SIZE, null);
        assertThat(deserializedFromEmptyJson, is(equalTo(expectedDeserializedObject)));
    }

    private String emptyJson() {
        return JsonUtils.dtoObjectMapper.createObjectNode().toString();
    }

    private ObjectMapper objectMapperWithoutSpecialConfig() {
        return new ObjectMapper();
    }
}