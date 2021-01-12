package nva.commons.apigateway;

import static nva.commons.essentials.JsonUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class RequestInfoUtilityFunctionsTest {

    private RequestInfo requestInfo;
    public static String KEY = "key";
    public static String VALUE = "value";

    @BeforeEach
    public void setUp() {
        RequestInfo spiedObject = new RequestInfo();
        requestInfo = spy(spiedObject);
    }

    @Test
    public void getHeaderReturnsValueOnValidKey() {
        when(requestInfo.getHeaders()).thenReturn(Map.of(KEY, VALUE));
        String value = requestInfo.getHeader(KEY);
        assertEquals(VALUE, value);
    }

    @Test
    public void getHeaderThrowsExceptionOnMissingKey() {
        when(requestInfo.getHeaders()).thenReturn(Map.of());
        Executable action = () -> requestInfo.getHeader(KEY);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, action);
        String expected = RequestInfo.MISSING_FROM_HEADERS + KEY;
        assertEquals(expected, exception.getMessage());
    }

    @Test
    public void getQueryParameterReturnsValueOnValidKey() {
        when(requestInfo.getQueryParameters()).thenReturn(Map.of(KEY, VALUE));
        String value = requestInfo.getQueryParameter(KEY);
        assertEquals(VALUE, value);
    }

    @Test
    public void getQueryParameterThrowsExceptionOnMissingKey() {
        when(requestInfo.getQueryParameters()).thenReturn(Map.of());
        Executable action = () -> requestInfo.getQueryParameter(KEY);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, action);
        String expected = RequestInfo.MISSING_FROM_QUERY_PARAMETERS + KEY;
        assertEquals(expected, exception.getMessage());
    }

    @Test
    public void getPathParameterReturnsValueOnValidKey() {
        when(requestInfo.getPathParameters()).thenReturn(Map.of(KEY, VALUE));
        String value = requestInfo.getPathParameter(KEY);
        assertEquals(VALUE, value);
    }

    @Test
    public void getPathParametersThrowsExceptionOnMissingKey() {
        when(requestInfo.getPathParameters()).thenReturn(Map.of());
        Executable action = () -> requestInfo.getPathParameter(KEY);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            action);
        String expected = RequestInfo.MISSING_FROM_PATH_PARAMETERS + KEY;
        assertEquals(expected, exception.getMessage());
    }

    @Test
    public void getRequestContextParameterReturnsValueOnValidJsonPointer() {
        when(requestInfo.getRequestContext())
            .thenReturn(objectMapper.convertValue(Map.of(KEY, VALUE), JsonNode.class));
        String value = requestInfo.getRequestContextParameter(JsonPointer.compile("/" + KEY));
        assertEquals(VALUE, value);
    }

    @Test
    public void getRequestContextParameterThrowsExceptionOnInvalidJsonPointer() {
        when(requestInfo.getRequestContext())
            .thenReturn(objectMapper.createObjectNode());
        JsonPointer jsonPointer = JsonPointer.compile("/" + KEY);

        Executable action = () -> requestInfo.getRequestContextParameter(jsonPointer);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, action);
        String expected = RequestInfo.MISSING_FROM_REQUEST_CONTEXT + jsonPointer;
        assertEquals(expected, exception.getMessage());
    }
}
