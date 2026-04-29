package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;

public class BadRequestException extends ApiGatewayException {

    private static final List<InvalidParam> EMPTY_LIST = Collections.emptyList();

    private final List<InvalidParam> invalidParams;

    public BadRequestException(String message) {
        super(message);
        this.invalidParams = EMPTY_LIST;
    }

    public BadRequestException(String message, List<InvalidParam> invalidParams) {
        super(message);
        this.invalidParams = invalidParams;
    }

    public BadRequestException(String message, Exception cause) {
        super(cause, message);
        this.invalidParams = EMPTY_LIST;
    }

    public List<InvalidParam> getInvalidParams() {
        return invalidParams;
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}
