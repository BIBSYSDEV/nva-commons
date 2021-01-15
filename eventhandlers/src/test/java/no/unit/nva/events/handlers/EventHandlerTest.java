package no.unit.nva.events.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class EventHandlerTest {

    public static final String AWS_EVENT_BRIDGE_EVENT =
        IoUtils.stringFromResources(Path.of("validEventBridgeEvent.json"));
    public static final String EXCEPTION_MESSAGE = "EXCEPTION_MESSAGE";
    private ByteArrayOutputStream outputStream;
    private Context context;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @Test
    public void handleRequestAcceptsValidEvent() throws JsonProcessingException {
        EventHandlerTestClass handler = new EventHandlerTestClass();
        handler.handleRequest(sampleInputStream(), outputStream, context);
        AwsEventBridgeEvent<SampleEventDetail> expectedEvent = parseEventFromSampleEventString();
        AwsEventBridgeEvent<SampleEventDetail> actualEvent = handler.eventBuffer.get();
        assertThat(actualEvent, is(equalTo(expectedEvent)));
    }

    @Test
    public void handleRequestLogsErrorWhenExceptionIsThrown() {
        TestAppender appender = LogUtils.getTestingAppender(EventHandler.class);
        var handler = new EventHandlerThrowingException();
        Executable action = () -> handler.handleRequest(sampleInputStream(), outputStream, context);
        assertThrows(RuntimeException.class, action);
        assertThat(appender.getMessages(), containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void handleRequestReThrowsExceptionWhenExceptionIsThrown() {
        var handler = new EventHandlerThrowingException();
        Executable action = () -> handler.handleRequest(sampleInputStream(), outputStream, context);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), is(equalTo(EXCEPTION_MESSAGE)));
    }

    private InputStream sampleInputStream() {
        return IoUtils.stringToStream(AWS_EVENT_BRIDGE_EVENT);
    }

    private AwsEventBridgeEvent<SampleEventDetail> parseEventFromSampleEventString() throws JsonProcessingException {
        JavaType javatype = JsonUtils.objectMapper.getTypeFactory()
            .constructParametricType(AwsEventBridgeEvent.class, SampleEventDetail.class);
        return JsonUtils.objectMapper.readValue(AWS_EVENT_BRIDGE_EVENT, javatype);
    }

    private static class EventHandlerTestClass extends EventHandler<SampleEventDetail, SampleEventDetail> {

        private AtomicReference<AwsEventBridgeEvent<SampleEventDetail>> eventBuffer = new AtomicReference<>();
        private AtomicReference<SampleEventDetail> inputBuffer = new AtomicReference<>();

        protected EventHandlerTestClass() {
            super(SampleEventDetail.class);
        }

        @Override
        protected SampleEventDetail processInput(SampleEventDetail input,
                                                 AwsEventBridgeEvent<SampleEventDetail> event,
                                                 Context context) {
            eventBuffer.set(event);
            inputBuffer.set(input);

            return input;
        }
    }

    private static class EventHandlerThrowingException extends EventHandler<SampleEventDetail, Void> {

        protected EventHandlerThrowingException() {
            super(SampleEventDetail.class);
        }

        @Override
        protected Void processInput(SampleEventDetail input, AwsEventBridgeEvent<SampleEventDetail> event,
                                    Context context) {
            throw new RuntimeException(EXCEPTION_MESSAGE);
        }
    }
}