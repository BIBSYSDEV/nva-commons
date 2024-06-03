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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public abstract class ApiS3PresignerGatewayHandler<I> extends ApiGatewayHandler<I, Void> {

    public static final String AWS_REGION = new Environment().readEnv("AWS_REGION");
    public static final String LOCATION = "Location";
    private final S3Presigner s3presigner;

    public ApiS3PresignerGatewayHandler(Class<I> iclass, S3Presigner s3Presigner) {
        super(iclass);
        this.s3presigner = s3Presigner;
    }

    @JacocoGenerated
    public ApiS3PresignerGatewayHandler(Class<I> iclass,
                                        S3Presigner s3Presigner,
                                        Environment environment,
                                        ObjectMapper objectMapper) {
        super(iclass, environment, objectMapper);
        this.s3presigner = s3Presigner;
    }

    @JacocoGenerated
    public static S3Presigner defaultS3Presigner() {
        return S3Presigner.builder()
                   .region(Region.of(AWS_REGION))
                   .credentialsProvider(DefaultCredentialsProvider.create())
                   .build();
    }

    @Override
    protected Void processInput(I input, RequestInfo requestInfo, Context context) throws BadRequestException {
        var filename = context.getAwsRequestId();
        generateAndWriteDataToS3(filename, input, requestInfo, context);
        var preSignedUrl = presignS3Object(filename);
        setLocationHeader(preSignedUrl.url().toString());
        return null;
    }

    @Override
    @JacocoGenerated
    protected Integer getSuccessStatusCode(I input, Void output) {
        return HttpURLConnection.HTTP_MOVED_TEMP;
    }

    protected abstract void generateAndWriteDataToS3(String filename, I input, RequestInfo requestInfo, Context context)
        throws BadRequestException;

    protected abstract String getBucketName();

    protected abstract Duration getSignDuration();

    protected void setLocationHeader(String uri) {
        addAdditionalHeaders(() -> Map.of(LOCATION, uri));
    }

    protected PresignedGetObjectRequest presignS3Object(String filename) {
        var presignRequest = createPresignRequest(filename);
        return s3presigner.presignGetObject(presignRequest);
    }

    protected GetObjectRequest buildRequest(String filename) {
        return GetObjectRequest.builder()
                   .bucket(getBucketName())
                   .key(filename)
                   .build();
    }

    protected GetObjectPresignRequest createPresignRequest(String filename) {
        return GetObjectPresignRequest.builder()
                   .signatureDuration(getSignDuration())
                   .getObjectRequest(buildRequest(filename))
                   .build();
    }
}
