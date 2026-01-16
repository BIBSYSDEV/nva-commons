package no.unit.nva.s3.async;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public final class S3DriverAsync {

    private static final int MAX_CONCURRENT_S3_READS = 200;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5;
    private static final int DEFAULT_READ_TIMEOUT = 60;
    private static final String DECOMPRESSION_FAILURE_MESSAGE = "Failed to decompress gzip content";
    private static final String GZIP_EXTENSION = ".gz";

    private final S3AsyncClient s3AsyncClient;
    private final S3Utilities s3Utilities;

    public S3DriverAsync(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Utilities = s3AsyncClient.utilities();
    }

    @JacocoGenerated
    public static S3DriverAsync defaultDriver() {
        var s3AsyncClient = S3AsyncClient.builder()
                                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                                                       .maxConcurrency(MAX_CONCURRENT_S3_READS)
                                                       .connectionTimeout(
                                                           Duration.ofSeconds(DEFAULT_CONNECTION_TIMEOUT))
                                                       .readTimeout(Duration.ofSeconds(DEFAULT_READ_TIMEOUT)))
                                .build();

        return new S3DriverAsync(s3AsyncClient);
    }

    /**
     * Read multiple files from S3 URIs and return string content. Automatically detects and handles gzipped files based
     * on file extension.
     *
     * @param uriList Collection of S3 URIs
     * @return List of fetch results containing file content as strings
     */
    public List<S3FetchResult<String>> readFiles(Collection<URI> uriList) {
        var s3UriList = convertToS3Uri(uriList);
        var futures = s3UriList.distinct().map(this::fetchFileAsync).toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private static void ensureKeyIsPresent(URI uri) {
        Optional.ofNullable(uri.getPath())
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new IllegalArgumentException("No key in URI: " + uri));
    }

    private static void ensureBucketIsPresent(URI uri) {
        Optional.ofNullable(uri.getHost())
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new IllegalArgumentException("No bucket in URI: " + uri));
    }

    private static Optional<String> getKey(S3Uri s3Uri) {
        return s3Uri.key();
    }

    private static Optional<String> getBucket(S3Uri s3Uri) {
        return s3Uri.bucket();
    }

    private static GetObjectRequest getObjectRequest(String bucket, String key) {
        return GetObjectRequest.builder().bucket(bucket).key(key).build();
    }

    private Stream<S3Uri> convertToS3Uri(Collection<URI> uriList) {
        return uriList.stream().distinct().map(this::convertToS3Uri);
    }

    private S3Uri convertToS3Uri(URI uri) {
        ensureBucketIsPresent(uri);
        ensureKeyIsPresent(uri);
        return s3Utilities.parseUri(uri);
    }

    private CompletableFuture<S3FetchResult<String>> fetchFileAsync(S3Uri s3Uri) {
        var bucket = getBucket(s3Uri).orElseThrow();
        var key = getKey(s3Uri).orElseThrow();
        return s3AsyncClient.getObject(getObjectRequest(bucket, key), AsyncResponseTransformer.toBytes())
                   .thenApply(response -> getS3ObjectAsString(s3Uri.uri(), response))
                   .thenApply(value -> createSuccessResult(s3Uri.uri(), value))
                   .exceptionally(exception -> createFailureResult(s3Uri.uri(), (Exception) exception));
    }

    private S3FetchResult<String> createFailureResult(URI uri, Exception exception) {
        return new S3FetchResult<>(uri, null, exception, false);
    }

    private S3FetchResult<String> createSuccessResult(URI uri, String content) {
        return new S3FetchResult<>(uri, content, null, true);
    }

    private String getS3ObjectAsString(URI uri, ResponseBytes<GetObjectResponse> response) {
        return isCompressed(uri) ? readCompressedStream(response.asByteArray())
                   : new String(response.asByteArray(), UTF_8);
    }

    private boolean isCompressed(URI uri) {
        return uri.toString().endsWith(GZIP_EXTENSION);
    }

    private String readCompressedStream(byte[] gzippedBytes) {
        try (var gzipStream = new GZIPInputStream(
            new ByteArrayInputStream(gzippedBytes)); var reader = new BufferedReader(
            new InputStreamReader(gzipStream, UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new UncheckedIOException(DECOMPRESSION_FAILURE_MESSAGE, e);
        }
    }
}
