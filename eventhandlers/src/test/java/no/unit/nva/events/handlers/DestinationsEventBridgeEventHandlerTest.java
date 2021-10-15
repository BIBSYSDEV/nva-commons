package no.unit.nva.events.handlers;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.beans.IntrospectionException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DestinationsEventBridgeEventHandlerTest extends AbstractEventHandlerTest {

    public static final String VALID_AWS_EVENT_BRIDGE_EVENT = IoUtils.stringFromResources(
        Path.of("validAwsEventBridgeEvent.json"));

    public static final boolean CONTAINS_EMPTY_FIELDS = true;
    private static final boolean DOES_NOT_CONTAIN_EMPTY_FIELDS = !CONTAINS_EMPTY_FIELDS;
    private static final JsonPointer RESPONSE_PAYLOAD_POINTER = JsonPointer.compile("/detail/responsePayload");

    private ByteArrayOutputStream outputStream;
    private Context context;

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @Test
    public void handleRequestAcceptsValidEvent() throws JsonProcessingException {
        DestinationsHandlerTestClass handler = new DestinationsHandlerTestClass();
        InputStream requestInput = IoUtils.stringToStream(VALID_AWS_EVENT_BRIDGE_EVENT);
        handler.handleRequest(requestInput, outputStream, context);
        SampleEventDetail expectedInput = extractInputFromValidAwsEventBridgeEvent(VALID_AWS_EVENT_BRIDGE_EVENT);
        assertThat(handler.inputBuffer.get(), is(equalTo(expectedInput)));
    }

    @Test
    public void handleRequestSerializesObjectsWithoutOmittingEmptyValuesWhenSuchMapperHasBeenSet()
        throws JsonProcessingException, IntrospectionException {
        final InputStream input = IoUtils.stringToStream(VALID_AWS_EVENT_BRIDGE_EVENT);
        ObjectMapper injectedMapper = JsonUtils.dtoObjectMapper;
        DestinationsHandlerTestClass handler = new DestinationsHandlerTestClass(injectedMapper);
        handler.handleRequest(input, outputStream, context);
        ObjectNode outputObject = (ObjectNode) injectedMapper.readTree(outputStream.toString());
        assertThatJsonObjectContainsEmptyFields(outputObject);
    }

    @Test
    public void handleRequestSerializesObjectsOmittingEmptyValuesWhenSuchMapperHasBeenSet()
        throws JsonProcessingException, IntrospectionException {
        final InputStream input = IoUtils.stringToStream(VALID_AWS_EVENT_BRIDGE_EVENT);
        ObjectMapper injectedMapper = JsonUtils.dynamoObjectMapper;
        DestinationsHandlerTestClass handler = new DestinationsHandlerTestClass(injectedMapper);
        handler.handleRequest(input, outputStream, context);
        ObjectNode outputObject = (ObjectNode) injectedMapper.readTree(outputStream.toString());
        assertThatJsonNodeDoesNotContainEmptyFields(outputObject);
    }

    private SampleEventDetail extractInputFromValidAwsEventBridgeEvent(String awsEventBridgeEvent)
        throws JsonProcessingException {
        JsonNode inputNode = extractResponseObjectFromAwsEventBridgeEvent(awsEventBridgeEvent);
        return dtoObjectMapper.convertValue(inputNode, SampleEventDetail.class);
    }

    private ObjectNode extractResponseObjectFromAwsEventBridgeEvent(String awsEventBridgeEvent)
        throws JsonProcessingException {
        ObjectNode tree = (ObjectNode) dtoObjectMapper.readTree(awsEventBridgeEvent);
        return (ObjectNode) tree.at(RESPONSE_PAYLOAD_POINTER);
    }

    private static class DestinationsHandlerTestClass
        extends DestinationsEventBridgeEventHandler<SampleEventDetail, SampleEventDetail> {

        private final AtomicReference<SampleEventDetail> inputBuffer = new AtomicReference<>();
        private final AtomicReference<AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>>> eventBuffer =
            new AtomicReference<>();

        protected DestinationsHandlerTestClass() {
            super(SampleEventDetail.class);
        }

        protected DestinationsHandlerTestClass(ObjectMapper objectMapper) {
            super(SampleEventDetail.class, objectMapper);
        }

        @Override
        protected SampleEventDetail processInputPayload(
            SampleEventDetail input,
            AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>> event,
            Context context
        ) {
            this.inputBuffer.set(input);
            this.eventBuffer.set(event);
            return SampleEventDetail.eventWithEmptyFields();
        }
    }
}