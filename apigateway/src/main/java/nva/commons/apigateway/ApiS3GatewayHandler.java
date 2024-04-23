package nva.commons.apigateway;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Map;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public abstract class ApiS3GatewayHandler<I> extends ApiGatewayHandler<I, Void> {

    public static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    public static final String BUCKET_NAME = new Environment().readEnv("LARGE_API_RESPONSES_BUCKET");
    public static final String LOCATION = "Location";
    public static final Duration SIGN_DURATION = Duration.ofMinutes(60);
    private final S3Client s3client;
    private final S3Presigner s3presigner;

    public ApiS3GatewayHandler(Class<I> iclass, S3Client s3client, S3Presigner s3Presigner) {
        super(iclass);
        this.s3client = s3client;
        this.s3presigner = s3Presigner;
    }

    @JacocoGenerated
    public ApiS3GatewayHandler(Class<I> iclass,
                               S3Client s3client,
                               S3Presigner s3Presigner,
                               Environment environment,
                               ObjectMapper objectMapper) {
        super(iclass, environment, objectMapper);
        this.s3client = s3client;
        this.s3presigner = s3Presigner;
    }

    @Override
    @JacocoGenerated
    protected Integer getSuccessStatusCode(I input, Void output) {
        return HttpURLConnection.HTTP_MOVED_TEMP;
    }

    @Override
    protected Void processInput(I input, RequestInfo requestInfo, Context context) throws BadRequestException {
        var data = processS3Input(input, requestInfo, context);
        var filename = context.getAwsRequestId();

        writeDataToS3(filename, data);

        var presign = presignS3Object(filename);

        setLocationHeader(presign.url().toString());
        return null;
    }

    private PresignedGetObjectRequest presignS3Object(String filename) {
        var presignRequest = createPresignRequest(filename);
        var presign = s3presigner.presignGetObject(presignRequest);
        return presign;
    }

    private void writeDataToS3(String filename, String data) {
        var request = PutObjectRequest.builder()
                          .bucket(BUCKET_NAME)
                          .contentType(getContentType())
                          .key(filename)
                          .build();
        var requestBody = RequestBody.fromString(data);
        this.s3client.putObject(request, requestBody);
    }

    private void setLocationHeader(String uri) {
        addAdditionalHeaders(() -> Map.of(LOCATION, uri));
    }

    private static GetObjectPresignRequest createPresignRequest(String filename) {
        return GetObjectPresignRequest.builder()
                   .signatureDuration(SIGN_DURATION)
                   .getObjectRequest(
                       GetObjectRequest.builder()
                           .bucket(BUCKET_NAME)
                           .key(filename)
                           .build()
                   )
                   .build();
    }

    abstract String processS3Input(I input, RequestInfo requestInfo, Context context) throws BadRequestException;

    abstract String getContentType();

    @JacocoGenerated
    public static S3Client defaultS3Client() {
        return S3Client.builder()
                   .region(Region.of(AWS_REGION))
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    @JacocoGenerated
    public static S3Presigner defaultS3Presigner() {
        return S3Presigner.builder()
                   .region(Region.of(AWS_REGION))
                   .credentialsProvider(DefaultCredentialsProvider.create())
                   .build();
    }
}

