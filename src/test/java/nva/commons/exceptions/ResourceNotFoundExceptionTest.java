package nva.commons.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ResourceNotFoundExceptionTest {

    public static final String SOME_MESSAGE = "some message";
    public static final Path SOME_PATH = Path.of("folder", "file");

    @Test
    @DisplayName("ResourceNotFoundException has a constructor that accepts a path and an exception")
    public void resourceNotFoundExceptionHasAConstructorThatAcceptsAPathAndAnException() {
        IOException cause = new IOException("IoExceptionMessage");
        ResourceNotFoundException exception = new ResourceNotFoundException(SOME_PATH, cause);
        assertThat(exception.getMessage(), containsString(ResourceNotFoundException.ERROR_MESSAGE));
        assertThat(exception.getMessage(), containsString(SOME_PATH.toString()));
    }
}
