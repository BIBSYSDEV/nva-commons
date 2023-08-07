package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.model.FirehoseException;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordRequest;
import software.amazon.awssdk.services.firehose.model.Record;

public class FakeFirehoseClientTest {

    private FakeFirehoseClient client;
    private ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    public void init() {
        this.client = new FakeFirehoseClient();
    }

    @Test
    void shouldReturnTheRecordPushedToFirehoseAsIs() {
        var expectedRecord = randomRecord();
        var request = PutRecordRequest.builder().record(expectedRecord).build();
        client.putRecord(request);
        var actualRecord = client.getRecords().stream().collect(SingletonCollector.collect());
        assertThat(actualRecord, is(equalTo(expectedRecord)));
    }

    @Test
    void shouldReturnAllRecordsWhenPushingABatchOfRecords() {
        var expectedRecords = List.of(randomRecord(), randomRecord());

        var request = PutRecordBatchRequest.builder()
                          .records(expectedRecords)
                          .build();
        client.putRecordBatch(request);
        var actualRecords = client.getRecords();
        assertThat(actualRecords, contains(expectedRecords.toArray(Record[]::new)));
    }

    @Test
    void shouldProvideParsedEmittedContent() {
        var expectedContent = randomJsons();
        var records = createRecords(expectedContent);

        var request = PutRecordBatchRequest.builder().records(records).build();
        client.putRecordBatch(request);
        var actualContent = client.extractPushedContent(this::parseString).collect(Collectors.toList());

        assertThat(actualContent, contains(expectedContent.toArray(JsonNode[]::new)));
    }

    @Test
    void shouldNotAcceptEmptyBatch() {
        var request = PutRecordBatchRequest.builder().records(Collections.emptyList()).build();
        assertThrows(FirehoseException.class, () -> client.putRecordBatch(request));
    }

    private static Record toRecord(String content) {
        return Record.builder().data(SdkBytes.fromString(content, StandardCharsets.UTF_8)).build();
    }

    private static Record randomRecord() {
        return toRecord(randomString());
    }

    private List<Record> createRecords(List<JsonNode> expectedContent) {
        return expectedContent.stream()
                   .map(attempt(OBJECT_MAPPER::writeValueAsString))
                   .flatMap(Try::stream)
                   .map(FakeFirehoseClientTest::toRecord)
                   .collect(Collectors.toList());
    }

    private List<JsonNode> randomJsons() {
        return Stream.of(randomJson(), randomJson())
                   .map(this::parseString)
                   .collect(Collectors.toList());
    }

    private JsonNode parseString(String jsonString) {
        return attempt(() -> OBJECT_MAPPER.readTree(jsonString)).orElseThrow();
    }
}
