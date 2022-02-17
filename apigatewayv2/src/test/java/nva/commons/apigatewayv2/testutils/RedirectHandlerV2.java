package nva.commons.apigatewayv2.testutils;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.jr.ob.JSON;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.net.URI;
import nva.commons.apigatewayv2.exceptions.ApiGatewayException;
import nva.commons.apigatewayv2.exceptions.RedirectException;

public class RedirectHandlerV2 extends HandlerV2 {

    private final URI location;
    private final Integer redirectStatusCode;

    public RedirectHandlerV2(URI location, Integer redirectStatusCode) {
        super();
        this.location = location;
        this.redirectStatusCode = redirectStatusCode;
    }

    @Override
    protected RequestBody processInput(String input, APIGatewayProxyRequestEvent requestInfo, Context context)
        throws ApiGatewayException {
        if (clientIsRequestingHtmlContent(requestInfo)) {
            throw new ExampleRedirectException(redirectStatusCode, location);
        }
        return attempt(() -> JSON.std.beanFrom(RequestBody.class, input)).orElse(fail -> null);
    }

    private boolean clientIsRequestingHtmlContent(APIGatewayProxyRequestEvent requestInfo) {
        var acceptHeader = requestInfo.getHeaders().get(HttpHeaders.ACCEPT);
        MediaType requestedContent = MediaType.parse(acceptHeader).withoutParameters();
        return MediaType.HTML_UTF_8.withoutParameters().equals(requestedContent);
    }

    private static final class ExampleRedirectException extends RedirectException {

        private final URI location;
        private final Integer statusCode;

        public ExampleRedirectException(Integer statusCode, URI location) {
            super("Redirection");
            this.location = location;
            this.statusCode = statusCode;
        }

        @Override
        public URI getLocation() {
            return location;
        }

        @Override
        protected Integer statusCode() {
            return statusCode;
        }
    }
}
