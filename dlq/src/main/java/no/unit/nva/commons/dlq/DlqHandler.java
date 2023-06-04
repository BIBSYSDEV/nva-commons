package no.unit.nva.commons.dlq;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.firehose.FirehoseClient;

/**
 * This DLQ handler pushes the failed events to an S3 bucket through a firehose for automatically organizing the events
 * by time. The DLQ handler is agnostic to the kind of event that has failed. It only stores the event to a bucket.
 * <p>
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
 *
 *             Destination: !GetAtt DlqStack.Outputs.DlqArn
 *       Role: !GetAtt SomeLambdaRole.Arn
 * }
 *
 *
 * </pre>
 * See more detailed example in the test resources.
 *
 * This class cannot be instantiated on purpose. You can use the default implementation (i.e. pushing to a Firehose) by
 * extending this class and calling the factory method {@link  DlqHandler#defaultService(FirehoseClient)}
 * in the default constructor. This will force developers to create an explicit trace of the logic they are using in
 * their code.
 *
 */
public class DlqHandler implements RequestHandler<SQSEvent, Void> {

    //TODO: Make number of groups configurable.
    public static final int NUMBER_OF_GROUPS = 10;
    private final FailedEventHandlingService failedEventsHandlingService;

    protected DlqHandler(FailedEventHandlingService failedEventsHandlingService) {
        this.failedEventsHandlingService = failedEventsHandlingService;
    }

    /**
     * @param firehoseClient
     * @return
     */
    public static PushToFirehoseService defaultService(FirehoseClient firehoseClient) {
        return new PushToFirehoseService(firehoseClient);
    }

    @Override
    public Void handleRequest(SQSEvent input, Context context) {

        var failedEvents = extractFailedEventsFromDlqMessages(input);
        Lists.partition(failedEvents, NUMBER_OF_GROUPS).forEach(failedEventsHandlingService::handleFailedEvents);
        return null;
    }

    private static List<String> extractFailedEventsFromDlqMessages(SQSEvent input) {
        return input.getRecords().stream()
                   .map(SQSMessage::getBody)
                   .collect(Collectors.toList());
    }
}

