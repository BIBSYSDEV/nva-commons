package no.unit.nva.commons.dlq;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.core.Environment;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.Record;

public class PushToFirehoseService implements FailedEventHandlingService {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String DELIVERY_STREAM_NAME = ENVIRONMENT.readEnv("DELIVERY_STREAM_NAME");

    private final FirehoseClient firehoseClient;

    public PushToFirehoseService(FirehoseClient firehoseClient) {
        this.firehoseClient = firehoseClient;
    }

    @Override
    public void handleFailedEvents(Collection<String> failedEvents) {
        pushToFirehose(failedEvents);
    }

    private static Record createFirehoseRecord(String failedEvent) {
        return Record.builder().data(SdkBytes.fromString(failedEvent, StandardCharsets.UTF_8)).build();
    }

    private static PutRecordBatchRequest assemblePutBatchRequest(List<Record> records) {
        return PutRecordBatchRequest.builder()
                   .records(records)
                   .deliveryStreamName(DELIVERY_STREAM_NAME)

                   .build();
    }

    private void pushToFirehose(Collection<String> group) {
        var records = group.stream()
                          .map(PushToFirehoseService::createFirehoseRecord)
                          .collect(Collectors.toList());
        var request = assemblePutBatchRequest(records);
        firehoseClient.putRecordBatch(request);
    }
}
