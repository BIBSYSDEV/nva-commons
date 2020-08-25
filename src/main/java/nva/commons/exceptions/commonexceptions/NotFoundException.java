package nva.commons.exceptions.commonexceptions;

import nva.commons.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class NotFoundException extends ApiGatewayException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(Exception exception) {
        super(exception);
    }

    public NotFoundException(Exception exception, String message) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_NOT_FOUND;
    }
}
