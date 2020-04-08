package nva.commons.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiIoExceptionTest {

    @Test
    @DisplayName("statusCode returns Internal server error")
    public void statusCodeReturnsInternalServerError() {
        assertThat(new ApiIoException(null, null).statusCode(), is(equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
    }
}