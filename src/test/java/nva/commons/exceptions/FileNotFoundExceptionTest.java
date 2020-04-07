package nva.commons.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FileNotFoundExceptionTest {

    public static final String SOME_MESSAGE = "some message";
    public static final Path SOME_PATH = Path.of("folder", "file");

    @Test
    @DisplayName("FileNotFoundUnchecked has a constructor that accepts a pah and an exception")
    public void fileNotFoundUncheckedExceptionHasAConstructorThatAcceptsAPathAndAnException() {
        IOException cause = new IOException("IoExceptionMessage");
        FileNotFoundUncheckedException exception = new FileNotFoundUncheckedException(SOME_PATH, cause);
        assertThat(exception.getMessage(), containsString(FileNotFoundUncheckedException.FILE_NOT_FOUND_MESSAGE));
        assertThat(exception.getMessage(), containsString(SOME_PATH.toString()));
    }
}
