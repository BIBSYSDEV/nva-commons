package nva.commons.apigateway.exceptions;

import java.util.List;

@FunctionalInterface
public interface WithErrors {

    List<ValidationError> getErrors();
}
