package no.unit.nva.stubs;

import static java.util.Objects.isNull;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@JacocoGenerated
public class FakeS3AsyncClient implements S3AsyncClient {

    private final Map<URI, byte[]> content;

    public FakeS3AsyncClient() {
        this.content = new HashMap<>();
    }

    @Override
    public String serviceName() {
        return "";
    }

    @Override
    public S3Utilities utilities() {
        return S3Utilities.builder().region(Region.EU_WEST_1).build();
    }

    @Override
    public <T> CompletableFuture<T> getObject(GetObjectRequest getObjectRequest,
                                                          AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        var bucket = getObjectRequest.bucket();
        var key = getObjectRequest.key();
        var uri = createS3Uri(bucket, key);
        var data = content.get(uri);
        if (isNull(data)) {
            return CompletableFuture.failedFuture(
                NoSuchKeyException.builder().message("The specified key does not exist: " + key).build());
        }
        var response = GetObjectResponse.builder().build();
        var responseBytes = ResponseBytes.fromByteArray(response, data);
        return CompletableFuture.completedFuture( (T) responseBytes);
    }

    @Override
    public void close() {

    }

    public void put(URI uri, String content) {
        this.content.put(uri, content.getBytes(StandardCharsets.UTF_8));
    }

    public void put(URI uri, byte[] content) {
        this.content.put(uri, content);
    }

    private URI createS3Uri(String bucket, String key) {
        return URI.create("s3://%s/%s".formatted(bucket, key));
    }
}
