package nva.commons.utils;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import nva.commons.handlers.RequestInfo;

public final class RequestUtils {

    public static final String MISSING_FROM_HEADERS = "Missing from headers: ";
    public static final String MISSING_FROM_QUERY_PARAMETERS = "Missing from query parameters: ";
    public static final String MISSING_FROM_PATH_PARAMETERS = "Missing from pathParameters: ";
    public static final String MISSING_FROM_REQUEST_CONTEXT = "Missing from requestContext: ";

    private RequestUtils() {
    }

    public static String getHeader(RequestInfo requestInfo, String header){
        return Optional.ofNullable(requestInfo.getHeaders().get(header))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_HEADERS + header));
    }

    public static String getQueryParameter(RequestInfo requestInfo, String parameter) {
        return Optional.ofNullable(requestInfo.getQueryParameters().get(parameter))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_QUERY_PARAMETERS + parameter));
    }

    public static String getPathParameter(RequestInfo requestInfo, String parameter) {
        return Optional.ofNullable(requestInfo.getPathParameters().get(parameter))
            .orElseThrow(() -> new IllegalArgumentException(MISSING_FROM_PATH_PARAMETERS + parameter));
    }

    public static String getRequestContextParameter(RequestInfo requestInfo, JsonPointer jsonPointer) {
        JsonNode jsonNode = requestInfo.getRequestContext().at(jsonPointer);
        if (jsonNode.isMissingNode()) {
            throw new IllegalArgumentException(MISSING_FROM_REQUEST_CONTEXT + jsonPointer.toString());
        }
        return  jsonNode.textValue();
    }
}
