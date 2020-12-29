package nva.commons.commons.logutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import nva.commons.commons.SamplePojo;
import nva.commons.commons.TestAppender;
import nva.commons.commons.logutils.DummyClassForLogTesting;
import nva.commons.commons.logutils.LogUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

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

    @Test
    public void toLoggerNameReturnsTheNameOfTheClass() {
        String loggerName = LogUtils.toLoggerName(SamplePojo.class);
        assertThat(loggerName,is(equalTo(SamplePojo.class.getCanonicalName())));
    }
}