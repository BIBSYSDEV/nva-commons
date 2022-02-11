package no.unit.nva.events.models;

import static java.util.Objects.nonNull;
import static no.unit.nva.events.EventsConfig.objectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

/**
 * Class that can be sent as an event to a lambda handler for scanning a page of a DynamoDB table. The {@code
 * startMarker} is a scan start marker as required by the DynamoDb client. The {@code pageSize} is the number of the
 * results the scan will return (max 1000). The {@code topic} is the event topic that the handler is listening for
 * events.
 */
public class ScanDatabaseRequestV2 implements EventBody, JsonSerializable {

    public static final String START_MARKER = "startMarker";
    public static final String PAGE_SIZE = "pageSize";

    public static final int DEFAULT_PAGE_SIZE = 700; // Choosing for safety 3/4 of max page size.
    public static final int MAX_PAGE_SIZE = 1000;
    public static final Map<String, AttributeValue> DYNAMODB_EMPTY_MARKER = null;

    @JsonProperty(START_MARKER)
    private final Map<String, String> startMarker;
    @JsonProperty(PAGE_SIZE)
    private final int pageSize;
    @JsonProperty(TOPIC)
    private final String topic;

    @JsonCreator
    public ScanDatabaseRequestV2(@JsonProperty(TOPIC) String topic,
                                 @JsonProperty(PAGE_SIZE) Integer pageSize,
                                 @JsonProperty(START_MARKER) Map<String, String> startMarker) {
        this.pageSize = isValid(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
        this.topic = topic;
        this.startMarker = startMarker;
    }

    public static ScanDatabaseRequestV2 fromJson(String detail)
        throws JsonProcessingException {
        return objectMapper.readValue(detail, ScanDatabaseRequestV2.class);
    }

    public Map<String, String> getStartMarker() {
        return startMarker;
    }

    public int getPageSize() {
        return pageSize;
    }

    public ScanDatabaseRequestV2 newScanDatabaseRequest(Map<String, AttributeValue> newStartMarker) {
        return new ScanDatabaseRequestV2(getTopic(), getPageSize(), toSerializableForm(newStartMarker));
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @JsonIgnore
    public Map<String, AttributeValue> toDynamoScanMarker() {
        return
            nonNull(getStartMarker())
                ? convertSerializableMarkerToDynamoDbMarker()
                : DYNAMODB_EMPTY_MARKER;
    }

    public PutEventsRequestEntry createNewEventEntry(
        String eventBusName,
        String detailType,
        String invokedFunctionArn
    ) {
        return PutEventsRequestEntry
            .builder()
            .eventBusName(eventBusName)
            .detail(this.toJsonString())
            .detailType(detailType)
            .resources(invokedFunctionArn)
            .time(Instant.now())
            .source(invokedFunctionArn)
            .build();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getStartMarker(), getPageSize(), getTopic());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScanDatabaseRequestV2)) {
            return false;
        }
        ScanDatabaseRequestV2 that = (ScanDatabaseRequestV2) o;
        return getPageSize() == that.getPageSize()
               && Objects.equals(getStartMarker(), that.getStartMarker())
               && Objects.equals(getTopic(), that.getTopic());
    }

    private Map<String, AttributeValue> convertSerializableMarkerToDynamoDbMarker() {
        return getStartMarker().entrySet()
            .stream()
            .collect(Collectors.toMap(Entry::getKey, e -> createAttributeValue(e.getValue())));
    }

    private boolean isValid(Integer pageSize) {
        return nonNull(pageSize) && pageSize > 0 && pageSize < MAX_PAGE_SIZE;
    }

    private Map<String, String> toSerializableForm(Map<String, AttributeValue> newStartMarker) {
        return newStartMarker.entrySet().stream().collect(Collectors.toMap(Entry::getKey,
                                                                           entry -> entry.getValue().s()));
    }

    private AttributeValue createAttributeValue(String value) {
        return AttributeValue.builder().s(value).build();
    }
}
