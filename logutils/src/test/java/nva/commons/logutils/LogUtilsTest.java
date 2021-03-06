package nva.commons.logutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import org.junit.jupiter.api.Test;

class LogUtilsTest {

    @Test
    public void getTestingAppenderReturnsAnAppenderWithTheLoggedMessages() {
        TestAppender appender = LogUtils.getTestingAppender(DummyClassForLogTesting.class);

        assertThatAppenderCapturesLogMessagesFromCustomClass(appender);
    }

    @Test
    public void toLoggerNameReturnsTheNameOfTheClass() {
        String loggerName = LogUtils.toLoggerName(SamplePojo.class);
        assertThat(loggerName, is(equalTo(SamplePojo.class.getCanonicalName())));
    }

    @Test
    public void getTestingAppenderForRootLoggerReturnsAppenderThatIncludesMessagesForClassOfInterestAsWell() {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();

        assertThatAppenderCapturesLogMessagesFromCustomClass(appender);
    }

    private void assertThatAppenderCapturesLogMessagesFromCustomClass(TestAppender appender) {
        DummyClassForLogTesting loggingObject = new DummyClassForLogTesting();
        String someMessage = "Some message";

        loggingObject.logMessage(someMessage);
        String actual = appender.getMessages();
        assertThat(actual, containsString(someMessage));
    }
}