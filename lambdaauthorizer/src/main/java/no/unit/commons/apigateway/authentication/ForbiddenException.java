package no.unit.commons.apigateway.authentication;

public class ForbiddenException extends Exception {

    public static final String DEFAULT_MESSAGE = "Forbidden";

    public ForbiddenException() {
        super(DEFAULT_MESSAGE);
    }
}
