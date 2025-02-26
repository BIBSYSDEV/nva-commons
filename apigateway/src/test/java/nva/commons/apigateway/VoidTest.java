package nva.commons.apigateway;

import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.file.Path;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class VoidTest {

    public static final String APIGATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    public static final String MISSING_BODY_REQUEST = "missingBody.json";
    public static final String SOME_ENV_VALUE = "some_env_value";
    public static final String EMPT_BODY_REQUEST = "bodyIsAnEmptyObject.json";

    private Environment environment;
    private Context context;
    private HttpClient httpClient;

    /**
     * Setup.
     */
    @BeforeEach
    @Test
    public void setup() {
        environment = mock(Environment.class);
        context = new FakeContext();
        when(environment.readEnv(anyString())).thenReturn(SOME_ENV_VALUE);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        httpClient = mock(HttpClient.class);
    }

    @DisplayName("handleRequest returns success when input class is void and body field is missing from "
        + "ApiGateway event")
    @Test
    public void handleRequestReturnsSuccessWhenInputClassIsVoidAndBodyFieldIsMissingFromApiGatewayEvent()
        throws IOException {
        ByteArrayOutputStream outputStream = responseFromVoidHandler(MISSING_BODY_REQUEST);
        GatewayResponse<String> output = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(output.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    @DisplayName("handleRequest return success when input class is Void and body field is an empty object in "
        + "ApiGateway event")
    @Test
    public void handleRequestReturnsSuccessWhenInputClassIsVoidAndBodyFieldIsAnEmptyObjectInApiGatewayEvent()
        throws IOException {
        ByteArrayOutputStream outputStream = responseFromVoidHandler(EMPT_BODY_REQUEST);
        GatewayResponse<String> output = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(output.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    }

    private ByteArrayOutputStream responseFromVoidHandler(String missingBodyRequest) throws IOException {
        InputStream input = IoUtils.inputStreamFromResources(Path.of(APIGATEWAY_MESSAGES_FOLDER, missingBodyRequest));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        VoidHandler handler = new VoidHandler(environment, httpClient);
        handler.handleRequest(input, outputStream, context);
        return outputStream;
    }

    private class VoidHandler extends ApiGatewayHandler<Void, String> {

        public static final String SAMPLE_STRING = "sampleString";

        public VoidHandler(Environment environment, HttpClient httpClient) {
            super(Void.class, environment, httpClient);
        }

        @Override
        protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
            throws ApiGatewayException {
            //no-op
        }

        @Override
        protected String processInput(Void input, RequestInfo requestInfo, Context context) {
            return SAMPLE_STRING;
        }

        @Override
        protected Integer getSuccessStatusCode(Void input, String output) {
            return HttpURLConnection.HTTP_OK;
        }
    }
}