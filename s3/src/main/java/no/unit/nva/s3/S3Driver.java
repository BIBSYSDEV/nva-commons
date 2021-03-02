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

    public void insertFile(Path filename, String content) {
        PutObjectRequest putObjectRequest = newPutObjectRequest(filename);

        client.putObject(putObjectRequest, RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));
    }

    public List<String> getFiles(Path folder) {
        List<String> filenames = listFiles(folder);
        return filenames.stream()
                   .map(this::readFileContent)
                   .collect(Collectors.toList());
    }

    public List<String> listFiles(Path folder) {
        List<String> resultBuffer = new ArrayList<>();
        ListObjectsResponse result = null;
        String nextMarker = null;
        boolean isFirstLoop = true;
        while (isFirstLoop || result.isTruncated()) {
            var request = listFilesRequest(folder, nextMarker);
            result = client.listObjects(request);
            addResultsToBuffer(resultBuffer, result);
            nextMarker = extractNextMarkerFromResultSet(result);
            isFirstLoop = false;
        }
        return resultBuffer;
    }

    public Optional<String> getFile(Path file) {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        ResponseBytes<GetObjectResponse> response = client.getObject(getObjectRequest, ResponseTransformer.toBytes());
        return attempt(response::asUtf8String).toOptional();
    }

    public String getCompressedFile(Path file) throws IOException {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        try (ResponseInputStream<GetObjectResponse> response = client.getObject(getObjectRequest)) {
            return decompressInputToString(response);
        }
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

    private String extractNextMarkerFromResultSet(ListObjectsResponse resultSet) {
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

    private ListObjectsRequest listFilesRequest(Path folder, String marker) {
        return ListObjectsRequest.builder()
                   .bucket(bucketName)
                   .prefix(pathToString(folder))
                   .marker(marker)
                   .build();
    }

    private PutObjectRequest newPutObjectRequest(Path filename) {
        return PutObjectRequest.builder()
                   .bucket(bucketName)
                   .key(pathToString(filename))
                   .build();
    }
}
