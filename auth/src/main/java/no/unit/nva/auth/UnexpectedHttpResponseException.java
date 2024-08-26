package no.unit.nva.auth;

import java.net.http.HttpResponse;

public class UnexpectedHttpResponseException extends RuntimeException {

    public static final String UNEXPECTED_HTTP_RESPONSE_MESSAGE = "Got unexpected http response: %s %s";

    public UnexpectedHttpResponseException(int statusCode, String message) {
        super(String.format(UNEXPECTED_HTTP_RESPONSE_MESSAGE, statusCode, message));
    }

    public static UnexpectedHttpResponseException fromHttpResponse(HttpResponse httpResponse) {
        return new UnexpectedHttpResponseException(httpResponse.statusCode(), httpResponse.body().toString());
    }
}