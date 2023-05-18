package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import java.nio.charset.StandardCharsets;
import java.util.List;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordRequest;
import software.amazon.awssdk.services.firehose.model.Record;

public class FakeFirehoseClientTest {

    private FakeFirehoseClient client;

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

    private static Record randomRecord() {
        return Record.builder().data(randomContent()).build();
    }

    private static SdkBytes randomContent() {
        return SdkBytes.fromString(randomString(), StandardCharsets.UTF_8);
    }
}
