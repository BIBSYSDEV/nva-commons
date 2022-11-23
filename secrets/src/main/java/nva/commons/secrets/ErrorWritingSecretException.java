package nva.commons.secrets;

import nva.commons.core.JacocoGenerated;

public class ErrorWritingSecretException extends RuntimeException {

    public static final String COULD_NOT_WRITE_SECRET_ERROR = "Could not write secret: ";

    @JacocoGenerated
    public ErrorWritingSecretException() {
        super(COULD_NOT_WRITE_SECRET_ERROR);
    }

}
