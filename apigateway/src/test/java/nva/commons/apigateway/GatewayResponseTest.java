package nva.commons.apigateway;

import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import nva.commons.apigateway.exceptions.GatewayResponseSerializingException;
import nva.commons.apigateway.testutils.RequestBody;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GatewayResponseTest {

    public static final String SOME_VALUE = "Some value";
    public static final String SOME_OTHER_VALUE = "Some other value";
    public static final String SOME_KEY = "key1";
    public static final String SOME_OTHER_KEY = "key2";
    public static final String API_GATEWAY_RESOURCES = "apiGatewayResponses";
    public static final String SAMPLE_RESPONSE_JSON = "sampleResponse.json";

    @Test
    @DisplayName("hashCode is the same for equivalent GatewayResponses")
    public void hashCodeIsTheSameForEquivalentGatewayResponses() throws GatewayResponseSerializingException {
        GatewayResponse<RequestBody> leftResponse = sampleGatewayResponse();
        GatewayResponse<RequestBody> rightResponse = sampleGatewayResponse();

        assertThat(leftResponse.hashCode(), is(equalTo(rightResponse.hashCode())));
    }

    @Test
    @DisplayName("equals returns true for equivalent Gateway responses")
    public void equalsReturnsTrueForEquivalentGatewayResponses() throws GatewayResponseSerializingException {
        GatewayResponse<RequestBody> leftResponse = sampleGatewayResponse();
        GatewayResponse<RequestBody> rightResponse = sampleGatewayResponse();

        assertThat(leftResponse, is(equalTo(rightResponse)));
    }

    @Test
    @DisplayName("fromOutputStream returns a GatewayResponse object for a valid json input")
    public void fromOutputStreamReturnsGatewayResponseWhenInputIsOutputStreamContainingValidJson() throws IOException {
        String sampleResponse = IoUtils.stringFromResources(Path.of(API_GATEWAY_RESOURCES, SAMPLE_RESPONSE_JSON));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
            sampleResponse.getBytes(StandardCharsets.UTF_8).length);
        outputStream.write(sampleResponse.getBytes(StandardCharsets.UTF_8));
        var response = GatewayResponse.fromOutputStream(outputStream, Map.class);
        var responseOf = GatewayResponse.<Map<String,String>>of(outputStream);
        Map<String, String> body = response.getBodyObject(Map.class);
        assertFalse(body.isEmpty());
        assertThat(responseOf.getBodyAsInstance(), is(equalTo(body)));
    }

    @Test
    @DisplayName("fromOutputStream returns a GatewayResponse object for a valid json input")
    public void fromOutputStreamReturnsGatewayResponseWhenInputIsValidJsonString() throws IOException {
        String sampleResponse = IoUtils.stringFromResources(Path.of(API_GATEWAY_RESOURCES, SAMPLE_RESPONSE_JSON));
        GatewayResponse<Map> response = GatewayResponse.fromString(sampleResponse,Map.class);
        Map<String, String> body = response.getBodyObject(Map.class);
        assertFalse(body.isEmpty());
    }


    @Test
    @DisplayName("returns a GatewayResponse with body instance for a valid json string")
    void returnsGatewayResponseWhenInputIsValidInputStream() throws JsonProcessingException {
        var jsonString =
            IoUtils.stringFromResources(Path.of(API_GATEWAY_RESOURCES, SAMPLE_RESPONSE_JSON));

        var gatewayResponse = GatewayResponse.<Map<String,String>>of(jsonString);
        var bodyAsInstance = gatewayResponse.getBodyAsInstance();

        assertFalse(bodyAsInstance.isEmpty());
    }
    @Test
    @DisplayName("returns a GatewayResponse with body instance for a valid json inputStream")
    void returnsGatewayResponseWhenInputIsValidOutputStream() throws IOException {
        var inputStream =
            IoUtils.inputStreamFromResources(Path.of(API_GATEWAY_RESOURCES, SAMPLE_RESPONSE_JSON).toString());

        var gatewayResponse = GatewayResponse.<Map<String,String>>of(inputStream);
        var bodyAsInstance = gatewayResponse.getBodyAsInstance();

        assertFalse(bodyAsInstance.isEmpty());
    }
    @Test
    @DisplayName("returns a GatewayResponse with body instance for a valid json byteArrayOutputStream")
    void returnsGatewayResponseWhenInputIsValidJsonString() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream;
        try (var inputStream = IoUtils.inputStreamFromResources(
            Path.of(API_GATEWAY_RESOURCES, SAMPLE_RESPONSE_JSON).toString())) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            inputStream.transferTo(byteArrayOutputStream);
        }

        var gatewayResponse = GatewayResponse.<Map<String,String>>of(byteArrayOutputStream);
        var bodyAsInstance = gatewayResponse.getBodyAsInstance();

        assertFalse(bodyAsInstance.isEmpty());
    }

    private GatewayResponse<RequestBody> sampleGatewayResponse()
        throws GatewayResponseSerializingException {
        return new GatewayResponse<>(sampleRequestBody(),
                                     sampleHeaders(),
                                     HttpURLConnection.HTTP_OK,
                                     defaultRestObjectMapper);
    }

    private Map<String, String> sampleHeaders() {
        Map<String, String> leftHeaders = new HashMap<>();
        leftHeaders.put(SOME_KEY, SOME_VALUE);
        leftHeaders.put(SOME_OTHER_KEY, SOME_OTHER_VALUE);
        return leftHeaders;
    }

    private RequestBody sampleRequestBody() {
        RequestBody leftBody = new RequestBody();
        leftBody.setField1(SOME_VALUE);
        leftBody.setField1(SOME_OTHER_VALUE);
        return leftBody;
    }
}