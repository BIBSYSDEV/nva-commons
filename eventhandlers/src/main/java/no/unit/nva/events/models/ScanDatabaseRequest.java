package no.unit.nva.events.models;

import static no.unit.nva.events.EventsConfig.objectMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class ScanDatabaseRequest implements JsonSerializable {

    public static final String START_MARKER = "startMarker";
    public static final String PAGE_SIZE = "pageSize";

    public static final int DEFAULT_PAGE_SIZE = 700; // Choosing for safety 3/4 of max page size.
    public static final int MAX_PAGE_SIZE = 1000;
    public static final String TOPIC = "topic";
    @JsonProperty(START_MARKER)
    private final Map<String, AttributeValue> startMarker;
    @JsonProperty(PAGE_SIZE)
    private final int pageSize;
    @JsonProperty(TOPIC)
    private final String topic;

    @JsonCreator
    public ScanDatabaseRequest(
        @JsonProperty(TOPIC) String topic,
        @JsonProperty(PAGE_SIZE) int pageSize,
        @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker) {
        this.pageSize = pageSize;
        this.startMarker = startMarker;
        this.topic = topic;
    }

    public static ScanDatabaseRequest fromJson(String detail) throws JsonProcessingException {
        return objectMapper.readValue(detail, ScanDatabaseRequest.class);
    }

    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    public int getPageSize() {
        return pageSizeWithinLimits(pageSize)
                   ? pageSize
                   : DEFAULT_PAGE_SIZE;
    }

    @JacocoGenerated
    public Map<String, AttributeValue> getStartMarker() {
        return startMarker;
    }

    public ScanDatabaseRequest newScanDatabaseRequest(Map<String, AttributeValue> newStartMarker) {
        return new ScanDatabaseRequest(this.getTopic(), this.getPageSize(), newStartMarker);
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

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStartMarker(), getPageSize(), getTopic());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScanDatabaseRequest)) {
            return false;
        }
        ScanDatabaseRequest that = (ScanDatabaseRequest) o;
        return getPageSize() == that.getPageSize()
               && Objects.equals(getStartMarker(), that.getStartMarker())
               && Objects.equals(getTopic(), that.getTopic());
    }

    private boolean pageSizeWithinLimits(int pageSize) {
        return pageSize > 0 && pageSize <= MAX_PAGE_SIZE;
    }
}
