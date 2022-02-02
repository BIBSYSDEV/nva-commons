package no.unit.nva.events.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.EventsConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class ScanDatabaseRequestV2Test {

    public static final String EVENT_TOPIC = "topic";

    @Test
    void shouldReturnAnEventBridgeEventWhereTheTopicIsSetInTheDetailBody() throws JsonProcessingException {
        String expectedTopic = randomString();
        var request = new ScanDatabaseRequestV2(expectedTopic, 100, null);
        var event = request.createNewEventEntry(randomString(), randomString(), randomString());
        var eventAsJson = EventsConfig.objectMapper.readTree(event.detail());
        assertThat(eventAsJson.get(EVENT_TOPIC).textValue(), is(equalTo(expectedTopic)));
    }

    @Test
    void shouldReturnScanRequestFromEventDetail() throws JsonProcessingException {
        var originalRequest = new ScanDatabaseRequestV2(randomString(), randomInteger(), null);
        var event = originalRequest.createNewEventEntry(randomString(), randomString(), randomString());
        var reconstructedRequest =
            ScanDatabaseRequestV2.fromJson(event.detail());
        assertThat(reconstructedRequest, is(equalTo(originalRequest)));
    }

    @Test
    void shouldGenerateNewEventWithSameTopicAndPageSize() {
        var originalRequest = new ScanDatabaseRequestV2(randomString(), randomInteger(), null);
        var expectedStartMarker = Map.of(randomString(), randomStringAttribute(),
                                         randomString(), randomStringAttribute());
        var newRequest = originalRequest.newScanDatabaseRequest(expectedStartMarker);
        assertThat(newRequest.getStartMarker(), is(equalTo(expectedStartMarker)));
    }

    @Test
    void shouldDeserializeEmptyObject() throws JsonProcessingException {
        var deserializedFromEmptyJson = objectMapperWithoutSpecialConfig()
            .readValue(emptyJson(), ScanDatabaseRequestV2.class);
        var expectedDeserializedObject =
            new ScanDatabaseRequestV2(null, ScanDatabaseRequestV2.DEFAULT_PAGE_SIZE, null);
        assertThat(deserializedFromEmptyJson, is(equalTo(expectedDeserializedObject)));
    }

    private AttributeValue randomStringAttribute() {
        return AttributeValue.builder().s(randomString()).build();
    }

    private String emptyJson() {
        return JsonUtils.dtoObjectMapper.createObjectNode().toString();
    }

    private ObjectMapper objectMapperWithoutSpecialConfig() {
        return new ObjectMapper();
    }
}