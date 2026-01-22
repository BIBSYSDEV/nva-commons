package no.unit.nva.events.models;

import static no.unit.nva.events.models.ScanDatabaseRequest.DEFAULT_PAGE_SIZE;
import static no.unit.nva.events.models.ScanDatabaseRequest.DYNAMODB_EMPTY_MARKER;
import static no.unit.nva.events.models.ScanDatabaseRequest.MAX_PAGE_SIZE;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class ScanDatabaseRequestTest {

    public static final String EVENT_TOPIC = "topic";
    private static final Map<String, String> EMPTY_MARKER = null;

    @Test
    void shouldReturnAnEventBridgeEventWhereTheTopicIsSetInTheDetailBody() throws JsonProcessingException {
        String expectedTopic = randomString();
        var request = new ScanDatabaseRequest(expectedTopic, 100, null);
        var event = request.createNewEventEntry(randomString(), randomString(), randomString());
        var eventAsJson = EventsConfig.objectMapper.readTree(event.detail());
        assertThat(eventAsJson.get(EVENT_TOPIC).textValue(), is(equalTo(expectedTopic)));
    }

    @Test
    void shouldReturnScanRequestFromEventDetail() {
        var originalRequest = new ScanDatabaseRequest(randomString(), randomInteger(), null);
        var event = originalRequest.createNewEventEntry(randomString(), randomString(), randomString());
        var reconstructedRequest =
            ScanDatabaseRequest.fromJson(event.detail());
        assertThat(reconstructedRequest, is(equalTo(originalRequest)));
    }

    @Test
    void shouldGenerateNewEventWithSameTopicAndPageSize() {
        var originalRequest =
            new ScanDatabaseRequest(randomString(), randomInteger(MAX_PAGE_SIZE), null);
        var expectedStartMarker = Map.of(randomString(), randomStringAttribute(),
                                         randomString(), randomStringAttribute());
        var newRequest = originalRequest.newScanDatabaseRequest(expectedStartMarker);
        assertThat(newRequest.toDynamoScanMarker(), is(equalTo(expectedStartMarker)));
    }

    @Test
    void shouldDeserializeEmptyObject() throws JsonProcessingException {
        var deserializedFromEmptyJson = ScanDatabaseRequest.fromJson(emptyJson());
        var expectedDeserializedObject =
            new ScanDatabaseRequest(null, ScanDatabaseRequest.DEFAULT_PAGE_SIZE, null);
        assertThat(deserializedFromEmptyJson, is(equalTo(expectedDeserializedObject)));
    }

    @ParameterizedTest(name = "should set default page size when page size is off limits.Page size:{0}")
    @ValueSource(ints = {-1, 0, MAX_PAGE_SIZE + 1, MAX_PAGE_SIZE + 100})
    void shouldSetDefaultPageSizeWhenRequestedPageSizeIsOfLimits(int pageSize) {
        var expectedStartMarker = Map.of(randomString(), randomString(),
                                         randomString(), randomString());
        var expectedTopic = randomString();
        var actualRequest =
            new ScanDatabaseRequest(expectedTopic, pageSize, expectedStartMarker);
        var expectedRequest = new ScanDatabaseRequest(expectedTopic, DEFAULT_PAGE_SIZE, expectedStartMarker);

        assertThat(actualRequest, is(equalTo(expectedRequest)));
    }

    @Test
    void shouldSerializeAndDeserializeWithoutInformationLoss() {
        var startMarker = randomMarker();
        var sampleRequest = new ScanDatabaseRequest(randomString(), randomInteger(), startMarker);
        var json = sampleRequest.toString();
        var deserialized = ScanDatabaseRequest.fromJson(json);
        assertThat(deserialized, is(equalTo(sampleRequest)));
        assertThatNonSerializableDynamoScanMarkerConstainsSameValuesAsItsEquivalentSerializableRepresentation(
            startMarker, deserialized);
    }

    @Test
    void shouldProduceDynamoCompatibleEmptyMarkerWhenSerializableVersionOfMarkerIsNull() {
        var sampleRequest = new ScanDatabaseRequest(randomString(), randomInteger(), EMPTY_MARKER);
        assertThat(sampleRequest.toDynamoScanMarker(), is(equalTo(DYNAMODB_EMPTY_MARKER)));
    }

    private void assertThatNonSerializableDynamoScanMarkerConstainsSameValuesAsItsEquivalentSerializableRepresentation(
        Map<String, String> startMarker, ScanDatabaseRequest deserialized) {
        for (var key : startMarker.keySet()) {
            var expectedAttributeValue = startMarker.get(key);
            var actualAttributeValue = deserialized.toDynamoScanMarker().get(key).s();
            assertThat(actualAttributeValue, is(equalTo(expectedAttributeValue)));
        }
    }

    private Map<String, String> randomMarker() {
        return Map.of(randomString(), randomString(),
                      randomString(), randomString());
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