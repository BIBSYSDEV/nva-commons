package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;

public class ConflictException extends ApiGatewayException {

    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    private final Map<String, String> conflictingKeys;

    public ConflictException(String message) {
        super(message);
        this.conflictingKeys = EMPTY_MAP;
    }

    public ConflictException(String message, Map<String, String> conflictingKeys) {
        super(message);
        this.conflictingKeys = conflictingKeys;
    }

    public ConflictException(Exception exception) {
        super(exception);
        this.conflictingKeys = EMPTY_MAP;
    }

    public ConflictException(Exception exception, String message) {
        super(exception, message);
        this.conflictingKeys = EMPTY_MAP;
    }

    public Map<String, String> getConflictingKeys() {
        return conflictingKeys;
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_CONFLICT;
    }
}
