package no.unit.nva.s3;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.javafaker.Faker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

class S3DriverTest {

    public static final Faker FAKER = Faker.instance();
    public static final String SAMPLE_BUCKET = "sampleBucket";
    public static final boolean NOT_LAST_OBJECT = false;
    public static final boolean LAST_OBJECT = true;
    public static final String REMOTELY_EXISTING_BUCKET = "orestis-export";
    public static final int REQUEST_BODY_ARG_INDEX = 1;
    private static final String FIRST_EXPECTED_OBJECT_KEY = randomFileName();
    private static final String SECOND_EXPECTED_OBJECT_KEY = randomFileName();
    private static final String EXPECTED_CONTENT = randomString();
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String PUT_ITEM_EXPECTED_PATH =
        constructPath("some", "nested", "path", randomFileName());
    private static final int PUT_OBJECT_REQUEST_ARG_INDEX = 0;
    public static final String SOME_PATH = "somePath";
    private S3Driver s3Driver;
    private String actualPutObjectContent;
    private String actualPutObjectKey;

    public static String streamToString(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(readInputStreamUsingUtf8(stream))) {
            return reader.lines().collect(Collectors.joining(LINE_SEPARATOR));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    public void init() {
        S3Client s3Client = mockS3Client();
        s3Driver = new S3Driver(s3Client, SAMPLE_BUCKET);
    }

    @Test
    @Tag("RemoteTests")
    public void listFilesReturnsListWithAllFilesInRemoteS3Folder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedFilename = constructNestedPath().toString();
        Path parentFolder = Path.of(expectedFilename).getParent();
        String content = randomString();
        s3Driver.insertFile(Path.of(expectedFilename), content);
        List<String> files = s3Driver.listFiles(Path.of(S3Driver.pathToString(parentFolder)));
        assertThat(files, hasItem(expectedFilename));
    }

    @Test
    @Tag("RemoteTest")
    public void getFileReturnsFileWhenFileExistsInRemoteFolder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = randomString();
        Path expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        String actualContent = s3Driver.getFile(expectedFilePath);
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    @Tag("RemoteTest")
    public void getFilesReturnsTheContentsOfAllFilesInARemoteFolder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = randomString();
        Path expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        List<String> actualContent = s3Driver.getFiles(expectedFilePath);

        assertThat(actualContent.size(), is(equalTo(1)));
        assertThat(actualContent, hasItem(expectedContent));
    }

    @Test
    public void listFilesReturnsListWithAllFilesInS3Folder() {
        List<String> files = s3Driver.listFiles(Path.of(SOME_PATH));
        assertThat(files, contains(FIRST_EXPECTED_OBJECT_KEY, SECOND_EXPECTED_OBJECT_KEY));
    }

    @Test
    public void getFileReturnsFileWhenFileExists() {
        Path somePath = Path.of(SOME_PATH);
        String actualContent = s3Driver.getFile(somePath);
        assertThat(actualContent, is(equalTo(EXPECTED_CONTENT)));
    }

    @Test
    public void insertFileInsertsObjectEncodedInUtf8() {
        s3Driver.insertFile(Path.of(PUT_ITEM_EXPECTED_PATH), EXPECTED_CONTENT);
        assertThat(actualPutObjectContent, is(equalTo(EXPECTED_CONTENT)));
        assertThat(actualPutObjectKey, is(equalTo(PUT_ITEM_EXPECTED_PATH)));
    }

    private static String constructPath(String first, String... more) {
        return S3Driver.pathToString(Path.of(first, more));
    }

    private static String randomFileName() {
        return FAKER.file().fileName();
    }

    private static String randomString() {
        return FAKER.lorem().sentence();
    }

    private static InputStreamReader readInputStreamUsingUtf8(InputStream stream) {
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    private static String parseContentAsUtf8String(RequestBody content) {
        InputStream contentSteam = content.contentStreamProvider().newStream();
        return streamToString(contentSteam);
    }

    private Path constructNestedPath() {
        String expectedFileName = randomFileName();
        Path parentFolder = Path.of("some", "nested", "path");
        return parentFolder.resolve(Path.of(expectedFileName));
    }

    private S3Client mockS3Client() {
        S3Client s3Client = mock(S3Client.class);
        setupObjectListing(s3Client);
        setupGetObject(s3Client);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenAnswer(this::extractInformationFromPutObjectRequest);
        return s3Client;
    }

    private PutObjectResponse extractInformationFromPutObjectRequest(InvocationOnMock invocation) {
        PutObjectRequest request = invocation.getArgument(PUT_OBJECT_REQUEST_ARG_INDEX);
        RequestBody content = invocation.getArgument(REQUEST_BODY_ARG_INDEX);
        actualPutObjectContent = parseContentAsUtf8String(content);
        actualPutObjectKey = request.key();
        return PutObjectResponse.builder().build();
    }

    private void setupGetObject(S3Client s3Client) {
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
            .thenAnswer(invocation -> returnPredefinedContent());
    }

    private ResponseBytes<GetObjectResponse> returnPredefinedContent() {
        return ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(),
            EXPECTED_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    private void setupObjectListing(S3Client s3Client) {
        when(s3Client.listObjects(any(ListObjectsRequest.class)))
            .thenAnswer(invocation -> listObjectsResponseWithSingleObject(FIRST_EXPECTED_OBJECT_KEY, NOT_LAST_OBJECT))
            .thenAnswer(invocation -> listObjectsResponseWithSingleObject(SECOND_EXPECTED_OBJECT_KEY, LAST_OBJECT));
    }

    private ListObjectsResponse listObjectsResponseWithSingleObject(String firstExpectedObjectKey, boolean lastObject) {
        S3Object responseObject = sampleObjectListing(firstExpectedObjectKey);
        return ListObjectsResponse.builder()
                   .contents(responseObject)
                   .isTruncated(!lastObject)
                   .build();
    }

    private S3Object sampleObjectListing(String firstExpectedObjectKey) {
        return S3Object.builder().key(firstExpectedObjectKey).build();
    }
}