package nva.commons.utils;

import static nva.commons.utils.JsonUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import nva.commons.handlers.RequestInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RequestUtilsTest {

    private RequestInfo requestInfo;
    public static String KEY = "key";
    public static String VALUE = "value";

    @BeforeEach
    public void setUp() {
        requestInfo = Mockito.mock(RequestInfo.class);
    }

    @Test
    public void getHeaderReturnsValueOnValidKey() {
        when(requestInfo.getHeaders()).thenReturn(Map.of(KEY, VALUE));
        String value = RequestUtils.getHeader(requestInfo, KEY);
        assertEquals(VALUE, value);
    }

    @Test
    public void getHeaderThrowsExceptionOnMissingKey() {
        when(requestInfo.getHeaders()).thenReturn(Map.of());
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            () -> RequestUtils.getHeader(requestInfo, KEY));
        assertEquals(RequestUtils.MISSING_FROM_HEADERS + KEY, exception.getMessage());
    }

    @Test
    public void getQueryParameterReturnsValueOnValidKey() {
        when(requestInfo.getQueryParameters()).thenReturn(Map.of(KEY, VALUE));
        String value = RequestUtils.getQueryParameter(requestInfo, KEY);
        assertEquals(VALUE, value);
    }

    @Test
    public void getQueryParameterThrowsExceptionOnMissingKey() {
        when(requestInfo.getQueryParameters()).thenReturn(Map.of());
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            () -> RequestUtils.getQueryParameter(requestInfo, KEY));
        assertEquals(RequestUtils.MISSING_FROM_QUERY_PARAMETERS + KEY, exception.getMessage());
    }

    @Test
    public void getPathParameterReturnsValueOnValidKey() {
        when(requestInfo.getPathParameters()).thenReturn(Map.of(KEY, VALUE));
        String value = RequestUtils.getPathParameter(requestInfo, KEY);
        assertEquals(VALUE, value);
    }

    @Test
    public void getPathParametersThrowsExceptionOnMissingKey() {
        when(requestInfo.getPathParameters()).thenReturn(Map.of());
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            () -> RequestUtils.getPathParameter(requestInfo, KEY));
        assertEquals(RequestUtils.MISSING_FROM_PATH_PARAMETERS + KEY, exception.getMessage());
    }

    @Test
    public void getRequestContextParameterReturnsValueOnValidJsonPointer() {
        when(requestInfo.getRequestContext()).thenReturn(
            objectMapper.convertValue(Map.of(KEY, VALUE), JsonNode.class));
        String value = RequestUtils.getRequestContextParameter(requestInfo, JsonPointer.compile("/" + KEY));
        assertEquals(VALUE, value);
    }

    @Test
    public void getRequestContextParameterThrowsExceptionOnInvalidJsonPointer() {
        when(requestInfo.getRequestContext()).thenReturn(
            objectMapper.createObjectNode());
        JsonPointer jsonPointer = JsonPointer.compile("/" + KEY);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            () -> RequestUtils.getRequestContextParameter(requestInfo, jsonPointer)
        );
        assertEquals(RequestUtils.MISSING_FROM_REQUEST_CONTEXT + jsonPointer, exception.getMessage());
    }

}
