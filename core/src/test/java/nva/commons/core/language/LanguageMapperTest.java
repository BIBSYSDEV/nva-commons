package nva.commons.core.language;

import static nva.commons.core.language.LanguageMapper.ERROR_MESSAGE_MISSING_RESOURCE_EXCEPTION;
import static nva.commons.core.language.LanguageMapper.LEXVO_URI_PREFIX;
import static nva.commons.core.language.LanguageMapper.LEXVO_URI_UNDEFINED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import java.net.URI;
import java.util.Optional;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class LanguageMapperTest {

    public static final String ISO3_LANG_CODE = "eng";
    public static final String ISO1_LANG_CODE = "en";
    public static final String ISO3_LANG_CODE_UPPERCASE = "ENG";
    public static final String ISO1_LANG_CODE_UPPERCASE = "EN";
    public static final String NON_EXISTENT_CODE = "afadfad";

    @Test
    public void toUriOptReturnsUriWhenInputIsIso3() {
        URI expectedOutput = URI.create(LEXVO_URI_PREFIX + ISO3_LANG_CODE);
        Optional<URI> actualOutput = LanguageMapper.toUriOpt(ISO3_LANG_CODE);
        assertThat(actualOutput.isPresent(), is(true));
        assertThat(actualOutput.orElseThrow(), is(equalTo(expectedOutput)));
    }

    @Test
    public void toUriOptReturnsUriWhenInputIsIso1() {
        URI expectedOutput = URI.create(LEXVO_URI_PREFIX + ISO3_LANG_CODE);
        Optional<URI> actualOutput = LanguageMapper.toUriOpt(ISO1_LANG_CODE);
        assertThat(actualOutput.isPresent(), is(true));
        assertThat(actualOutput.orElseThrow(), is(equalTo(expectedOutput)));
    }

    @Test
    public void toUriOptReturnsOptionalEmptyWhenInputLanguageCodeIsNotFound() {
        Optional<URI> actualOutput = LanguageMapper.toUriOpt(NON_EXISTENT_CODE);
        assertThat(actualOutput, is(equalTo(Optional.empty())));
    }

    @Test
    public void toUriReturnsUndefinedWhenInputLanguageCodeIsNotFound() {
        URI actualOutput = LanguageMapper.toUri(NON_EXISTENT_CODE);
        assertThat(actualOutput, is(equalTo(LEXVO_URI_UNDEFINED)));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ISO3_LANG_CODE_UPPERCASE, ISO1_LANG_CODE_UPPERCASE})
    public void toUriOptReturnsUriWhenInputIsUpperCase(String upperCaseLangCode) {
        URI expectedOutput = URI.create(LEXVO_URI_PREFIX + ISO3_LANG_CODE);
        Optional<URI> actualOutput = LanguageMapper.toUriOpt(upperCaseLangCode);
        assertThat(actualOutput.isPresent(), is(true));
        assertThat(actualOutput.orElseThrow(), is(equalTo(expectedOutput)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void toUriReturnUndefinedWhenInputIsEmpty(String emptyLanguageCode) {
        URI actualOutput = LanguageMapper.toUri(emptyLanguageCode);
        assertThat(actualOutput, is(equalTo(LEXVO_URI_UNDEFINED)));
    }

    @Test
    public void toUriWritesFailureMessageInLogWhenFailing() {
        TestAppender appender = LogUtils.getTestingAppender(LanguageMapper.class);
        LanguageMapper.toUri(NON_EXISTENT_CODE);
        String expectedValue = ERROR_MESSAGE_MISSING_RESOURCE_EXCEPTION + NON_EXISTENT_CODE;
        assertThat(appender.getMessages(), containsString(expectedValue));
    }
}
