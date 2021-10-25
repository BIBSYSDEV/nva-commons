package no.unit.nva.s3;

import static no.unit.nva.s3.S3Driver.S3_SCHEME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    public static final int LARGE_NUMBER_OF_INPUTS = 10_000;
    public static final String EMPTY_STRING = "";
    public static final String ROOT = "/";
    private static final Faker FAKER = Faker.instance();
    private static final String SAMPLE_BUCKET = "sampleBucket";
    private static final boolean NOT_LAST_OBJECT = false;
    private static final boolean LAST_OBJECT = true;
    private static final String REMOTELY_EXISTING_BUCKET = "orestis-export";
    private static final int REQUEST_BODY_ARG_INDEX = 1;
    private static final String SOME_PATH = "somePath";
    private static final UnixPath FIRST_EXPECTED_OBJECT_KEY = UnixPath.of(randomFileName());
    private static final UnixPath SECOND_EXPECTED_OBJECT_KEY = UnixPath.of(randomFileName());
    private static final String EXPECTED_NON_COMPRESSED_CONTENT = randomString();
    private static final String PUT_ITEM_EXPECTED_PATH = constructNestedPath().toString();
    private static final int PUT_OBJECT_REQUEST_ARG_INDEX = 0;
    private static final String EXPECTED_COMPRESSED_CONTENT = randomString();
    private static final Integer NUMBER_OF_LISTED_ITEMS_IN_MOCKED_LISTING = 2;
    private static final UnixPath NOT_COMPRESSED_OBJECT_KEY = UnixPath.of(randomFileName());
    private static final UnixPath COMPRESSED_OBJECT_KEY = UnixPath.of(randomFileName() + S3Driver.GZIP_ENDING);
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
    public void listAllFilesReturnsListWithAllFilenamesInRemoteS3Folder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        UnixPath expectedFilename = constructNestedPath();
        UnixPath parentFolder = expectedFilename.getParent().orElseThrow();
        String content = longText();
        s3Driver.insertFile(expectedFilename, content);
        List<UnixPath> files = s3Driver.listAllFiles(UnixPath.of(parentFolder.toString()));
        assertThat(files, CoreMatchers.hasItem(expectedFilename));
    }

    @Test
    @Tag("RemoteTest")
    public void listFilesReturnsPartialResultAndInformationForFetchingNextBatch() {

        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        UnixPath path = UnixPath.of("some/known/path");
        ListingResult result = s3Driver.listFiles(path, null, 100);
        List<UnixPath> totalResults = new ArrayList<>(result.getFiles());
        while (result.isTruncated()) {
            result = s3Driver.listFiles(path, result.getListingStartingPoint(), 100);
            totalResults.addAll(result.getFiles());
        }
        int knownSizeOfResult = 2000;
        assertThat(totalResults.size(), is(equalTo(knownSizeOfResult)));
    }

    @Test
    @Tag("RemoteTest")
    public void getUncompressedFileReturnsFileWhenFileExistsInRemoteFolder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = longText();
        UnixPath expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        String actualContent = s3Driver.getUncompressedFile(expectedFilePath).orElseThrow();
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    @Tag("RemoteTest")
    public void getFilesReturnsTheContentsOfAllFilesInARemoteFolder() {
        S3Driver s3Driver = new S3Driver(S3Client.create(), REMOTELY_EXISTING_BUCKET);
        String expectedContent = longText();
        UnixPath expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        List<String> actualContent = s3Driver.getFiles(expectedFilePath);

        assertThat(actualContent.size(), is(equalTo(1)));
        assertThat(actualContent, CoreMatchers.hasItem(expectedContent));
    }

    @Test
    public void listAllFilesReturnsListWithAllFilenamesInS3Folder() {
        List<UnixPath> files = s3Driver.listAllFiles(UnixPath.of(SOME_PATH));
        assertThat(files, contains(FIRST_EXPECTED_OBJECT_KEY, SECOND_EXPECTED_OBJECT_KEY));
    }

    @Test
    public void listFilesReturnsResultContainingPartialFileListAndNewListStartingPointAndSignForTerminatingListing() {
        ListingResult firstBatch = s3Driver.listFiles(UnixPath.of(SOME_PATH), null, 1);
        assertThat(firstBatch.getListingStartingPoint(), is(equalTo(FIRST_EXPECTED_OBJECT_KEY.toString())));
        assertThat(firstBatch.isTruncated(), is(true));

        ListingResult secondBatch = s3Driver.listFiles(UnixPath.of(SOME_PATH), FIRST_EXPECTED_OBJECT_KEY.toString(), 1);
        assertThat(secondBatch.getListingStartingPoint(), is(equalTo(SECOND_EXPECTED_OBJECT_KEY.toString())));
        assertThat(secondBatch.isTruncated(), is(false));
    }

    @Test
    public void getFileReturnsFileWhenFileExists() {
        UnixPath somePath = UnixPath.of(SOME_PATH);
        String actualContent = s3Driver.getUncompressedFile(somePath).orElseThrow();
        assertThat(actualContent, is(equalTo(EXPECTED_NON_COMPRESSED_CONTENT)));
    }

    @Test
    public void insertFileInsertsObjectEncodedInUtf8() {
        s3Driver.insertFile(UnixPath.of(PUT_ITEM_EXPECTED_PATH), EXPECTED_NON_COMPRESSED_CONTENT);
        String actualContent = IoUtils.streamToString(actualPutObjectContent);
        assertThat(actualContent, is(equalTo(EXPECTED_NON_COMPRESSED_CONTENT)));
        assertThat(actualPutObjectKey, is(equalTo(PUT_ITEM_EXPECTED_PATH)));
    }

    @Test
    public void getCompressedFileReturnsContentsWhenInputIsCompressed() throws IOException {
        String compressedFilename = "compressed.gz";
        GZIPInputStream contents = s3Driver.getCompressedFile(UnixPath.of(compressedFilename));
        BufferedReader reader = new BufferedReader(new InputStreamReader(contents));
        String result = reader.lines().collect(Collectors.joining());
        assertThat(result, is(equalTo(EXPECTED_COMPRESSED_CONTENT)));
    }

    @Test
    public void getFilesReturnsTheContentsOfAllFilesInFolder() {
        when(s3Client.listObjects(any(ListObjectsRequest.class)))
            .thenAnswer(invocation -> listObjectsResponseWithSingleObject(NOT_COMPRESSED_OBJECT_KEY, NOT_LAST_OBJECT))
            .thenAnswer(invocation -> listObjectsResponseWithSingleObject(COMPRESSED_OBJECT_KEY, LAST_OBJECT));

        String expectedContent = randomString();
        UnixPath expectedFilePath = constructNestedPath();
        s3Driver.insertFile(expectedFilePath, expectedContent);
        List<String> actualContent = s3Driver.getFiles(expectedFilePath);

        assertThat(actualContent.size(), is(equalTo(NUMBER_OF_LISTED_ITEMS_IN_MOCKED_LISTING)));
        assertThat(actualContent, contains(EXPECTED_NON_COMPRESSED_CONTENT, EXPECTED_COMPRESSED_CONTENT));
    }

    @Test
    public void insertFileWithInputStreamSendsDataToS3() throws IOException {
        String randomString = longText();
        InputStream inputStream = IoUtils.stringToStream(randomString);

        s3Driver.insertFile(UnixPath.of("input"), inputStream);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(actualPutObjectContent));
        String actualContent = inputReader.lines().collect(Collectors.joining(S3Driver.LINE_SEPARATOR));

        assertThat(actualContent, is(equalTo(randomString)));
    }

    @Test
    public void insertAndCompressFilesFilesStoresAllFilesCompressedInGzipStream() throws IOException {
        List<String> input = new ArrayList<>();
        for (int i = 0; i < LARGE_NUMBER_OF_INPUTS; i++) {
            input.add(longText());
        }
        s3Driver.insertAndCompressFiles(input);
        BufferedReader inputReader =
            new BufferedReader(new InputStreamReader(new GZIPInputStream(actualPutObjectContent)));
        List<String> actualContent = inputReader.lines().collect(Collectors.toList());
        assertThat(actualContent, is(equalTo(input)));
    }

    @Test
    public void shouldReturnUriToS3FileLocationWhenSavingEvent() {
        String content = randomString();
        UnixPath someFolder = UnixPath.of("parent", "child1", "child2");
        URI fileLocation = s3Driver.insertEvent(someFolder, content);
        String randomFilename = new UriWrapper(fileLocation).getFilename();
        assertThat(fileLocation.getScheme(), is(equalTo(S3_SCHEME)));
        assertThat(fileLocation.getHost(), is(equalTo(SAMPLE_BUCKET)));
        assertThat(fileLocation.getPath(), is(equalTo("/parent/child1/child2/" + randomFilename)));
    }

    @ParameterizedTest(name = "insertAndCompressFilesStoresAllFilesCompressedInGzipStreamInSpecifiedFolder")
    @ValueSource(strings = {EMPTY_STRING, ROOT})
    public void insertAndCompressFilesStoresAllFilesCompressedInGzipStreamInSpecifiedFolder(String pathPrefix)
        throws IOException {
        List<String> input = new ArrayList<>();
        for (int i = 0; i < LARGE_NUMBER_OF_INPUTS; i++) {
            input.add(longText());
        }
        String expectedFolderPath = "parent/child/";
        s3Driver.insertAndCompressFiles(UnixPath.of(pathPrefix + expectedFolderPath), input);
        BufferedReader inputReader =
            new BufferedReader(new InputStreamReader(new GZIPInputStream(actualPutObjectContent)));
        List<String> actualContent = inputReader.lines().collect(Collectors.toList());
        assertThat(actualContent, is(equalTo(input)));
        assertThat(actualPutObjectKey, startsWith(expectedFolderPath));
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

    private static UnixPath constructNestedPath() {
        UnixPath expectedFileName = UnixPath.of(randomFileName());
        UnixPath parentFolder = UnixPath.of("some", "nested", "path");
        return parentFolder.addChild(expectedFileName);
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

    private ListObjectsResponse listObjectsResponseWithSingleObject(UnixPath listingStartingPoint,
                                                                    boolean lastObject) {
        S3Object responseObject = sampleObjectListing(listingStartingPoint);
        return ListObjectsResponse.builder()
            .contents(responseObject)
            .isTruncated(!lastObject)
            .build();
    }

    private S3Object sampleObjectListing(UnixPath firstExpectedObjectKey) {
        return S3Object.builder().key(firstExpectedObjectKey.toString()).build();
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