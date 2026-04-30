package nva.commons.apigateway.exceptions;

public record ValidationError(String detail, String pointer) {}
