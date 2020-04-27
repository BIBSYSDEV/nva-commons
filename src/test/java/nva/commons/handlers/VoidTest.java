package nva.commons.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.utils.Environment;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class VoidTest {

    public static final String APIGATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    public static final String MISSING_BODY_REQUEST = "missingBody.json";
    public static final String SOME_ENV_VALUE = "some_env_value";
    public static final String EMPT_BODY_REQUEST = "bodyIsAnEmptyObject.json";

    private Environment environment;
    private Context context;

    /**
     * Setup.
     */
    @BeforeEach
    @Test
    public void setup() {
        environment = mock(Environment.class);
        context = mock(Context.class);
        when(environment.readEnv(anyString())).thenReturn(SOME_ENV_VALUE);
    }

    @DisplayName("handleRequest returns success when input class is void and body field is missing from "
        + "ApiGateway event")
    @Test
    public void handleRequestReturnsSuccessWhenInputClassIsVoidAndBodyFieldIsMissingFromApiGatewayEvent()
        throws IOException {
        ByteArrayOutputStream outputStream = responseFromVoidHandler(MISSING_BODY_REQUEST);

        TypeReference<GatewayResponse<String>> tr = new TypeReference<>() {};
        GatewayResponse<String> output = JsonUtils.objectMapper.readValue(outputStream.toString(), tr);
        assertThat(output.getStatusCode(), is(equalTo(HttpStatus.SC_OK)));
    }

    @DisplayName("handleRequest return success when input class is Void and body field is an empty object in "
        + "ApiGateway event")
    @Test
    public void handleRequestReturnsSuccessWhenInputClassIsVoidAndBodyFieldIsAnEmptyObjectInApiGatewayEvent()
        throws IOException {
        ByteArrayOutputStream outputStream = responseFromVoidHandler(EMPT_BODY_REQUEST);

        TypeReference<GatewayResponse<String>> tr = new TypeReference<>() {};
        GatewayResponse<String> output = JsonUtils.objectMapper.readValue(outputStream.toString(), tr);
        assertThat(output.getStatusCode(), is(equalTo(HttpStatus.SC_OK)));
    }

    private ByteArrayOutputStream responseFromVoidHandler(String missingBodyRequest) throws IOException {
        InputStream input = IoUtils.inputStreamFromResources(Path.of(APIGATEWAY_MESSAGES_FOLDER, missingBodyRequest));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        VoidHandler handler = new VoidHandler(environment);
        handler.handleRequest(input, outputStream, context);
        return outputStream;
    }

    private class VoidHandler extends ApiGatewayHandler<Void, String> {

        public static final String SAMPLE_STRING = "sampleString";

        public VoidHandler(Environment environment) {
            super(Void.class, environment);
            logger = LoggerFactory.getLogger(VoidHandler.class);
        }

        @Override
        protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
            return SAMPLE_STRING;
        }

        @Override
        protected Integer getSuccessStatusCode(Void input, String output) {
            return HttpStatus.SC_OK;
        }
    }
}