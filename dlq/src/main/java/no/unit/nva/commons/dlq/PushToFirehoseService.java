package no.unit.nva.commons.dlq;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.Record;

public class PushToFirehoseService implements FailedEventHandlingService {

    private final FirehoseClient firehoseClient;
    private final String deliveryStreamName;

    public PushToFirehoseService(FirehoseClient firehoseClient, String deliveryStreamName) {
        this.firehoseClient = firehoseClient;
        this.deliveryStreamName = deliveryStreamName;
    }

    @Override
    public void handleFailedEvents(Collection<String> failedEvents) {
        pushToFirehose(failedEvents);
    }

    private static Record createFirehoseRecord(String failedEvent) {
        return Record.builder().data(SdkBytes.fromString(failedEvent, StandardCharsets.UTF_8)).build();
    }

    private  PutRecordBatchRequest assemblePutBatchRequest(List<Record> records) {
        return PutRecordBatchRequest.builder()
                   .records(records)
                   .deliveryStreamName(deliveryStreamName)
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
