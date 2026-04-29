package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;
import java.util.List;

public class BadRequestException extends ApiGatewayException implements WithErrors {

    private final transient List<ValidationError> errors;

    public BadRequestException(String message) {
        super(message);
        this.errors = List.of();
    }

    public BadRequestException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public BadRequestException(String message, Exception cause) {
        super(cause, message);
        this.errors = List.of();
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
