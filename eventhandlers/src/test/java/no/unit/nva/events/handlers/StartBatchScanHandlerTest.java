package no.unit.nva.events.handlers;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.events.models.AbstractScanDatabaseRequest;
import no.unit.nva.events.models.ScanDatabaseRequest;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

class StartBatchScanHandlerTest {

    public static final int MAX_PAGE_SIZE_FOR_DYNAMO = 1000;
    public static final Integer NO_PAGE_SIZE = null;
    public static final Map<String, AttributeValue> NO_START_MARKER = null;
    private static final String EMPTY_JSON = "{}";
    private StartBatchScanHandler startBatchScanHandler;
    private String eventTopicSetByHandler;
    private InputStream input;
    private ByteArrayOutputStream output;
    private FakeEventBridgeClient eventBridgeClient;
    private Integer randomPageSize;
    private Map<String, AttributeValue> randomStartMarker;

    @BeforeEach
    public void init() {
        eventBridgeClient = new FakeEventBridgeClient();
        this.eventTopicSetByHandler = randomString();
        this.input = IoUtils.stringToStream(EMPTY_JSON);
        this.output = new ByteArrayOutputStream();
        this.randomPageSize = positivePageSizeWithinDynamosPageLimits();
        this.randomStartMarker = dynamoDbScanStartMarker();
        startBatchScanHandler = newStartBatchHandler(eventBridgeClient);
    }

    @Test
    void shouldEmitEventWithSpecifiedTopic() throws IOException {
        startBatchScanHandler.handleRequest(input, output, mock(Context.class));
        var actualEmittedTopic = getEmittedScanRequest().getTopic();
        assertThat(actualEmittedTopic, is(equalTo(eventTopicSetByHandler)));
    }

    @Test
    void shouldEmitScanRequestWithNullMarkerWhenScanMarkerIsNotPresent() throws IOException {
        startBatchScanHandler.handleRequest(input, output, mock(Context.class));
        var actualStartMarker = getEmittedScanRequest().getStartMarker();
        assertThat(actualStartMarker, is(nullValue()));
    }

    @Test
    void shouldEmitScanRequestWithRequestedPageSizeWhenPageSizeIsSet() throws IOException {
        input = scanDatabaseRequestWithPageSize();
        startBatchScanHandler.handleRequest(input, output, mock(Context.class));
        var actualPageSize = getEmittedScanRequest().getPageSize();
        assertThat(actualPageSize, is(equalTo(randomPageSize)));
    }

    @Test
    void shouldEmitScanRequestWithRequestedStartMarkerWhenStartMarkerIsSet() throws IOException {
        input = scanDatabaseRequestWithStartMarker();
        startBatchScanHandler.handleRequest(input, output, mock(Context.class));
        var actualStartMarker = getEmittedScanRequest().getStartMarker();
        assertThat(actualStartMarker, is(equalTo(randomStartMarker)));
    }

    @Test
    void shouldEmitScanRequestWithTopicDecidedByHandlerAndNotByUser() throws IOException {
        String userDefinedTopic = randomString();
        input = scanDatabaseRequestWithCustomTopic(userDefinedTopic);
        startBatchScanHandler.handleRequest(input, output, mock(Context.class));
        var actualTopic = getEmittedScanRequest().getTopic();
        assertThat(actualTopic, is(not(equalTo(userDefinedTopic))));
        assertThat(actualTopic, is(equalTo(eventTopicSetByHandler)));
    }

    private Map<String, AttributeValue> dynamoDbScanStartMarker() {
        return Map.of(randomString(), new AttributeValue().withS(randomString()),
                      randomString(), new AttributeValue().withS(randomString()));
    }

    private int positivePageSizeWithinDynamosPageLimits() {
        return 1 + randomInteger(MAX_PAGE_SIZE_FOR_DYNAMO - 1);
    }

    private InputStream scanDatabaseRequestWithCustomTopic(String userDefinedTopic) {
        var request = new ScanDatabaseRequest(userDefinedTopic, NO_PAGE_SIZE, NO_START_MARKER);
        return IoUtils.stringToStream(request.toJsonString());
    }

    private InputStream scanDatabaseRequestWithStartMarker() {
        var request = new ScanDatabaseRequest(randomString(), randomPageSize, randomStartMarker);
        return IoUtils.stringToStream(request.toJsonString());
    }

    private InputStream scanDatabaseRequestWithPageSize() {
        var request = new ScanDatabaseRequest(randomString(), randomPageSize, null);
        return IoUtils.stringToStream(request.toJsonString());
    }

    private StartBatchScanHandler newStartBatchHandler(FakeEventBridgeClient eventBridgeClient) {
        return new StartBatchScanHandler(eventBridgeClient) {
            @Override
            protected String getScanEventTopic() {
                return eventTopicSetByHandler;
            }
        };
    }

    private AbstractScanDatabaseRequest<AttributeValue> getEmittedScanRequest() {
        return eventBridgeClient.getRequestEntries().stream()
            .map(PutEventsRequestEntry::detail)
            .map(attempt(ScanDatabaseRequest::fromJson))
            .map(Try::orElseThrow)
            .collect(SingletonCollector.collect());
    }
}