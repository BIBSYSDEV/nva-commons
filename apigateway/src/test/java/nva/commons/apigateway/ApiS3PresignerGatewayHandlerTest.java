package nva.commons.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class ApiS3PresignerGatewayHandlerTest {

    private S3Presigner s3Presigner;
    private ByteArrayOutputStream output;
    private InputStream input;
    private ApiS3PresignerGatewayHandler<Void> handler;

    @BeforeEach
    void init() {
        s3Presigner = mock(S3Presigner.class);
        input = IoUtils.stringToStream("{}");
        output = new ByteArrayOutputStream();
        handler = createHandler();
    }

    @Test
    void shouldSetLocationHeader() throws IOException {
        var context = new FakeContext();
        var expectedFilename = context.getAwsRequestId();

        var presignedGetObjectRequest = mockPresignResponse(expectedFilename);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);

        handler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getHeaders().get("Location"), containsString(expectedFilename));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatusCode.MOVED_TEMPORARILY)));
    }

    private static PresignedGetObjectRequest mockPresignResponse(String filename) throws MalformedURLException {
        var presignRequest = mock(PresignedGetObjectRequest.class);
        var presignedUrl = "https://example.com/" + filename;
        when(presignRequest.url()).thenReturn(new URL(presignedUrl));
        return presignRequest;
    }

    private ApiS3PresignerGatewayHandler<Void> createHandler() {
        return new ApiS3PresignerGatewayHandler<>(Void.class, s3Presigner) {

            @Override
            protected void validateAccessRights(Void input, RequestInfo requestInfo, Context context)
                throws UnauthorizedException {
                //no-op
            }

            @Override
            protected void generateAndWriteDataToS3(String filename, Void input, RequestInfo requestInfo,
                                                    Context context) throws BadRequestException {
            }

            @Override
            protected String getBucketName() {
                return "someTestBucket";
            }

            @Override
            protected Duration getSignDuration() {
                return Duration.ofMinutes(60);
            }
        };
    }
}