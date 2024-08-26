package no.unit.nva.auth;

import java.net.http.HttpResponse;

public class UnhandledHttpResponseException extends RuntimeException {

    public static final String UNHANDLED_HTTP_RESPONSE_MESSAGE = "Got unknown http response: %s %s";

    public UnhandledHttpResponseException(int statusCode, String message) {
        super(String.format(UNHANDLED_HTTP_RESPONSE_MESSAGE, statusCode, message));
    }

    public static UnhandledHttpResponseException fromHttpResponse(HttpResponse httpResponse) {
        return new UnhandledHttpResponseException(httpResponse.statusCode(), httpResponse.body().toString());
    }
}