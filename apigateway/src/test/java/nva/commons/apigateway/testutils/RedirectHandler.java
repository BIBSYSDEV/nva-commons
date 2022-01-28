package nva.commons.apigateway.testutils;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.net.URI;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.RedirectException;

public class RedirectHandler extends Handler {

    private final URI location;
    private final Integer redirectStatusCode;

    public RedirectHandler(URI location, Integer redirectStatusCode) {
        super();
        this.location = location;
        this.redirectStatusCode = redirectStatusCode;
    }

    @Override
    protected RequestBody processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        if (clientIsRequestingHtmlContent(requestInfo)) {
            throw new ExampleRedirectException(redirectStatusCode, location);
        }
        return input;
    }

    private boolean clientIsRequestingHtmlContent(RequestInfo requestInfo) {
        MediaType requestedContent = MediaType.parse(requestInfo.getHeader(HttpHeaders.ACCEPT)).withoutParameters();
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
