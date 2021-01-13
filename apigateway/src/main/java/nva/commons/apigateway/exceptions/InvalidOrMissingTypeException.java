package nva.commons.apigateway.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.net.HttpURLConnection;

public class InvalidOrMissingTypeException extends ApiGatewayException {

    public static final String MESSAGE = "JSON object is missing a type attribute";

    public InvalidOrMissingTypeException(InvalidTypeIdException e) {
        super(e, MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_REQUEST;
    }
}
