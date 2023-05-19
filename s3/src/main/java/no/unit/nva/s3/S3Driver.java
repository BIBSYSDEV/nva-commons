package no.unit.nva.s3;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
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
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

//TODO: Address God Class issue
@SuppressWarnings("PMD.GodClass")
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
    public static final int MAX_RESPONSE_SIZE_FOR_S3_LISTING = 1000;
    public static final String S3_SCHEME = "s3";
    public static final int IDLE_TIME = 30;
    public static final int TIMEOUT_TIME = 30;
    private static final Environment ENVIRONMENT = new Environment();
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
    public static S3ClientBuilder defaultS3Client() {
        Region region = ENVIRONMENT.readEnvOpt(AWS_REGION_ENV_VARIABLE)
                            .map(Region::of)
                            .orElse(Region.EU_WEST_1);
        return S3Client.builder()
                   .region(region)
                   .httpClient(httpClientForConcurrentQueries());
    }

    /**
     * Inserts the content of the string in the specified location.If the filename is gz, it compresses the contents.
     *
     * @param fullPath the Location path of the item (without the bucketname)
     * @param content  The data we want to store
     * @return URI for the S3 object
     * @throws IOException when compression fails.
     */
    public URI insertFile(UnixPath fullPath, String content) throws IOException {
        if (fullPath.getLastPathElement().endsWith(GZIP_ENDING)) {
            insertCompressedFile(fullPath, content);
        } else {
            insertUncompressedFile(fullPath, content);
        }
        return s3BucketUri().addChild(fullPath).getUri();
    }

    public URI insertFile(UnixPath fullPath, InputStream content) throws IOException {
        client.putObject(newPutObjectRequest(fullPath), createRequestBody(content));
        return s3BucketUri().addChild(fullPath).getUri();
    }

    /**
     * Method for creating event bodies in S3 bucket.
     *
     * @param folder  The folder where the event will be stored
     * @param content the event body
     * @return S3 uri to the event file.
     * @throws IOException when IO fails
     */
    public URI insertEvent(UnixPath folder, String content) throws IOException {
        UnixPath filePath = folder.addChild(UUID.randomUUID() + GZIP_ENDING);
        insertCompressedFile(filePath, content);
        return s3BucketUri().addChild(filePath).getUri();
    }

    /**
     * Method for reading event bodies from S3 bucket.
     *
     * @param uri the S3 URI to the file. The host must be equal to the bucket name of the S3 driver
     * @return the file contents uncompressed.
     */
    public String readEvent(URI uri) {
        UnixPath filePath = UriWrapper.fromUri(uri).toS3bucketPath();
        return getFile(filePath);
    }

    @JacocoGenerated
    @Deprecated
    public URI insertAndCompressFiles(UnixPath s3Folder, List<String> content) throws IOException {
        return insertAndCompressObjects(s3Folder, content);
    }

    @JacocoGenerated
    @Deprecated
    public URI insertAndCompressFiles(List<String> content) throws IOException {
        return insertAndCompressObjects(content);
    }

    public URI insertAndCompressObjects(UnixPath s3Folder, List<String> content) throws IOException {
        UnixPath path = filenameForZippedFile(s3Folder);
        PutObjectRequest putObjectRequest = newPutObjectRequest(path);
        try (InputStream compressedContent = compressContent(content)) {
            RequestBody requestBody = createRequestBody(compressedContent);
            client.putObject(putObjectRequest, requestBody);
        }
        return s3BucketUri().addChild(path).getUri();
    }

    public URI insertAndCompressObjects(List<String> content) throws IOException {
        return insertAndCompressObjects(UnixPath.EMPTY_PATH, content);
    }

    public List<String> getFiles(UnixPath folder) {
        return listAllFiles(folder)
                   .stream()
                   .map(this::getFile)
                   .collect(Collectors.toList());
    }

    public List<UnixPath> listAllFiles(URI s3Uri) {
        return listAllFiles(UriWrapper.fromUri(s3Uri).toS3bucketPath());
    }

    public List<UnixPath> listAllFiles(UnixPath folder) {

        ListingResult result = ListingResult.emptyResult();
        do {
            var currentStartingPoint = result.getListingStartingPoint();
            var newBatch = listFiles(calculateListingFolder(folder),
                                     currentStartingPoint,
                                     MAX_RESPONSE_SIZE_FOR_S3_LISTING);

            result = result.add(newBatch);
        } while (result.isTruncated());

        return result.getFiles();
    }

    /**
     * Returns a partial result of the files contained in the specified folder. The listing starts from the
     * {@code listingStartingPoint}  if is not null or from the beginning if it is null. After a call the next starting
     * point can be acquired by the {@link ListingResult}
     *
     * @param folder               The folder that we wish to list its files.
     * @param listingStartingPoint The starting point for the listing, can be {@code null} to indicate that the
     *                             beginning of the listing.
     * @param responseSize         The number of filenames returned in each batch. Max size determined by S3 is 1000.
     * @return a result containing the returned filenames, the next {@code listingStartingPoint} and whether there are
     *     more files to list.
     */
    public ListingResult listFiles(UnixPath folder, String listingStartingPoint, int responseSize) {
        var listingResult = fetchNewResultsBatch(folder, listingStartingPoint, responseSize);
        List<UnixPath> files = extractResultsFromResponse(listingResult);
        return new ListingResult(files, listingResult.nextContinuationToken(), listingResult.isTruncated());
    }

    public String getUncompressedFile(UnixPath file) {
        return getUncompressedFile(file, StandardCharsets.UTF_8);
    }

    public String getUncompressedFile(UnixPath file, Charset charset) {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        ResponseBytes<GetObjectResponse> response = fetchObject(getObjectRequest);
        return response.asString(charset);
    }

    public GZIPInputStream getCompressedFile(UnixPath file) throws IOException {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        ResponseInputStream<GetObjectResponse> response = client.getObject(getObjectRequest);
        return new GZIPInputStream(response);
    }

    public String getFile(UnixPath filename, Charset charset) {
        if (isCompressed(filename.getLastPathElement())) {
            return attempt(() -> getCompressedFile(filename))
                       .map(stream -> readCompressedStream(stream, charset))
                       .orElseThrow();
        } else {
            return getUncompressedFile(filename, charset);
        }
    }

    public String getFile(UnixPath filename) {
        return getFile(filename, StandardCharsets.UTF_8);
    }

    @JacocoGenerated
    private static SdkHttpClient httpClientForConcurrentQueries() {
        return ApacheHttpClient.builder()
                   .useIdleConnectionReaper(true)
                   .maxConnections(MAX_CONNECTIONS)
                   .connectionMaxIdleTime(Duration.ofMinutes(IDLE_TIME))
                   .connectionTimeout(Duration.ofMinutes(TIMEOUT_TIME))
                   .build();
    }

    @JacocoGenerated
    private static void verifyThatRequiredEnvVariablesAreInPlace() {
        ENVIRONMENT.readEnv(AWS_ACCESS_KEY_ID_ENV_VARIABLE_NAME);
        ENVIRONMENT.readEnv(AWS_SECRET_ACCESS_KEY_ENV_VARIABLE_NAME);
    }

    private UnixPath calculateListingFolder(UnixPath folder) {
        return isNull(folder) || folder.isEmptyPath() || folder.isRoot()
                   ? UnixPath.EMPTY_PATH
                   : folder;
    }

    private UriWrapper s3BucketUri() {
        return new UriWrapper(S3_SCHEME, bucketName);
    }

    private void insertUncompressedFile(UnixPath fullPath, String content) throws IOException {
        try (InputStream inputStream = IoUtils.stringToStream(content)) {
            client.putObject(newPutObjectRequest(fullPath), createRequestBody(inputStream));
        }
    }

    private void insertCompressedFile(UnixPath fullPath, String content) throws IOException {
        try (InputStream inputStream = compressContent(List.of(content))) {
            insertFile(fullPath, inputStream);
        }
    }

    private UnixPath filenameForZippedFile(UnixPath s3Folder) {
        String folderPath = processPath(s3Folder);
        return UnixPath.of(folderPath, UUID.randomUUID() + GZIP_ENDING);
    }

    private String processPath(UnixPath s3Folder) {
        String unixPath = s3Folder.toString()
                              .replaceAll(DOUBLE_BACKSLASH, UNIX_SEPARATOR)
                              .replaceAll(SINGLE_BACKSLASH, UNIX_SEPARATOR);
        return unixPath.startsWith(UNIX_SEPARATOR)
                   ? unixPath.substring(REMOVE_ROOT)
                   : unixPath;
    }

    private RequestBody createRequestBody(InputStream input) throws IOException {
        var bytes = IoUtils.inputStreamToBytes(input);
        return RequestBody.fromBytes(bytes);
    }

    private InputStream compressContent(List<String> content) throws IOException {
        return new StringCompressor(content).gzippedData();
    }

    private ListObjectsV2Response fetchNewResultsBatch(UnixPath folder, String listingStartingPoint, int responseSize) {
        var request = requestForListingFiles(folder, listingStartingPoint, responseSize);
        return client.listObjectsV2(request);
    }

    private ResponseBytes<GetObjectResponse> fetchObject(GetObjectRequest getObjectRequest) {
        return client.getObject(getObjectRequest, ResponseTransformer.toBytes());
    }

    private String readCompressedStream(GZIPInputStream gzipInputStream, Charset charset) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream, charset))) {
            return reader.lines().collect(Collectors.joining(LINE_SEPARATOR));
        }
    }

    private boolean isCompressed(String filename) {
        return filename.endsWith(GZIP_ENDING);
    }

    private GetObjectRequest createGetObjectRequest(UnixPath file) {
        return GetObjectRequest.builder()
                   .bucket(bucketName)
                   .key(file.toString())
                   .build();
    }

    private List<UnixPath> extractResultsFromResponse(ListObjectsV2Response result) {
        return result.contents().stream()
                   .map(S3Object::key)
                   .map(UnixPath::of)
                   .collect(Collectors.toList());
    }

    private ListObjectsV2Request requestForListingFiles(UnixPath folder, String startingPoint, int responseSize) {
        return ListObjectsV2Request.builder()
                   .bucket(bucketName)
                   .prefix(folder.toString())
                   .continuationToken(startingPoint)
                   .maxKeys(responseSize)
                   .build();
    }

    private PutObjectRequest newPutObjectRequest(UnixPath fullPath) {
        return PutObjectRequest.builder()
                   .bucket(bucketName)
                   .key(fullPath.toString())
                   .build();
    }
}
