package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;

public class BadRequestException extends ApiGatewayException implements WithErrors {

    private static final List<ValidationError> EMPTY_LIST = Collections.emptyList();

    private final transient List<ValidationError> errors;

    public BadRequestException(String message) {
        super(message);
        this.errors = EMPTY_LIST;
    }

    public BadRequestException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }

    public BadRequestException(String message, Exception cause) {
        super(cause, message);
        this.errors = EMPTY_LIST;
    }

    @Override
    public List<ValidationError> getErrors() {
        return errors;
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}
