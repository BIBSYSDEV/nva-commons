package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class UnsupportedMediaTypeException extends ApiGatewayException {

  public UnsupportedMediaTypeException(String message) {
    super(message);
  }

  @Override
  protected Integer statusCode() {
    return HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
  }
}
