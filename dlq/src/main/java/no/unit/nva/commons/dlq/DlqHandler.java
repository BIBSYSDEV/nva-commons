package no.unit.nva.commons.dlq;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.core.Environment;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.Record;

/**
 *
 * This DLQ handler pushes the failed events to an S3 bucket through a firehose for automatically organizing
 * the events by time.
 * The DLQ handler is agnostic to the kind of event that has failed. It only stores the event to a bucket.
 *
 * Example setup:
 * <pre>{@code
 *   SomeLambda:
 *     Type: AWS::Serverless::Function
 *     Properties:
 *       CodeUri: some-lambda
 *       Handler: no.sikt.nva.something.SomeHandler::handleRequest
 *       PackageType: Zip
 *       EventInvokeConfig:
 *         DestinationConfig:
 *           OnFailure:
 *             Type: SQS
 *             Destination: !GetAtt DlqStack.Outputs.DlqArn
 *       Role: !GetAtt SomeLambdaRole.Arn
 * }
 *
 *
 * </pre>
 * See more detailed example in the test resources.
 */
public class DlqHandler implements RequestHandler<SQSEvent, Void> {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String DELIVERY_STREAM_NAME = ENVIRONMENT.readEnv("DELIVERY_STREAM_NAME");
    public static final int NUMBER_OF_GROUPS = 10;
    private final FirehoseClient firehoseClient;

    public DlqHandler(FirehoseClient firehoseClient) {
        this.firehoseClient = firehoseClient;
    }

    @Override
    public Void handleRequest(SQSEvent input, Context context) {

        var failedEvents = input.getRecords().stream()
                               .map(SQSMessage::getBody)
                               .collect(Collectors.toList());
        var groups = splitToSubLists(failedEvents);
        groups.forEach(this::pushToFirehose);
        return null;
    }

    private static Record createFirehoseRecord(String failedEvent) {
        return Record.builder().data(SdkBytes.fromString(failedEvent, StandardCharsets.UTF_8)).build();
    }

    private static Collection<List<String>> splitToSubLists(List<String> failedEvents) {

        return Lists.partition(failedEvents, NUMBER_OF_GROUPS);
    }

    private static PutRecordBatchRequest assemblePutBatchRequest(List<Record> records) {
        return PutRecordBatchRequest.builder()
                   .records(records)
                   .deliveryStreamName(DELIVERY_STREAM_NAME)
                   .build();
    }

    private void pushToFirehose(List<String> group) {
        var records = group.stream()
                          .map(DlqHandler::createFirehoseRecord)
                          .collect(Collectors.toList());
        var request = assemblePutBatchRequest(records);
        firehoseClient.putRecordBatch(request);
    }
}
