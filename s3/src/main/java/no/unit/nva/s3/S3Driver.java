package no.unit.nva.s3;

import static nva.commons.core.attempt.Try.attempt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Driver {

    public static final String GZIP_ENDING = ".gz";
    public static final String AWS_ACCESS_KEY_ID_ENV_VARIABLE_NAME = "AWS_ACCESS_KEY_ID";
    public static final String AWS_SECRET_ACCESS_KEY_ENV_VARIABLE_NAME = "AWS_SECRET_ACCESS_KEY";
    public static final int MAX_CONNECTIONS = 10_000;
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final String AWS_REGION_ENV_VARIABLE = "AWS_REGION";
    public static final String DOUBLE_BACKSLASH = "\\\\\\\\";
    public static final String SINGLE_BACKSLASH = "\\\\";
    public static final String UNIX_SEPARATOR = "/";
    public static final int REMOVE_ROOT = 1;
    private static final Environment ENVIRONMENT = new Environment();
    private static final String EMPTY_STRING = "";
    private final S3Client client;
    private final String bucketName;

    @JacocoGenerated
    public S3Driver(String bucketName) {
        this(defaultS3Client().build(), bucketName);
    }

    public S3Driver(S3Client s3Client, String bucketName) {
        this.client = s3Client;
        this.bucketName = bucketName;
    }

    @JacocoGenerated
    public static S3Driver fromPermanentCredentialsInEnvironment(String bucketName) {
        verifyThatRequiredEnvVariablesAreInPlace();
        S3Client s3Client = defaultS3Client()
                                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                .build();
        return new S3Driver(s3Client, bucketName);
    }

    @JacocoGenerated
    public static SdkHttpClient httpClientForConcurrentQueries() {
        return ApacheHttpClient.builder()
                   .useIdleConnectionReaper(true)
                   .maxConnections(MAX_CONNECTIONS)
                   .connectionMaxIdleTime(Duration.ofMinutes(30))
                   .connectionTimeout(Duration.ofMinutes(30))
                   .build();
    }

    public void insertFile(UnixPath fullPath, String content) {
        client.putObject(newPutObjectRequest(fullPath), createRequestBody(content));
    }

    public void insertFile(UnixPath fullPath, InputStream content) throws IOException {
        client.putObject(newPutObjectRequest(fullPath), createRequestBody(content));
    }

    public void insertAndCompressFiles(UnixPath s3Folder, List<String> content) throws IOException {
        UnixPath path = filenameForZippedFile(s3Folder);
        PutObjectRequest putObjectRequest = newPutObjectRequest(path);
        try (InputStream compressedContent = contentToZippedStream(content)) {
            RequestBody requestBody = createRequestBody(compressedContent);
            client.putObject(putObjectRequest, requestBody);
        }
    }

    public void insertAndCompressFiles(List<String> content) throws IOException {
        insertAndCompressFiles(UnixPath.of(EMPTY_STRING), content);
    }

    public List<String> getFiles(UnixPath folder) {
        return listFiles(folder)
                   .stream()
                   .map(this::getFile)
                   .collect(Collectors.toList());
    }

    public List<String> listFiles(UnixPath folder) {
        List<String> resultBuffer = new ArrayList<>();
        ListObjectsResponse partialResult;
        String listingStartingPoint = null;
        do {
            partialResult = fetchNewResultsBatch(folder, listingStartingPoint);
            addResultsToBuffer(resultBuffer, partialResult);
            listingStartingPoint = extractNextListingStartingPoint(partialResult);
        } while (partialResult.isTruncated());

        return resultBuffer;
    }

    public Optional<String> getUncompressedFile(UnixPath file) {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        ResponseBytes<GetObjectResponse> response = fetchObject(getObjectRequest);
        return attempt(response::asUtf8String).toOptional();
    }

    public String getCompressedFile(UnixPath file) throws IOException {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        try (ResponseInputStream<GetObjectResponse> response = client.getObject(getObjectRequest)) {
            return decompressInputToString(response);
        }
    }

    public String getFile(String filename) {
        if (isCompressed(filename)) {
            return attempt(() -> getCompressedFile(UnixPath.of(filename))).orElseThrow();
        } else {
            return getUncompressedFile(UnixPath.of(filename)).orElseThrow();
        }
    }

    @JacocoGenerated
    private static S3ClientBuilder defaultS3Client() {
        Region region = ENVIRONMENT.readEnvOpt(AWS_REGION_ENV_VARIABLE)
                            .map(Region::of)
                            .orElse(Region.EU_WEST_1);
        return S3Client.builder()
                   .region(region)
                   .httpClient(httpClientForConcurrentQueries());
    }

    @JacocoGenerated
    private static void verifyThatRequiredEnvVariablesAreInPlace() {

        ENVIRONMENT.readEnv(AWS_ACCESS_KEY_ID_ENV_VARIABLE_NAME);
        ENVIRONMENT.readEnv(AWS_SECRET_ACCESS_KEY_ENV_VARIABLE_NAME);
    }

    private UnixPath filenameForZippedFile(UnixPath s3Folder) {
        String folderPath = processPath(s3Folder);
        return UnixPath.of(folderPath, UUID.randomUUID().toString() + GZIP_ENDING);
    }

    private String processPath(UnixPath s3Folder) {
        String unixPath = s3Folder.toString()
                              .replaceAll(DOUBLE_BACKSLASH, UNIX_SEPARATOR)
                              .replaceAll(SINGLE_BACKSLASH, UNIX_SEPARATOR);
        return unixPath.startsWith(UNIX_SEPARATOR)
                   ? unixPath.substring(REMOVE_ROOT)
                   : unixPath;
    }

    private RequestBody createRequestBody(InputStream compressedContent) throws IOException {
        var bytes = IoUtils.inputStreamToBytes(compressedContent);
        return RequestBody.fromBytes(bytes);
    }

    private RequestBody createRequestBody(String content) {
        return RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream contentToZippedStream(List<String> content) throws IOException {
        return new StringCompressor(content).gzippedData();
    }

    private ListObjectsResponse fetchNewResultsBatch(UnixPath folder, String listingStartingPoint) {
        ListObjectsRequest request = requestForListingFiles(folder, listingStartingPoint);
        return client.listObjects(request);
    }

    private ResponseBytes<GetObjectResponse> fetchObject(GetObjectRequest getObjectRequest) {
        return client.getObject(getObjectRequest, ResponseTransformer.toBytes());
    }

    private String decompressInputToString(ResponseInputStream<GetObjectResponse> response) throws IOException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(response)) {
            return readCompressedStream(gzipInputStream);
        }
    }

    private String readCompressedStream(GZIPInputStream gzipInputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream))) {
            return reader.lines().collect(Collectors.joining(LINE_SEPARATOR));
        }
    }

    private boolean isCompressed(String filename) {
        return filename.endsWith(GZIP_ENDING);
    }

    private String extractNextListingStartingPoint(ListObjectsResponse resultSet) {
        if (!resultSet.contents().isEmpty()) {
            return lastObjectKeyInReturnedResults(resultSet);
        }
        return null;
    }

    private GetObjectRequest createGetObjectRequest(UnixPath file) {
        return GetObjectRequest.builder()
                   .bucket(bucketName)
                   .key(file.toString())
                   .build();
    }

    private String lastObjectKeyInReturnedResults(ListObjectsResponse result) {
        return result.contents().get(result.contents().size() - 1).key();
    }

    private void addResultsToBuffer(List<String> resultBuffer, ListObjectsResponse result) {
        List<String> results = extractResultsFromResponse(result);
        resultBuffer.addAll(results);
    }

    private List<String> extractResultsFromResponse(ListObjectsResponse result) {
        return result.contents().stream().map(S3Object::key).collect(Collectors.toList());
    }

    private ListObjectsRequest requestForListingFiles(UnixPath folder, String startingPoint) {
        return ListObjectsRequest.builder()
                   .bucket(bucketName)
                   .prefix(folder.toString())
                   .marker(startingPoint)
                   .build();
    }

    private PutObjectRequest newPutObjectRequest(UnixPath fullPath) {
        return PutObjectRequest.builder()
                   .bucket(bucketName)
                   .key(fullPath.toString())
                   .build();
    }
}
