package nva.commons.core.useragent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import org.junit.jupiter.api.Test;

class UserAgentTest {

    @Test
    void shouldAllowCreationOfUserAgentString() {
        var actual = UserAgent.newBuilder()
                         .client(UserAgentTest.class)
                         .environment("dev")
                         .version("1.0")
                         .repository(URI.create("https://example.org/someRepo"))
                         .email("someone@example.org")
                         .build();
        var expected = "UserAgentTest-dev/1.0 (https://example.org/someRepo; mailto:someone@example.org)";
        assertThat(actual.toString(), is(equalTo(expected)));
    }

    @Test
    void shouldThrowWhenBuilderValuesAreNull() {
        var builder = UserAgent.newBuilder();
        assertThrows(InvalidUserAgentException.class, builder::build);
    }

    @Test
    void shouldHaveConstantForUserAgentHeaderString() {
        assertThat(UserAgent.USER_AGENT, is(equalTo("User-Agent")));
    }
}
