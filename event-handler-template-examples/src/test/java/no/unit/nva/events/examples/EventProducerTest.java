package no.unit.nva.events.examples;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.eventbridge.AmazonEventBridge;
import com.amazonaws.services.eventbridge.model.PutEventsRequest;
import com.amazonaws.services.eventbridge.model.PutEventsResult;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import no.unit.nva.stubs.FakeContext;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class EventProducerTest {

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handleRequestDoesNotThrowExceptionWhenInputIsNull() {
        Context context = new FakeContext();
        AmazonEventBridge mockClient = mock(AmazonEventBridge.class);
        when(mockClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(new PutEventsResult().withFailedEntryCount(0));
        Environment env = mock(Environment.class);
        when(env.readEnv(anyString())).thenReturn("anything");

        EventProducer eventConducer = new EventProducer(env, mockClient);
        Executable action = () -> eventConducer.handleRequest(null, outputStream, context);
        assertDoesNotThrow(action);
    }

    @Test
    public void handleRequestDoesNotThrowExceptionWhenInputIsNotNull() {
        Context context = new FakeContext();
        AmazonEventBridge mockClient = mock(AmazonEventBridge.class);
        when(mockClient.putEvents(any(PutEventsRequest.class)))
            .thenReturn(new PutEventsResult().withFailedEntryCount(0));
        Environment env = mock(Environment.class);
        when(env.readEnv(anyString())).thenReturn("anything");
        InputStream inputStream = IoUtils.stringToStream("success");
        EventProducer eventConducer = new EventProducer(env, mockClient);
        Executable action = () -> eventConducer.handleRequest(inputStream, outputStream, context);
        assertDoesNotThrow(action);
    }
}