package no.unit.nva.s3;

import static nva.commons.core.ioutils.IoUtils.pathToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.javafaker.Faker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import nva.commons.core.ioutils.IoUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

class S3DriverTest {

    private static final Faker FAKER = Faker.instance();
    private static final String SAMPLE_BUCKET = "sampleBucket";
    private static final boolean NOT_LAST_OBJECT = false;
    private static final boolean LAST_OBJECT = true;
    private static final String REMOTELY_EXISTING_BUCKET = "orestis-export";
    private static final int REQUEST_BODY_ARG_INDEX = 1;
    private static final String SOME_PATH = "somePath";
    private static final String FIRST_EXPECTED_OBJECT_KEY = randomFileName();
    private static final String SECOND_EXPECTED_OBJECT_KEY = randomFileName();
    private static final String EXPECTED_NON_COMPRESSED_CONTENT = randomString();
    private static final String PUT_ITEM_EXPECTED_PATH = pathToString(constructNestedPath());
    private static final int PUT_OBJECT_REQUEST_ARG_INDEX = 0;
    private static final String EXPECTED_COMPRESSED_CONTENT = randomString();
    private static final Integer NUMBER_OF_LISTED_ITEMS_IN_MOCKED_LISTING = 2;
    private static final String NOT_COMPRESSED_OBJECT_KEY = randomFileName();
    private static final String COMPRESSED_OBJECT_KEY = randomFileName() + S3Driver.GZIP_ENDING;

    private S3Driver s3Driver;
    private InputStream actualPutObjectContent;
    private String actualPutObjectKey;
    private S3Client s3Client;

    @BeforeEach
    public void init() throws IOException {
        s3Client = mockS3Client();
        s3Driver = new S3Driver(s3Client, SAMPLE_BUCKET);
    }

    @Test
    @Tag("RemoteTest")
    public void listFilesReturnsListWithAllFilenamesInRemoteS3Folder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedFilename = constructNestedPath().toString();
        Path parentFolder = Path.of(expectedFilename).getParent();
        String content = longText();
        s3Driver.insertFile(Path.of(expectedFilename), content);
        List<String> files = s3Driver.listFiles(Path.of(pathToString(parentFolder)));
        assertThat(files, CoreMatchers.hasItem(expectedFilename));
    }

    @Test
    @Tag("RemoteTest")
    public void getFileReturnsFileWhenFileExistsInRemoteFolder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = longText();
        Path expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        String actualContent = s3Driver.getFile(expectedFilePath).orElseThrow();
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    @Tag("RemoteTest")
    public void getFilesReturnsTheContentsOfAllFilesInARemoteFolder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = longText();
        Path expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        List<String> actualContent = s3Driver.getFiles(expectedFilePath);

        assertThat(actualContent.size(), is(equalTo(1)));
        assertThat(actualContent, CoreMatchers.hasItem(expectedContent));
    }

    @Test
    public void listFilesReturnsListWithAllFilenamesInS3Folder() {
        List<String> files = s3Driver.listFiles(Path.of(SOME_PATH));
        assertThat(files, contains(FIRST_EXPECTED_OBJECT_KEY, SECOND_EXPECTED_OBJECT_KEY));
    }

    @Test
    public void getFileReturnsFileWhenFileExists() {
        Path somePath = Path.of(SOME_PATH);
        String actualContent = s3Driver.getFile(somePath).orElseThrow();
        assertThat(actualContent, is(equalTo(EXPECTED_NON_COMPRESSED_CONTENT)));
    }

    @Test
    public void insertFileInsertsObjectEncodedInUtf8() {
        s3Driver.insertFile(Path.of(PUT_ITEM_EXPECTED_PATH), EXPECTED_NON_COMPRESSED_CONTENT);
        String actualContent = IoUtils.streamToString(actualPutObjectContent);
        assertThat(actualContent, is(equalTo(EXPECTED_NON_COMPRESSED_CONTENT)));
        assertThat(actualPutObjectKey, is(equalTo(PUT_ITEM_EXPECTED_PATH)));
    }

    @Test
    public void getCompressedFileReturnsContentsWhenInputIsCompressed() throws IOException {
        String compressedFilename = "compressed.gz";
        String result = s3Driver.getCompressedFile(Path.of(compressedFilename));
        assertThat(result, is(equalTo(EXPECTED_COMPRESSED_CONTENT)));
    }

    @Test
    public void getFilesReturnsTheContentsOfAllFilesInFolder() {
        when(s3Client.listObjects(any(ListObjectsRequest.class)))
            .thenAnswer(invocation -> listObjectsResponseWithSingleObject(NOT_COMPRESSED_OBJECT_KEY, NOT_LAST_OBJECT))
            .thenAnswer(invocation -> listObjectsResponseWithSingleObject(COMPRESSED_OBJECT_KEY, LAST_OBJECT));

        String expectedContent = randomString();
        Path expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        List<String> actualContent = s3Driver.getFiles(expectedFilePath);

        assertThat(actualContent.size(), is(equalTo(NUMBER_OF_LISTED_ITEMS_IN_MOCKED_LISTING)));
        assertThat(actualContent, contains(EXPECTED_NON_COMPRESSED_CONTENT, EXPECTED_COMPRESSED_CONTENT));
    }

    @Test
    public void insertFileWithInputStreamSendsDataToS3() throws IOException {
        String randomString = longText();
        InputStream inputStream = IoUtils.stringToStream(randomString);

        s3Driver.insertFile(Path.of("input"), inputStream);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(actualPutObjectContent));
        String actualContent = inputReader.lines().collect(Collectors.joining(S3Driver.LINE_SEPARATOR));

        assertThat(actualContent, is(equalTo(randomString)));
    }

    @Test
    public void insertFilesStoresAllFilesCompressedInGzipStream() throws IOException {
        List<String> input = List.of(longText(), longText(), longText());
        s3Driver.insertFiles(input);
        BufferedReader inputReader =
            new BufferedReader(new InputStreamReader(new GZIPInputStream(actualPutObjectContent)));
        List<String> actualContent = inputReader.lines().collect(Collectors.toList());
        assertThat(actualContent, is(equalTo(input)));
        ;
    }

    private static String randomFileName() {
        return FAKER.file().fileName();
    }

    private static String randomString() {
        return FAKER.lorem().word();
    }

    private static InputStream requestContentStream(RequestBody content) {
        return content.contentStreamProvider().newStream();
    }

    private static Path constructNestedPath() {
        String expectedFileName = randomFileName();
        Path parentFolder = Path.of("some", "nested", "path");
        return parentFolder.resolve(Path.of(expectedFileName));
    }

    private static void writeStringToCompressedFile(File tempFile) throws IOException {
        try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
            new GZIPOutputStream(new FileOutputStream(tempFile)), StandardCharsets.UTF_8)) {
            outputStreamWriter.write(EXPECTED_COMPRESSED_CONTENT);
            outputStreamWriter.flush();
        }
    }

    private String longText() {
        return FAKER.lorem().paragraph(10);
    }

    private S3Client mockS3Client() throws IOException {
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
        actualPutObjectContent = requestContentStream(content);
        actualPutObjectKey = request.key();
        return PutObjectResponse.builder().build();
    }

    private void setupGetObject(S3Client s3Client) throws IOException {
        setupGetUncompressedObject(s3Client);
        setupGetCompressObject(s3Client);
    }

    @SuppressWarnings("unchecked")
    private void setupGetUncompressedObject(S3Client s3Client) {
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
            .thenAnswer(invocation -> returnPredefinedContent());
    }

    private void setupGetCompressObject(S3Client s3Client) throws IOException {
        when(s3Client.getObject(any(GetObjectRequest.class)))
            .thenReturn(s3InputStreamWithCompressedContents());
    }

    private ResponseInputStream<GetObjectResponse> s3InputStreamWithCompressedContents() throws IOException {
        return new ResponseInputStream<>(GetObjectResponse.builder().build(),
                                         AbortableInputStream.create(zippedStream()));
    }

    private ResponseBytes<GetObjectResponse> returnPredefinedContent() {
        return ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(),
            EXPECTED_NON_COMPRESSED_CONTENT.getBytes(StandardCharsets.UTF_8));
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

    private InputStream zippedStream() throws IOException {
        File tempFile = createTempFile();
        writeStringToCompressedFile(tempFile);
        return inputStreamToCompressedFile(tempFile);
    }

    private FileInputStream inputStreamToCompressedFile(File tempFile) throws FileNotFoundException {
        return new FileInputStream(tempFile);
    }

    private File createTempFile() {
        File tempFile = new File("compressedFile.gz");
        if (tempFile.exists()) {
            tempFile.delete();
        }
        tempFile.deleteOnExit();
        return tempFile;
    }
}