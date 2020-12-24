package nva.commons.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import nva.commons.apigateway.exceptions.GatewayResponseSerializingException;

import nva.commons.apigateway.testutils.RequestBody;
import nva.commons.commons.IoUtils;
import org.apache.http.HttpStatus;
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
        GatewayResponse<Map> response = GatewayResponse.fromOutputStream(outputStream);
        Map<String, String> body = response.getBodyObject(Map.class);
        assertFalse(body.isEmpty());
    }

    @Test
    @DisplayName("fromOutputStream returns a GatewayResponse object for a valid json input")
    public void fromOutputStreamReturnsGatewayResponseWhenInputIsValidJsonString() throws IOException {
        String sampleResponse = IoUtils.stringFromResources(Path.of(API_GATEWAY_RESOURCES, SAMPLE_RESPONSE_JSON));
        GatewayResponse<Map> response = GatewayResponse.fromString(sampleResponse);
        Map<String, String> body = response.getBodyObject(Map.class);
        assertFalse(body.isEmpty());
    }

    private GatewayResponse<RequestBody> sampleGatewayResponse()
        throws GatewayResponseSerializingException {
        return new GatewayResponse<>(sampleRequestBody(), sampleHeaders(), HttpStatus.SC_OK);
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