package nva.commons.apigatewayv2.exceptions;

import java.net.URI;

public abstract class RedirectException extends ApiGatewayException {

    public RedirectException(String message) {
        super(message);
    }

    public abstract URI getLocation();
}
