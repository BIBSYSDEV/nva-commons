package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

class FakeS3ClientTest {
    
    public static final URI SOME_URI = URI.create("s3://bucket/some/path/file.txt");
    public static final String SOME_BUCKET = "somebucket";
    public static final String SOME_BUCKET_URI = "s3://" + SOME_BUCKET;
    
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
        var actualFilename = result.contents().stream().map(S3Object::key).collect(Collectors.toList());
        assertThat(actualFilename, contains(expectedFile.toString()));
        assertThat(actualFilename, not(contains(unexpectedFile.toString())));
    }
    
    private static ListObjectsRequest createListObjectsRequest(String bucket, UnixPath folder) {
        return ListObjectsRequest.builder()
                   .bucket(bucket)
                   .prefix(folder.toString())
                   .maxKeys(10)
                   .build();
    }
    
    private static UnixPath insertRandomFileToS3(FakeS3Client s3Client, String bucket) {
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