package nva.commons.exceptions;

import java.nio.file.Path;

public class ResourceNotFoundException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Could not find resource in path:";

    public ResourceNotFoundException(Path path, Exception exc) {
        super(ERROR_MESSAGE + path.toString(), exc);
    }
}
