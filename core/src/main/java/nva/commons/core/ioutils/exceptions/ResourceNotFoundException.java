package nva.commons.core.ioutils.exceptions;

import java.nio.file.Path;

public class ResourceNotFoundException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Could not find resource in path: ";

    @Deprecated
    public ResourceNotFoundException(Path path, Exception exc) {
        super(ERROR_MESSAGE + path, exc);
    }

    public ResourceNotFoundException(String path, Exception exc) {
        this(Path.of(path), exc);
    }
}
