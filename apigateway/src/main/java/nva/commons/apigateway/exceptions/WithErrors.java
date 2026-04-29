package nva.commons.apigateway.exceptions;

import java.util.List;

public interface WithErrors {

    List<ValidationError> getErrors();
}
