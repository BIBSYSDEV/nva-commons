package no.unit.nva.events.models;

import static no.unit.nva.events.EventsConfig.objectMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;

/**
 * Class that can be sent as an event to a lambda handler for scanning a page of a DynamoDB table. The {@code
 * startMarker} is a scan start marker as required by the DynamoDb client. The {@code pageSize} is the number of the
 * results the scan will return (max 1000). The {@code topic} is the event topic that the handler is listening for
 * events.
 */
public class ScanDatabaseRequest extends AbstractScanDatabaseRequest<AttributeValue> {

    @JsonCreator
    public ScanDatabaseRequest(@JsonProperty(TOPIC) String topic,
                               @JsonProperty(PAGE_SIZE) Integer pageSize,
                               @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker) {
        super(topic, pageSize, startMarker);
    }

    @Override
    public ScanDatabaseRequest newScanDatabaseRequest(Map<String, AttributeValue> newStartMarker) {
        return new ScanDatabaseRequest(getTopic(), getPageSize(), newStartMarker);
    }

    public static ScanDatabaseRequest fromJson(String detail)
        throws JsonProcessingException {
        return objectMapper.readValue(detail, ScanDatabaseRequest.class);
    }
}
