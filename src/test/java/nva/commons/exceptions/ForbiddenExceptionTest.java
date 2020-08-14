package nva.commons.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class ForbiddenExceptionTest {

    @Test
    public void forbiddenExceptionReturnsForbiddenStatusCode() {
        ForbiddenException exception = new ForbiddenException();
        assertThat(exception.getStatusCode(), is(equalTo(HttpStatus.SC_FORBIDDEN)));
    }
}