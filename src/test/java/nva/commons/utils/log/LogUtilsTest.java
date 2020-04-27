package nva.commons.utils.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

class LogUtilsTest {

    @Test
    public void getTestingAppenderReturnsAnAppenderWithTheLoggedMessages() {
        TestAppender appender = LogUtils.getTestingAppender(DummyClassForLogTesting.class);

        DummyClassForLogTesting loggingObject = new DummyClassForLogTesting();
        String someMessage = "Some message";

        loggingObject.logMessage(someMessage);
        String actual = appender.getMessages();
        assertThat(actual, containsString(someMessage));
    }
}