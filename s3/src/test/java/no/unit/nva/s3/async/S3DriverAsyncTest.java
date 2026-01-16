package no.unit.nva.s3.async;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import no.unit.nva.stubs.FakeS3AsyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class S3DriverAsyncTest {

    private FakeS3AsyncClient client;
    private S3DriverAsync driver;

    @BeforeEach
    public void init() {
        client = new FakeS3AsyncClient();
        driver = new S3DriverAsync(client);
    }

    @Test
    void shouldThrowExceptionWhenConsumingUriWithoutS3Bucket() {
        var invalidS3Uri = URI.create(randomString());

        assertThrows(IllegalArgumentException.class, () -> driver.readFiles(List.of(invalidS3Uri)));
    }

    @Test
    void shouldThrowExceptionWhenConsumingUriWithoutS3Key() {
        var invalidS3Uri = URI.create("s3://bucket-name");

        assertThrows(IllegalArgumentException.class, () -> driver.readFiles(List.of(invalidS3Uri)));
    }

    @Test
    void shouldThrowExceptionWhenConsumingEmptyStringUri() {
        var invalidS3Uri = URI.create("");

        assertThrows(IllegalArgumentException.class, () -> driver.readFiles(List.of(invalidS3Uri)));
    }

    @Test
    void shouldReturnSuccessWhenResultIsReadSuccessfully() {
        var uri = insertContentToS3().uri();

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldReturnContentWhenResultIsReadSuccessfully() {
        var wrapper = insertContentToS3();

        var result = driver.readFiles(List.of(wrapper.uri())).getFirst();

        assertEquals(wrapper.content(), result.getContent().orElseThrow());
    }

    @Test
    void shouldReturnOptionalEmptyOnErrorWhenResultIsReadSuccessfully() {
        var uri = insertContentToS3().uri();

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertEquals(Optional.empty(), result.getError());
    }

    @Test
    void shouldReturnUriWhenResultIsReadSuccessfully() {
        var uri = insertContentToS3().uri();

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertEquals(uri, result.getUri());
    }

    @Test
    void shouldReturnFalseFailureWhenResultIsReadSuccessfully() {
        var uri = insertContentToS3().uri();

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertFalse(result.isFailure());
    }

    @Test
    void shouldReturnFailureWhenFileDoesNotExist() {
        var uri = randomS3Uri(randomString(), randomString());

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertTrue(result.isFailure());
    }

    @Test
    void shouldReturnEmptyContentWhenFileDoesNotExist() {
        var uri = randomS3Uri(randomString(), randomString());

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertEquals(Optional.empty(), result.getContent());
    }

    @Test
    void shouldReturnErrorWhenFileDoesNotExist() {
        var uri = randomS3Uri(randomString(), randomString());

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertTrue(result.getError().isPresent());
    }

    @Test
    void shouldReturnFalseSuccessWhenFileDoesNotExist() {
        var uri = randomS3Uri(randomString(), randomString());

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertFalse(result.isSuccess());
    }

    @Test
    void shouldDeduplicateUris() {
        var wrapper = insertContentToS3();
        var uri = wrapper.uri();

        var results = driver.readFiles(List.of(uri, uri));

        assertEquals(1, results.size());
    }

    @Test
    void shouldReturnContentWhenFileIsCompressed() {
        var uri = randomS3Uri(randomString(), "%s.gz".formatted(randomString()));
        var content = randomString();
        client.put(uri, compressContent(content));

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertEquals(content, result.getContent().orElseThrow());
    }

    @Test
    void shouldReturnFailureWhenGzipContentIsCorrupted() {
        var uri = randomS3Uri(randomString(), "%s.gz".formatted(randomString()));
        client.put(uri, randomString().getBytes(StandardCharsets.UTF_8));

        var result = driver.readFiles(List.of(uri)).getFirst();

        assertTrue(result.isFailure());
    }

    @Test
    void shouldReturnSuccessWhenS3UriHasTrailingSlash() {
        var wrapper = insertContentToS3(randomString() + "/");

        assertTrue(wrapper.uri().getPath().endsWith("/"));

        var result = driver.readFiles(List.of(wrapper.uri())).getFirst();

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldReadMultipleUris() {
        var results = driver.readFiles(List.of(insertContentToS3().uri(), insertContentToS3().uri()));

        assertEquals(2, results.size());
    }

    private static URI randomS3Uri(String bucket, String key) {
        return URI.create("s3://%s/%s".formatted(bucket, key));
    }

    private UriContentWrapper insertContentToS3(String key) {
        var uri = randomS3Uri(randomString(), key);
        var content = randomString();
        client.put(uri, content);
        return new UriContentWrapper(uri, content);
    }

    private UriContentWrapper insertContentToS3() {
        return insertContentToS3(randomString());
    }

    private byte[] compressContent(String content) {
        try (var byteStream = new ByteArrayOutputStream(); var gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(content.getBytes(StandardCharsets.UTF_8));
            gzipStream.finish();
            return byteStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record UriContentWrapper(URI uri, String content) {

    }
}