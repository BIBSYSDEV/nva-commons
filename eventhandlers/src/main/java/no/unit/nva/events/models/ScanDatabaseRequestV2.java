package no.unit.nva.events.models;

import static java.util.Objects.nonNull;
import static no.unit.nva.events.EventsConfig.objectMapperLight;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

/**
 * Class that can be sent as an event to a lambda handler for scanning a page of a DynamoDB table. The {@code
 * startMarker} is a scan start marker as required by the DynamoDb client. The {@code pageSize} is the number of the
 * results the scan will return (max 1000). The {@code topic} is the event topic that the handler is listening for
 * events.
 */
public class ScanDatabaseRequestV2 implements EventBody {

    public static final String START_MARKER = "startMarker";
    public static final String PAGE_SIZE = "pageSize";

    public static final int DEFAULT_PAGE_SIZE = 700; // Choosing for safety 3/4 of max page size.
    public static final int MAX_PAGE_SIZE = 1000;
    public static final Map<String, AttributeValue> DYNAMODB_EMPTY_MARKER = null;

    @JsonProperty(START_MARKER)
    private Map<String, String> startMarker;
    @JsonProperty(PAGE_SIZE)
    private Integer pageSize;
    @JsonProperty(TOPIC)
    private String topic;

    public ScanDatabaseRequestV2() {

    }

    public ScanDatabaseRequestV2(String topic, Integer pageSize, Map<String, String> startMarker) {
        setPageSize(pageSize);
        setTopic(topic);
        setStartMarker(startMarker);
    }

    public static ScanDatabaseRequestV2 fromJson(String detail) {
        return attempt(() -> objectMapperLight.beanFrom(ScanDatabaseRequestV2.class, detail)).orElseThrow();
    }

    public Map<String, String> getStartMarker() {
        return startMarker;
    }

    public final void setStartMarker(Map<String, String> startMarker) {
        this.startMarker = startMarker;
    }

    public int getPageSize() {
        return nonNull(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    public final void setPageSize(int pageSize) {
        this.pageSize = isValid(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    public ScanDatabaseRequestV2 newScanDatabaseRequest(Map<String, AttributeValue> newStartMarker) {
        return new ScanDatabaseRequestV2(getTopic(), getPageSize(), toSerializableForm(newStartMarker));
    }

    @Override
    public String getTopic() {
        return topic;
    }

    public final void setTopic(String topic) {
        this.topic = topic;
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
            .detail(this.toString())
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

    @Override
    public String toString() {
        return attempt(() -> objectMapperLight.asString(this)).orElseThrow();
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
