package no.unit.nva.s3;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.pathToString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Driver {

    public static final String GZIP_ENDING = ".gz";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private final S3Client client;
    private final String bucketName;

    public S3Driver(S3Client s3Client, String bucketName) {
        this.client = s3Client;
        this.bucketName = bucketName;
    }

    public void insertFile(Path fullPath, String content) {
        PutObjectRequest putObjectRequest = newPutObjectRequest(fullPath);
        client.putObject(putObjectRequest, createRequestBody(content));
    }

    public List<String> getFiles(Path folder) {
        List<String> filenames = listFiles(folder);
        return filenames.stream()
                   .map(this::readFileContent)
                   .collect(Collectors.toList());
    }

    public List<String> listFiles(Path folder) {
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

    private ListObjectsResponse fetchNewResultsBatch(Path folder, String listingStartingPoint) {
        ListObjectsRequest request = requestForListingFiles(folder, listingStartingPoint);
        return client.listObjects(request);
    }

    public Optional<String> getFile(Path file) {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        ResponseBytes<GetObjectResponse> response = fetchObject(getObjectRequest);
        return attempt(response::asUtf8String).toOptional();
    }

    public String getCompressedFile(Path file) throws IOException {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        try (ResponseInputStream<GetObjectResponse> response = client.getObject(getObjectRequest)) {
            return decompressInputToString(response);
        }
    }

    private RequestBody createRequestBody(String content) {
        return RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8));
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

    private String readFileContent(String filename) {
        if (isCompressed(filename)) {
            return attempt(() -> getCompressedFile(Path.of(filename))).orElseThrow();
        } else {
            return getFile(Path.of(filename)).orElseThrow();
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

    private GetObjectRequest createGetObjectRequest(Path file) {
        return GetObjectRequest.builder()
                   .bucket(bucketName)
                   .key(pathToString(file))
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

    private ListObjectsRequest requestForListingFiles(Path folder, String startingPoint) {
        return ListObjectsRequest.builder()
                   .bucket(bucketName)
                   .prefix(pathToString(folder))
                   .marker(startingPoint)
                   .build();
    }

    private PutObjectRequest newPutObjectRequest(Path fullPath) {
        return PutObjectRequest.builder()
                   .bucket(bucketName)
                   .key(pathToString(fullPath))
                   .build();
    }
}
