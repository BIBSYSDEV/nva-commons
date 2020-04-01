package nva.commons.exceptions;

import org.apache.http.HttpStatus;

public class ApiIoException extends ApiGatewayException {

    public ApiIoException(Exception e) {
        super(e);
    }

    @Override
    public Integer statusCode() {
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
}
