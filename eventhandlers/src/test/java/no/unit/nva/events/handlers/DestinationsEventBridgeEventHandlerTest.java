package no.unit.nva.events.handlers;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.stubs.FakeContext;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DestinationsEventBridgeEventHandlerTest {

    public static final String VALID_AWS_EVENT_BRIDGE_EVENT = IoUtils.stringFromResources(
        Path.of("validAwsEventBridgeEvent.json"));
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
        SampleEventDetail expectedInput = extractInputFromValidAwsEventBridgeEvent();
        assertThat(handler.inputBuffer.get(), is(equalTo(expectedInput)));
    }

    private SampleEventDetail extractInputFromValidAwsEventBridgeEvent() throws JsonProcessingException {
        JsonNode tree = dtoObjectMapper.readTree(VALID_AWS_EVENT_BRIDGE_EVENT);
        JsonNode inputNode = tree.at(RESPONSE_PAYLOAD_POINTER);
        return dtoObjectMapper.convertValue(inputNode, SampleEventDetail.class);
    }

    private static class DestinationsHandlerTestClass
        extends DestinationsEventBridgeEventHandler<SampleEventDetail, Void> {

        private final AtomicReference<SampleEventDetail> inputBuffer = new AtomicReference<>();
        private final AtomicReference<AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>>> eventBuffer =
            new AtomicReference<>();

        protected DestinationsHandlerTestClass() {
            super(SampleEventDetail.class);
        }

        @Override
        protected Void processInputPayload(SampleEventDetail input,
                                           AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>> event,
                                           Context context) {
            this.inputBuffer.set(input);
            this.eventBuffer.set(event);
            return null;
        }
    }
}