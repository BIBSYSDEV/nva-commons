package no.unit.nva.auth.utils;

import java.net.http.HttpRequest;
import org.mockito.ArgumentMatcher;

/**
 * A ArgumentMatcher that will match whenever 2 HttpRequests have the same URI, Headers and Method. Does not compare the
 * body
 */
public class HttpRequestMetadataMatcher implements ArgumentMatcher<HttpRequest> {

    private HttpRequest sourceRequest;

    public HttpRequestMetadataMatcher(HttpRequest sourceRequest) {
        this.sourceRequest = sourceRequest;
    }

    @Override
    public boolean matches(HttpRequest request) {
        return request.method().equals(sourceRequest.method())
               && request.uri().equals(sourceRequest.uri())
               && request.headers().equals(sourceRequest.headers());
    }
}
