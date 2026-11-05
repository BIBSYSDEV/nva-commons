package nva.commons.apigateway.exceptions;

import java.io.Serializable;

public record ValidationError(String detail, String pointer) implements Serializable {}
