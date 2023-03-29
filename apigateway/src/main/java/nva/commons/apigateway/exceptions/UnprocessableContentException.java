package nva.commons.apigateway.exceptions;

public class UnprocessableContentException extends ApiGatewayException {

    public static final int HTTP_UNPROCESSABLE_CONTENT = 422;

    public UnprocessableContentException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HTTP_UNPROCESSABLE_CONTENT;
    }
}
