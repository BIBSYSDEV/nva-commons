package nva.commons.apigateway;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

public abstract class ApiS3PresignerGatewayHandler<I> extends ApiGatewayHandler<I, Void> {

    public static final String LOCATION = "Location";
    private final S3Presigner s3presigner;

    public ApiS3PresignerGatewayHandler(Class<I> iclass, S3Presigner s3Presigner) {
        super(iclass);
        this.s3presigner = s3Presigner;
    }

    @Override
    protected Void processInput(I input, RequestInfo requestInfo, Context context) throws BadRequestException {
        var filename = context.getAwsRequestId();
        var preSignedUrl = presignS3Object(filename).url();
        generateAndWriteDataToS3(preSignedUrl, input);
        setLocationHeader(preSignedUrl.toString());
        return null;
    }

    @Override
    @JacocoGenerated
    protected Integer getSuccessStatusCode(I input, Void output) {
        return HttpURLConnection.HTTP_MOVED_TEMP;
    }

    protected abstract void generateAndWriteDataToS3(URL preSignedUrl, I input);

    protected abstract String getBucketName();

    protected abstract Duration getSignDuration();

    private GetObjectPresignRequest createPresignRequest(String filename) {
        return GetObjectPresignRequest.builder()
                   .signatureDuration(getSignDuration())
                   .getObjectRequest(buildRequest(filename))
                   .build();
    }

    private GetObjectRequest buildRequest(String filename) {
        return GetObjectRequest.builder()
                   .bucket(getBucketName())
                   .key(filename)
                   .build();
    }

    private void setLocationHeader(String uri) {
        addAdditionalHeaders(() -> Map.of(LOCATION, uri));
    }

    private PresignedGetObjectRequest presignS3Object(String filename) {
        var presignRequest = createPresignRequest(filename);
        return s3presigner.presignGetObject(presignRequest);
    }
}
