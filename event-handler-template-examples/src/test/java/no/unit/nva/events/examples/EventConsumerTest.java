package no.unit.nva.events.examples;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import no.unit.nva.stubs.FakeContext;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class EventConsumerTest {

    public static final InputStream AWS_EVENT_BRIDGE_EVENT =
        IoUtils.inputStreamFromResources(Path.of("validAwsEventBridgeEvent.json"));
    private ByteArrayOutputStream outputStream;
    private Context context;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @Test
    public void handleRequestDoesNotThrowExceptionForValidEvent() {
        DestinationsEventConsumer destinationsEventConsumer = new DestinationsEventConsumer();
        Executable action = () -> destinationsEventConsumer.handleRequest(AWS_EVENT_BRIDGE_EVENT, outputStream,
            context);
        assertDoesNotThrow(action);
    }
}