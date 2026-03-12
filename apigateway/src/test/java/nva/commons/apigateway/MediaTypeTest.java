package nva.commons.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MediaTypeTest {

    static Stream<Arguments> parseProvider() {
        return Stream.of(
            Arguments.of("application/json", "application/json"),
            Arguments.of("application/json; charset=utf-8", "application/json; charset=utf-8"),
            Arguments.of("  text/html ;  charset=utf-8 ", "text/html; charset=utf-8"));
    }

    static Stream<Arguments> predefinedConstantsProvider() {
        return Stream.of(
            Arguments.of(MediaType.CSV_UTF_8, "text/csv; charset=utf-8"),
            Arguments.of(MediaType.JSON_UTF_8, "application/json; charset=utf-8"),
            Arguments.of(MediaType.XML_UTF_8, "text/xml; charset=utf-8"),
            Arguments.of(MediaType.HTML_UTF_8, "text/html; charset=utf-8"),
            Arguments.of(MediaType.ANY_TYPE, "*/*"),
            Arguments.of(MediaType.ANY_APPLICATION_TYPE, "application/*"),
            Arguments.of(MediaType.ANY_TEXT_TYPE, "text/*"),
            Arguments.of(MediaType.XHTML_UTF_8, "application/xhtml+xml; charset=utf-8"),
            Arguments.of(MediaType.MICROSOFT_EXCEL, "application/vnd.ms-excel"),
            Arguments.of(
                MediaType.OOXML_SHEET,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    static Stream<Arguments> matchingProvider() {
        return Stream.of(
            Arguments.of("application/json", "*/*", true),
            Arguments.of("application/json", "application/*", true),
            Arguments.of("application/json", "application/json", true),
            Arguments.of("application/json", "text/plain", false),
            Arguments.of("application/json", "application/xml", false),
            Arguments.of("application/json; charset=utf-8", "application/*", true),
            Arguments.of("text/html; charset=utf-8", "application/*", false),
            Arguments.of("text/html; charset=utf-8", "text/*", true),
            Arguments.of("application/json; charset=utf-8", "text/*", false));
    }

    @ParameterizedTest
    @MethodSource("parseProvider")
    void shouldParseMediaType(String input, String expected) {
        var mediaType = MediaType.parse(input);
        assertThat(mediaType.toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("predefinedConstantsProvider")
    void shouldHaveExpectedValuesForPredefinedConstants(MediaType constant, String expected) {
        assertThat(constant.toString()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("matchingProvider")
    void shouldMatchCorrectly(String mediaType, String pattern, boolean shouldMatch) {
        var parsed = MediaType.parse(mediaType);
        var parsedPattern = MediaType.parse(pattern);
        assertThat(parsed.matches(parsedPattern)).isEqualTo(shouldMatch);
    }

    @Test
    void shouldCreateMediaTypeFromComponents() {
        var mediaType = MediaType.create("application", "ld+json");
        assertThat(mediaType.toString()).isEqualTo("application/ld+json");
    }

    @Test
    void shouldStripCharsetWithWithoutParameters() {
        var withCharset = MediaType.parse("application/json; charset=utf-8");
        var withoutParams = withCharset.withoutParameters();
        assertThat(withoutParams.toString()).isEqualTo("application/json");
    }

    @Test
    void shouldReturnSameValueWhenWithoutParametersCalledOnTypeWithNoParams() {
        var mediaType = MediaType.create("text", "plain");
        assertThat(mediaType.withoutParameters().toString()).isEqualTo("text/plain");
    }

    @Test
    void shouldBeEqualAndHaveSameHashCodeForEqualTypes() {
        var a = MediaType.parse("application/json; charset=utf-8");
        var b = MediaType.parse("application/json; charset=utf-8");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldBeEqualRegardlessOfCase() {
        var a = MediaType.parse("Application/JSON; charset=utf-8");
        var b = MediaType.parse("application/json; charset=utf-8");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentCharset() {
        var a = MediaType.parse("text/html; charset=utf-8");
        var b = MediaType.parse("text/html; charset=iso-8859-1");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void shouldNotBeEqualWhenOnlyOneHasCharset() {
        var a = MediaType.parse("text/html; charset=utf-8");
        var b = MediaType.create("text", "html");
        assertThat(a).isNotEqualTo(b);
    }
}