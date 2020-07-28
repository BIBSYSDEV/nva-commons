package nva.commons.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import org.apache.http.HttpStatus;

public class InvalidOrMissingTypeException extends ApiGatewayException {

    public static final String MESSAGE = "JSON object is missing a type attribute";

    public InvalidOrMissingTypeException(InvalidTypeIdException e) {
        super(e, MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_BAD_REQUEST;
    }
}
