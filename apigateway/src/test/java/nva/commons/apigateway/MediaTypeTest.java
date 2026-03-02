package nva.commons.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaTypeTest {

    @Test
    void shouldParseSimpleMediaType() {
        var mediaType = MediaType.parse("application/json");
        assertThat(mediaType.toString()).isEqualTo("application/json");
    }

    @Test
    void shouldParseMediaTypeWithCharset() {
        var mediaType = MediaType.parse("application/json; charset=utf-8");
        assertThat(mediaType.toString()).isEqualTo("application/json; charset=utf-8");
    }

    @Test
    void shouldParseMediaTypeWithWhitespace() {
        var mediaType = MediaType.parse("  text/html ;  charset=utf-8 ");
        assertThat(mediaType.toString()).isEqualTo("text/html; charset=utf-8");
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
    void shouldMatchWildcardType() {
        var json = MediaType.parse("application/json");
        assertThat(json.matches(MediaType.ANY_TYPE)).isTrue();
    }

    @Test
    void shouldMatchWildcardSubtype() {
        var json = MediaType.parse("application/json");
        var anyApplication = MediaType.parse("application/*");
        assertThat(json.matches(anyApplication)).isTrue();
    }

    @Test
    void shouldNotMatchDifferentType() {
        var json = MediaType.parse("application/json");
        var textPlain = MediaType.parse("text/plain");
        assertThat(json.matches(textPlain)).isFalse();
    }

    @Test
    void shouldNotMatchDifferentSubtype() {
        var json = MediaType.parse("application/json");
        var xml = MediaType.parse("application/xml");
        assertThat(json.matches(xml)).isFalse();
    }

    @Test
    void shouldMatchExactSameType() {
        var json = MediaType.parse("application/json");
        var alsoJson = MediaType.parse("application/json");
        assertThat(json.matches(alsoJson)).isTrue();
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

    @Test
    void shouldHaveExpectedValuesForPredefinedConstants() {
        assertThat(MediaType.JSON_UTF_8.toString()).isEqualTo("application/json; charset=utf-8");
        assertThat(MediaType.XML_UTF_8.toString()).isEqualTo("text/xml; charset=utf-8");
        assertThat(MediaType.HTML_UTF_8.toString()).isEqualTo("text/html; charset=utf-8");
        assertThat(MediaType.ANY_TYPE.toString()).isEqualTo("*/*");
        assertThat(MediaType.ANY_APPLICATION_TYPE.toString()).isEqualTo("application/*");
        assertThat(MediaType.ANY_TEXT_TYPE.toString()).isEqualTo("text/*");
        assertThat(MediaType.XHTML_UTF_8.toString()).isEqualTo("application/xhtml+xml; charset=utf-8");
        assertThat(MediaType.MICROSOFT_EXCEL.toString()).isEqualTo("application/vnd.ms-excel");
        assertThat(MediaType.OOXML_SHEET.toString())
            .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    void shouldMatchApplicationSubtypesWithAnyApplicationType() {
        assertThat(MediaType.JSON_UTF_8.matches(MediaType.ANY_APPLICATION_TYPE)).isTrue();
    }

    @Test
    void shouldNotMatchTextSubtypesWithAnyApplicationType() {
        assertThat(MediaType.HTML_UTF_8.matches(MediaType.ANY_APPLICATION_TYPE)).isFalse();
    }

    @Test
    void shouldMatchTextSubtypesWithAnyTextType() {
        assertThat(MediaType.HTML_UTF_8.matches(MediaType.ANY_TEXT_TYPE)).isTrue();
    }

    @Test
    void shouldNotMatchApplicationSubtypesWithAnyTextType() {
        assertThat(MediaType.JSON_UTF_8.matches(MediaType.ANY_TEXT_TYPE)).isFalse();
    }
}
