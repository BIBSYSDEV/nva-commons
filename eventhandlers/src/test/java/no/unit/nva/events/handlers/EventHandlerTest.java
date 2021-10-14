package no.unit.nva.events.handlers;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static nva.commons.core.JsonUtils.dynamoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.stubs.FakeContext;
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
    public static final String CLASS_PROPERTY = "class";
    public static final boolean CONTAINS_EMPTY_FIELDS = true;
    public static final boolean DOES_NOT_CONTAIN_EMPTY_FIELDS = !CONTAINS_EMPTY_FIELDS;
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
        final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
        handler.handleRequest(input, outputStream, context);
        AwsEventBridgeEvent<SampleEventDetail> expectedEvent = parseEventFromSampleEventString();
        AwsEventBridgeEvent<SampleEventDetail> actualEvent = handler.eventBuffer.get();
        assertThat(actualEvent, is(equalTo(expectedEvent)));
    }

    @Test
    public void handleRequestLogsErrorWhenExceptionIsThrown() {
        TestAppender appender = LogUtils.getTestingAppender(EventHandler.class);
        var handler = new EventHandlerThrowingException();
        final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
        Executable action = () -> handler.handleRequest(input, outputStream, context);
        assertThrows(RuntimeException.class, action);
        assertThat(appender.getMessages(), containsString(EXCEPTION_MESSAGE));
    }

    @Test
    public void handleRequestReThrowsExceptionWhenExceptionIsThrown() {
        var handler = new EventHandlerThrowingException();
        Executable action = () -> handler.handleRequest(sampleInputStream(AWS_EVENT_BRIDGE_EVENT), outputStream,
                                                        context);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), is(equalTo(EXCEPTION_MESSAGE)));
    }

    @Test
    public void handleRequestSerializesObjectsWithoutOmittingEmptyValuesWhenSuchMapperHasBeenSet()
        throws JsonProcessingException, IntrospectionException {
        final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
        EventHandlerTestClass handler = new EventHandlerTestClass(dtoObjectMapper);
        ObjectNode objectNode = sendEventAndCollectOutputAsJsonObject(input, handler);
        assertThatJsonObjectContainsOrNotContainsNonNullEmptyFields(objectNode, CONTAINS_EMPTY_FIELDS);
    }

    private void assertThatJsonObjectContainsOrNotContainsNonNullEmptyFields(ObjectNode objectNode,
                                                                             boolean containsEmptyFields)
        throws IntrospectionException {
        List<String> properties = SampleEventDetail.propertyNamesOfEmptyFields();
        properties.forEach(property -> assertThat(property, objectNode.has(property), is(containsEmptyFields)));
    }

    @Test
    public void handleRequestSerializesObjectsOmittingEmptyValuesWhenSuchMapperHasBeenSet()
        throws JsonProcessingException, IntrospectionException {
        final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
        EventHandlerTestClass handler = new EventHandlerTestClass(dynamoObjectMapper);
        ObjectNode objectNode = sendEventAndCollectOutputAsJsonObject(input, handler);

        assertThatJsonObjectContainsOrNotContainsNonNullEmptyFields(objectNode, DOES_NOT_CONTAIN_EMPTY_FIELDS);
    }

    @Test
    public void handleRequestSerializesObjectsOmittingEmptyValuesByDefault()
        throws JsonProcessingException, IntrospectionException {
        final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
        EventHandlerTestClass handler = new EventHandlerTestClass();
        ObjectNode objectNode = sendEventAndCollectOutputAsJsonObject(input, handler);
        assertThatJsonObjectContainsOrNotContainsNonNullEmptyFields(objectNode, false);
    }

    private ObjectNode sendEventAndCollectOutputAsJsonObject(InputStream input, EventHandlerTestClass handler)
        throws JsonProcessingException {
        handler.handleRequest(input, outputStream, context);
        String output = outputStream.toString();
        assertThat(output, is(not(emptyString())));
        return (ObjectNode) dtoObjectMapper.readTree(output);
    }

    private Stream<String> extractPropertyNamesFromSamleEventDetailClass() throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(SampleEventDetail.class).getPropertyDescriptors())
            .map(FeatureDescriptor::getName)
            .filter(name -> !name.equals(CLASS_PROPERTY));
    }

    private InputStream sampleInputStream(String filename) {
        return IoUtils.stringToStream(filename);
    }

    private AwsEventBridgeEvent<SampleEventDetail> parseEventFromSampleEventString() throws JsonProcessingException {
        JavaType javatype = dtoObjectMapper.getTypeFactory()
            .constructParametricType(AwsEventBridgeEvent.class, SampleEventDetail.class);
        return dtoObjectMapper.readValue(AWS_EVENT_BRIDGE_EVENT, javatype);
    }

    private static class EventHandlerTestClass extends EventHandler<SampleEventDetail, SampleEventDetail> {

        private final AtomicReference<AwsEventBridgeEvent<SampleEventDetail>> eventBuffer = new AtomicReference<>();
        private final AtomicReference<SampleEventDetail> inputBuffer = new AtomicReference<>();

        protected EventHandlerTestClass(ObjectMapper objectMapper) {
            super(SampleEventDetail.class, objectMapper);
        }

        protected EventHandlerTestClass() {
            super(SampleEventDetail.class);
        }

        @Override
        protected SampleEventDetail processInput(SampleEventDetail input,
                                                 AwsEventBridgeEvent<SampleEventDetail> event,
                                                 Context context) {
            eventBuffer.set(event);
            inputBuffer.set(input);

            return SampleEventDetail.eventWithEmptyFields();
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