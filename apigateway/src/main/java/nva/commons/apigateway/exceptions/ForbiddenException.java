package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class ForbiddenException extends ApiGatewayException {

  public static final String DEFAULT_MESSAGE = "Forbidden";

  public ForbiddenException() {
    super(DEFAULT_MESSAGE);
  }

  public ForbiddenException(String message) {
    super(message);
  }

  @Override
  protected Integer statusCode() {
    return HttpURLConnection.HTTP_FORBIDDEN;
  }
}
