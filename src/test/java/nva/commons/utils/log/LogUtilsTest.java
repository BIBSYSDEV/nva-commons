package nva.commons.utils.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogUtilsTest {

    public static final String ANY_VALUE = "ANY_VALUE";
    private Environment environment;

    @BeforeEach
    public void setup() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(anyString())).thenReturn(ANY_VALUE);
    }

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