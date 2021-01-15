package no.unit.nva.events.examples;

import static nva.commons.utils.JsonUtils.objectMapper;
import static nva.commons.utils.attempt.Try.attempt;

import com.amazonaws.services.eventbridge.AmazonEventBridge;
import com.amazonaws.services.eventbridge.AmazonEventBridgeClientBuilder;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsRequestEntry;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Optional;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.StringUtils;
import nva.commons.utils.attempt.Failure;
import nva.commons.utils.attempt.FunctionWithException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The classes {@link EventProducer}, {@link EventConducer} ,and {@link DestinationsEventConsumer} are a demo of the
 * usage of the LambdaBridge events.
 *
 * <p>When {@link EventProducer} is run, {@link EventConducer} reads the emitted message
 * and {@link DestinationsEventConsumer} reads the message from {@link EventConducer}
 *
 * <p>To run {@link EventProducer}, run from the command line the script "lambda.sh" (requires login via
 * aws-cli-tools). The script runs the {@link EventProducer} with input the file "requestPayload.json". The file
 * contains a field with name "request" and the whole payload is send to the {@link EventConducer}. <br/> If the
 * "request" field in the payload contains the word "success" then the {@link EventConducer} sends a message to
 * EventBridge. <br/> If it contains the word "failure" then the {@link EventConducer} throws an Exception and the
 * failed execution details appear in the attached SQL queue.
 */
public class EventProducer implements RequestStreamHandler {

    public static final String EVENT_BUS_ENV_VAR = "EVENT_BUS";
    public static final String SOURCE = "SomeSource";
    public static final String LOG_HANDLER_HAS_RUN = "Event Producer has been called!!!";
    private static final Logger logger = LoggerFactory.getLogger(EventProducer.class);
    public static final String DEFAULT_MESSAGE = "success";

    private final Environment environment;
    private final AmazonEventBridge eventBridgeClient;

    @JacocoGenerated
    public EventProducer() {
        this(new Environment(), AmazonEventBridgeClientBuilder.defaultClient());
    }

    @JacocoGenerated
    public EventProducer(Environment environment, AmazonEventBridge eventBridgeClient) {
        this.environment = environment;
        this.eventBridgeClient = eventBridgeClient;
    }

    @JacocoGenerated
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String inputString = readInputStream(input);
        String message = "success";
        if (inputString.contains("failure")) {
            message = "failure";
        }

        DataciteDoiRequest sentDirectly = newDataciteDoiRequest(message);

        logger.info(LOG_HANDLER_HAS_RUN);
        putEventDirectlyToEventBridge(sentDirectly);
        writeOutput(null, output);
    }

    @JacocoGenerated
    private String readInputStream(InputStream input) {
        return attempt(() -> IoUtils.streamToString(input)).orElse(sendSuccessOnEmptyInput());
    }

    private FunctionWithException<Failure<String>, String, RuntimeException> sendSuccessOnEmptyInput() {
        return fail -> DEFAULT_MESSAGE;
    }

    @JacocoGenerated
    private <I> void writeOutput(I event, OutputStream outputStream)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String responseJson = Optional.ofNullable(objectMapper.writeValueAsString(event))
                .map(StringUtils::replaceWhiteSpacesWithSpace)
                .map(StringUtils::removeMultipleWhiteSpaces)
                .orElseThrow();
            logger.info(responseJson);
            writer.write(responseJson);
        }
    }

    @JacocoGenerated
    private void putEventDirectlyToEventBridge(DataciteDoiRequest dataciteDoiRequest) {
        logger.info("Putting event directly to eventbridge");
        PutEventsRequestEntry putEventsRequestEntry = new PutEventsRequestEntry()
            .withDetail(dataciteDoiRequest.toString())
            .withEventBusName(environment.readEnv(EVENT_BUS_ENV_VAR))
            .withSource(SOURCE)
            .withDetailType(dataciteDoiRequest.getType());

        PutEventsRequest putEventsRequest = new PutEventsRequest().withEntries(putEventsRequestEntry);
        eventBridgeClient.putEvents(putEventsRequest);
    }

    @JacocoGenerated
    private DataciteDoiRequest newDataciteDoiRequest(String message) {
        return DataciteDoiRequest.newBuilder()
            .withExistingDoi(URI.create("http://somedoi.org"))
            .withPublicationId(URI.create("https://somepublication.com"))
            .withXml(message)
            .withType("MyType")
            .build();
    }
}
