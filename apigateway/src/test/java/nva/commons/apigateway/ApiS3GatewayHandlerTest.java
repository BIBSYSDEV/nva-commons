package nva.commons.apigateway;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class ApiS3GatewayHandlerTest {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private ByteArrayOutputStream output;
    private InputStream input;

    @BeforeEach
    void init() {
        s3Client = new FakeS3Client();
        s3Presigner = mock(S3Presigner.class);
        this.input = InputStream.nullInputStream();
        this.output = new ByteArrayOutputStream();
    }

    @Test
    void shouldCallS3PutObjectWithCorrectData() throws IOException {
        var expectedData = randomString();
        var handler = createHandler(expectedData);
        var context = new FakeContext();
        var expectedFilename = context.getAwsRequestId();

        PresignedGetObjectRequest t = mockPresignResponse(expectedFilename);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(t);

        handler.handleRequest(IoUtils.stringToStream("{}"), this.output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        var getRequest = GetObjectRequest.builder()
                             .bucket("large-bucket")
                             .key(expectedFilename)
                             .build();

        var s3object = s3Client.getObject(getRequest, ResponseTransformer.toBytes());
        assertThat(s3object.asUtf8String(), is(equalTo(expectedData)));
        assertThat(response.getHeaders().get("Location"), containsString(expectedFilename));
        assertThat(response.getStatusCode(), is(equalTo(HttpStatusCode.MOVED_TEMPORARILY)));
    }

    private static PresignedGetObjectRequest mockPresignResponse(String filename) throws MalformedURLException {
        var presignRequest = mock(PresignedGetObjectRequest.class);
        var presignedUrl = "https://example.com/" + filename;
        when(presignRequest.url()).thenReturn(new URL(presignedUrl));
        return presignRequest;
    }

    private ApiS3GatewayHandler createHandler(String data) {
        return new ApiS3GatewayHandler<>(Void.class, s3Client, s3Presigner) {
            @Override
            public String processS3Input(Void input, RequestInfo requestInfo, Context context) throws BadRequestException {
                return data;
            }

            @Override
            public String getContentType() {
                return randomString();
            }
        };
    }

}