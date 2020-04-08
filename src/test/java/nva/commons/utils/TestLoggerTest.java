package nva.commons.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestLoggerTest {

    private static final String MESSAGE = "Some log message";

    @Test
    @DisplayName("log appends string to the buffer")
    void logAppendsStringToTheBuffer() {
        TestLogger logger = new TestLogger();
        logger.log(MESSAGE);
        assertThat(logger.getLogs(), containsString(MESSAGE));
    }

    @Test
    @DisplayName("log appends string to the buffer")
    void logAppendsBytesToTheBuffer() {
        TestLogger logger = new TestLogger();
        logger.log(MESSAGE.getBytes());
        assertThat(logger.getLogs(), containsString(MESSAGE));
    }
}