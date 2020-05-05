package nva.commons.utils.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DoiConverterTest {

    private static final String DOI = "10.1000/182";
    private static final String EXPECTED = "https://doi.org/" + DOI;

    DoiConverter doiConverter = new DoiConverter();

    @DisplayName("toUri returns a URI if input is a valid DOI URI")
    @Test
    public void toUriReturnsAUriIfInputIsAValidDoiUri() {
        String input = "https://doi.org/" + DOI;
        URI actual = doiConverter.toUri(input);
        assertThat(actual.toString(), is(equalTo(input)));
    }

    @DisplayName("toUri returns a URI when input is a doi identifier")
    @Test
    public void toUriReturnsAUriIfInputIsADoiIdentifier() {
        URI actual = doiConverter.toUri(DOI);
        assertThat(actual.toString(), is(equalTo(EXPECTED)));
    }

    @DisplayName("toUri returns an HTTPS URI when input is an HTTP URI")
    @Test
    public void toUriReturnsAUriIfInputIsAnHttpDoiUri() {
        String input = "http://doi.org/" + DOI;
        URI actual = doiConverter.toUri(input);
        assertThat(actual.toString(), is(equalTo(EXPECTED)));
    }

    @Test
    @DisplayName("toUri throws Exception when input is not a valid URI")
    public void toUriThrowsAnExceptionWhenInputIsNotValidUri() {
        TestAppender appender = LogUtils.getTestingAppender(DoiConverter.class);
        String input = "http://somethingelse.org/" + DOI;
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> doiConverter.toUri(input));
        assertThat(exception.getMessage(), containsString(input));
        assertThat(appender.getMessages(), containsString(input));
    }

    @Test
    @DisplayName("toURI returns a URI when input is a DOI string with DOI prefix")
    public void toUriReturnsAUriIfUriWhenInputIsDoiStringWithDoiPrefix() {
        String input = "doi:" + DOI;
        URI actual = doiConverter.toUri(input);
        assertThat(actual.toString(), is(equalTo(EXPECTED)));
    }

    @Test
    @DisplayName("toURI throws Exception when input is an invalid DOI string")
    public void toUriThrowsExceptionWhenInputIsAnInvalidDoiString() {
        TestAppender appender = LogUtils.getTestingAppender(DoiConverter.class);
        String input = "213456";
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> doiConverter.toUri(input));
        assertThat(exception.getMessage(), containsString(input));
        assertThat(appender.getMessages(), containsString(input));
    }
}