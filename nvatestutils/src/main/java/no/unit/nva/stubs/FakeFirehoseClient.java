package no.unit.nva.stubs;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordResponse;
import software.amazon.awssdk.services.firehose.model.Record;

/**
 * A Kinesis Firehose collects the records that are pushed to it and stores them in an S3 bucket. The important feature
 * is that it collects multiple records in ZIP files and organizes the ZIP files by timestamp:
 * s3://some-bucket/some-folder/2023/05/08/18/some-prefix-2023-05-08-18-01-17-lfksdjflskd-srdfsdef-sefsdf-sdfsd
 * -sgdfsdfsdfs.gz
 */
public class FakeFirehoseClient implements FirehoseClient {

    private final List<Record> records = new ArrayList<>();

    @Override
    public PutRecordResponse putRecord(PutRecordRequest putRecordRequest) {
        records.add(putRecordRequest.record());
        return PutRecordResponse.builder().build();
    }

    @Override
    public PutRecordBatchResponse putRecordBatch(PutRecordBatchRequest putRecordBatchRequest) {
        records.addAll(putRecordBatchRequest.records());
        return PutRecordBatchResponse.builder().build();
    }

    @JacocoGenerated
    @Override
    public String serviceName() {
        return "FakeFirehoseClient";
    }

    @Override
    @JacocoGenerated
    public void close() {
        //NO-OP
    }

    public List<Record> getRecords() {
        return this.records;
    }

    public Stream<String> extractPushedContent() {
        return this.getRecords().stream()
                   .map(Record::data)
                   .map(data -> data.asString(StandardCharsets.UTF_8));
    }

    public <T> Stream<T> extractPushedContent(Function<String, T> parser) {
        return extractPushedContent().map(parser::apply);
    }
}
