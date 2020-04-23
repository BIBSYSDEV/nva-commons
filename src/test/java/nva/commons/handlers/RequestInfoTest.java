package nva.commons.handlers;

import static nva.commons.utils.JsonUtils.jsonParser;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RequestInfoTest {

    private static final String API_GATEWAY_MESSAGES_FOLDER = "apiGatewayMessages";
    private static final Path NULL_VALUES_FOR_MAPS = Path.of(API_GATEWAY_MESSAGES_FOLDER,
        "queryStringParametersIsNull.json");

    @Test
    @DisplayName("RequestInfo initializes queryParameters to empty map when JSON object sets "
        + "queryStringParameters to null")
    public void requestInfoInitializesQueryParametesToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(RequestInfo::getQueryParameters);
    }

    @Test
    @DisplayName("RequestInfo initializes headers to empty map when JSON object sets "
        + "Headers to null")
    public void requestInfoInitializesHeadersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(RequestInfo::getHeaders);
    }

    @Test
    @DisplayName("RequestInfo initializes pathParameters to empty map when JSON object sets "
        + "pathParameters to null")
    public void requestInfoInitializesPathParametersToEmptyMapWhenJsonObjectsSetsQueryStringParametersToNull()
        throws JsonProcessingException {
        checkForNonNullMap(RequestInfo::getPathParameters);
    }

    private void checkForNonNullMap(Function<RequestInfo, Map<String, String>> getMap) throws JsonProcessingException {
        String apiGatewayEvent = IoUtils.stringFromResources(NULL_VALUES_FOR_MAPS);
        RequestInfo requestInfo = jsonParser.readValue(apiGatewayEvent, RequestInfo.class);
        assertNotNull(getMap.apply(requestInfo));
    }
}