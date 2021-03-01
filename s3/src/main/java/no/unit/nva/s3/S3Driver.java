package no.unit.nva.s3;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.ResponseBytes;
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

    public static final String OS_PATH_DELIMITER = File.separator;
    public static final String UNIX_PATH_DELIMITER = "/";
    private final S3Client client;
    private final String bucketName;

    public S3Driver(S3Client s3Client, String bucketName) {
        this.client = s3Client;
        this.bucketName = bucketName;
    }

    public static String pathToString(Path filename) {
        if (!OS_PATH_DELIMITER.equals(UNIX_PATH_DELIMITER)) {
            return filename.toString().replaceAll(OS_PATH_DELIMITER, UNIX_PATH_DELIMITER);
        }
        return filename.toString();
    }

    public void insertFile(Path filename, String content) {
        PutObjectRequest putObjectRequest = newPutObjectRequest(filename);

        client.putObject(putObjectRequest, RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));
    }

    public List<String> getFiles(Path folder) {
        List<String> filenames = listFiles(folder);
        return filenames.stream()
                   .map(filename -> getFile(Path.of(filename)))
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

    public String getFile(Path file) {
        GetObjectRequest getObjectRequest = createGetObjectRequest(file);
        ResponseBytes<GetObjectResponse> response = client.getObject(getObjectRequest, ResponseTransformer.toBytes());
        return response.asUtf8String();
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
        var contents = result.contents();
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
