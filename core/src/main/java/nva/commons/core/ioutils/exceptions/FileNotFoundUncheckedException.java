package nva.commons.core.ioutils.exceptions;

import java.nio.file.Path;

public class FileNotFoundUncheckedException extends RuntimeException {

    public static final String FILE_NOT_FOUND_MESSAGE = "File not found: ";

    public FileNotFoundUncheckedException(Path path, Exception e) {
        super(FILE_NOT_FOUND_MESSAGE + path.toString(), e);
    }
}
