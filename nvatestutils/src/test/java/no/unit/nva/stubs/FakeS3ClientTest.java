package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

class FakeS3ClientTest {

    public static final URI SOME_URI = URI.create("s3://bucket/some/path/file.txt");
    public static final String SOME_BUCKET = "somebucket";
    public static final String SOME_BUCKET_URI = "s3://" + SOME_BUCKET;
    public static final int SOME_PAGE_SIZE = 10;
    public static final String LIST_FROM_BEGINNING = null;

    private static ListObjectsRequest createListObjectsRequest(String bucket,
                                                               UnixPath folder,
                                                               int pageSize,
                                                               String listingStartPoint) {
        return ListObjectsRequest.builder()
                .bucket(bucket)
                .prefix(folder.toString())
                .maxKeys(pageSize)
                .marker(listingStartPoint)
                .build();
    }

    private static ListObjectsRequest createListObjectsRequest(String bucket, UnixPath folder) {
        return createListObjectsRequest(bucket, folder, SOME_PAGE_SIZE, LIST_FROM_BEGINNING);
    }

    private static UnixPath insertFileToS3UnderSubfolder(S3Client s3Client,
                                                         String bucket,
                                                         UnixPath subfolder) {
        var filePath = subfolder.addChild(randomString()).addChild(randomString());
        var putRequest = insertFileToS3(bucket, filePath);
        s3Client.putObject(putRequest, RequestBody.fromBytes(randomString().getBytes()));
        return filePath;
    }

    private static UnixPath insertRandomFileToS3(S3Client s3Client, String bucket) {
        var filePath = UnixPath.of(randomString(), randomString(), randomString());
        var putRequest = insertFileToS3(bucket, filePath);
        s3Client.putObject(putRequest, RequestBody.fromBytes(randomString().getBytes()));
        return filePath;
    }

    private static PutObjectRequest insertFileToS3(String bucket, UnixPath filePath) {
        return PutObjectRequest.builder()
                .bucket(bucket)
                .key(filePath.toString())
                .build();
    }

    private static ArrayList<String> fetchAllExpectedFilesUsingPagination(FakeS3Client s3Client,
                                                                          String bucket,
                                                                          UnixPath expectedFolder) {
        final int smallPage = 3;

        var listObjectRequest =
                createListObjectsRequest(bucket, expectedFolder, smallPage, LIST_FROM_BEGINNING);
        var listingResult = s3Client.listObjects(listObjectRequest);
        var allListedKeys = new ArrayList<>(extractListedKeys(listingResult));

        while (listingResult.isTruncated()) {
            listObjectRequest =
                    createListObjectsRequest(bucket, expectedFolder, smallPage, listingResult.marker());
            listingResult = s3Client.listObjects(listObjectRequest);
            allListedKeys.addAll(extractListedKeys(listingResult));
        }
        return allListedKeys;
    }

    private static List<String> extractListedKeys(ListObjectsResponse listingResult) {
        return listingResult.contents()
                .stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    @Test
    void putObjectMakesContentAvailableForGetting() {
        FakeS3Client fakeS3Client = new FakeS3Client(new ConcurrentHashMap<>());

        String expectedContent = randomString();

        putObject(fakeS3Client, SOME_URI, expectedContent);
        ResponseBytes<GetObjectResponse> result = getObject(fakeS3Client, SOME_URI);
        String actualContent = result.asUtf8String();
        assertThat(actualContent, is(equalTo(expectedContent)));
    }

    @Test
    void putObjectDoesNotAlterInputDataToFakeS3Client() {
        Map<String, InputStream> inputData = new ConcurrentHashMap<>();
        inputData.put(randomString(), IoUtils.stringToStream(randomString()));
        Map<String, InputStream> inputDataCopy = new ConcurrentHashMap<>(inputData);
        FakeS3Client fakeS3Client = FakeS3Client.fromContentsMap(inputData);
        putObject(fakeS3Client, SOME_URI, randomString());
        assertThat(inputData, is(equalTo(inputDataCopy)));
    }

    @Test
    void shouldListInsertedFilesByInsertionOrder() {
        var sampleFilenames = createLargeSetOfRandomFilenames();
        var s3Client = new FakeS3Client();
        var listObjectsRequest = insertFilesToBucketInOrder(sampleFilenames, s3Client);
        var response = s3Client.listObjects(listObjectsRequest);
        var actualFilenames = extractFilenamesFromResponse(response);

        assertThat(actualFilenames, contains(sampleFilenames.toArray(String[]::new)));
    }

    @Test
    void shouldListFilesForSpecifiedFolder() {
        var s3Client = new FakeS3Client();
        var bucket = randomString();
        var expectedFile = insertRandomFileToS3(s3Client, bucket);
        var unexpectedFile = insertRandomFileToS3(s3Client, bucket);

        var listObjectRequest = createListObjectsRequest(bucket, expectedFile.getParent().orElseThrow());
        var result = s3Client.listObjects(listObjectRequest);
        var actualFilename = extractListedKeys(result);
        assertThat(actualFilename, contains(expectedFile.toString()));
        assertThat(actualFilename, not(contains(unexpectedFile.toString())));
    }

    @Test
    void shouldReturnNextListingStartPointWhenReturningAPageOfResults() {
        var s3Client = new FakeS3Client();
        var bucket = randomString();
        var expectedFile1 = insertRandomFileToS3(s3Client, bucket);
        var expectedFile2 = insertRandomFileToS3(s3Client, bucket);

        var listObjectRequest = createListObjectsRequest(bucket,
                expectedFile1.getParent().orElseThrow(), 1, LIST_FROM_BEGINNING);
        var result = s3Client.listObjects(listObjectRequest);
        assertThat(result.marker(), is(equalTo(expectedFile1.toString())));
    }

    @Test
    void shouldReturnNextListingStartPointWhenReturningAPageOfResultsWithVersion2Request() {
        var s3Client = new FakeS3Client();
        var bucket = randomString();
        var expectedFile1 = insertRandomFileToS3(s3Client, bucket);
        var expectedFile2 = insertRandomFileToS3(s3Client, bucket);

        var listObjectRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(expectedFile1.getParent().orElseThrow().toString())
                .maxKeys(1)
                .startAfter(LIST_FROM_BEGINNING)
                .build();
        var result = s3Client.listObjectsV2(listObjectRequest);
        assertThat(result.nextContinuationToken(), is(equalTo(expectedFile1.toString())));
    }

    @Test
    void shouldBeAbleToListALlFilesUsingPaginationWhenAlsoSpecifyingAPrefix() {
        var s3Client = new FakeS3Client();
        var bucket = randomString();
        var expectedFolder = UnixPath.of(randomString());
        var unexpectedFolder = UnixPath.of(randomString());
        int numberOfExpectedFiles = 10;
        createAMixOfExpectedAndUnexpectedFiles(s3Client, bucket, expectedFolder, unexpectedFolder, numberOfExpectedFiles);
        var listedFiles = fetchAllExpectedFilesUsingPagination(s3Client, bucket, expectedFolder);

        assertThat(listedFiles, everyItem(containsString(expectedFolder.toString())));
        assertThat(listedFiles.size(), is(equalTo(numberOfExpectedFiles)));

    }

    private void createAMixOfExpectedAndUnexpectedFiles(FakeS3Client s3Client,
                                                        String bucket,
                                                        UnixPath expectedFolder,
                                                        UnixPath unexpectedFolder,
                                                        int numberOfExpectedFiles) {

        for (int counter = 0; counter < numberOfExpectedFiles; counter++) {
            insertFileToS3UnderSubfolder(s3Client, bucket, expectedFolder);
            insertFileToS3UnderSubfolder(s3Client, bucket, unexpectedFolder);
        }

    }


    private List<String> extractFilenamesFromResponse(ListObjectsResponse response) {
        return response.contents().stream()
                .map(S3Object::key)
                .map(UnixPath::of)
                .map(UnixPath::removeRoot)
                .map(UnixPath::toString)
                .collect(Collectors.toList());
    }

    private ListObjectsRequest insertFilesToBucketInOrder(List<String> sampleFilenames,
                                                          FakeS3Client s3Client) {
        for (var filename : sampleFilenames) {
            putObject(s3Client, URI.create(SOME_BUCKET_URI + "/" + filename), randomString());
        }
        var listObjectsRequest = ListObjectsRequest.builder()
                .bucket(SOME_BUCKET)
                .maxKeys(sampleFilenames.size())
                .build();
        return listObjectsRequest;
    }

    private List<String> createLargeSetOfRandomFilenames() {
        return IntStream.range(0, 100)
                .boxed()
                .map(i -> randomString())
                .collect(Collectors.toList());
    }

    private ResponseBytes<GetObjectResponse> getObject(FakeS3Client fakeS3Client, URI s3Uri) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Uri.getHost())
                .key(s3Uri.getPath())
                .build();
        ResponseBytes<GetObjectResponse> result =
                fakeS3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());
        return result;
    }

    private void putObject(FakeS3Client fakeS3Client, URI s3Uri, String expectedContent) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Uri.getHost())
                .key(s3Uri.getPath())
                .build();

        fakeS3Client.putObject(putObjectRequest,
                RequestBody.fromBytes(expectedContent.getBytes(StandardCharsets.UTF_8)));
    }
}