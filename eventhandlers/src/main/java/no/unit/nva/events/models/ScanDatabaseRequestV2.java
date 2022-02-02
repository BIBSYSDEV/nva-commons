package no.unit.nva.events.models;

import static no.unit.nva.events.EventsConfig.objectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Class that can be sent as an event to a lambda handler for scanning a page of a DynamoDB table. The {@code
 * startMarker} is a scan start marker as required by the DynamoDb client. The {@code pageSize} is the number of the
 * results the scan will return (max 1000). The {@code topic} is the event topic that the handler is listening for
 * events.
 */
public class ScanDatabaseRequestV2 extends AbstractScanDatabaseRequest<AttributeValue> {

    @JsonCreator
    public ScanDatabaseRequestV2(@JsonProperty(TOPIC) String topic,
                                 @JsonProperty(PAGE_SIZE) Integer pageSize,
                                 @JsonProperty(START_MARKER) Map<String, AttributeValue> startMarker) {
        super(topic, pageSize, startMarker);
    }

    public static ScanDatabaseRequestV2 fromJson(String detail)
        throws JsonProcessingException {
        return objectMapper.readValue(detail, ScanDatabaseRequestV2.class);
    }

    @Override
    public ScanDatabaseRequestV2 newScanDatabaseRequest(Map<String, AttributeValue> newStartMarker) {
        return new ScanDatabaseRequestV2(getTopic(), getPageSize(), newStartMarker);
    }
}
