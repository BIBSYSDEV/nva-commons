package nva.commons.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestContextTest {

    @DisplayName("TestContext contains a TestLogger")
    @Test
    public void testContextContainsATestLogger() {

        String expected = "Some message";
        TestContext testContext = new TestContext();
        testContext.getLogger().log(expected);
        String actual = testContext.logger.getLogs().strip();
        assertThat(actual, is(equalTo(expected)));
    }
}