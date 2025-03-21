package nva.commons.apigateway;

import com.amazonaws.services.lambda.runtime.Context;
import java.time.Duration;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public abstract class ApiS3GatewayHandler<I> extends ApiS3PresignerGatewayHandler<I> {

    public static final String BUCKET_NAME = new Environment().readEnv("LARGE_API_RESPONSES_BUCKET");
    public static final Duration SIGN_DURATION = Duration.ofMinutes(60);
    private final S3Client s3client;

    public ApiS3GatewayHandler(Class<I> iclass, S3Client s3client, S3Presigner s3Presigner) {
        super(iclass, s3Presigner);
        this.s3client = s3client;
    }

    @JacocoGenerated
    public ApiS3GatewayHandler(Class<I> iclass,
                               S3Client s3client,
                               S3Presigner s3Presigner,
                               Environment environment) {
        super(iclass, s3Presigner, environment);
        this.s3client = s3client;
    }

    @JacocoGenerated
    public static S3Client defaultS3Client() {
        return S3Client.builder()
                   .region(Region.of(AWS_REGION))
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    @Override
    protected void generateAndWriteDataToS3(String filename, I input, RequestInfo requestInfo, Context context)
        throws BadRequestException {
        var data = processS3Input(input, requestInfo, context);
        writeDataToS3(filename, data);
    }

    @Override
    protected String getBucketName() {
        return BUCKET_NAME;
    }

    @Override
    protected Duration getSignDuration() {
        return SIGN_DURATION;
    }

    protected abstract String processS3Input(I input, RequestInfo requestInfo, Context context)
        throws BadRequestException;

    protected abstract String getContentType();

    private void writeDataToS3(String filename, String data) {
        var request = PutObjectRequest.builder()
                          .bucket(BUCKET_NAME)
                          .contentType(getContentType())
                          .key(filename)
                          .build();
        var requestBody = RequestBody.fromString(data);
        this.s3client.putObject(request, requestBody);
    }
}

