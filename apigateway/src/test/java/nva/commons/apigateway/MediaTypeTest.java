package nva.commons.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MediaTypeTest {

    @Test
    void parseSimpleType() {
        var mediaType = MediaType.parse("application/json");
        assertThat(mediaType.toString()).isEqualTo("application/json");
    }

    @Test
    void parseTypeWithCharset() {
        var mediaType = MediaType.parse("application/json; charset=utf-8");
        assertThat(mediaType.toString()).isEqualTo("application/json; charset=utf-8");
    }

    @Test
    void parseTypeWithWhitespace() {
        var mediaType = MediaType.parse("  text/html ;  charset=UTF-8 ");
        assertThat(mediaType.toString()).isEqualTo("text/html; charset=UTF-8");
    }

    @Test
    void createFromComponents() {
        var mediaType = MediaType.create("application", "ld+json");
        assertThat(mediaType.toString()).isEqualTo("application/ld+json");
    }

    @Test
    void withoutParametersStripsCharset() {
        var withCharset = MediaType.parse("application/json; charset=utf-8");
        var withoutParams = withCharset.withoutParameters();
        assertThat(withoutParams.toString()).isEqualTo("application/json");
    }

    @Test
    void withoutParametersOnTypeWithNoParamsReturnsSameValue() {
        var mediaType = MediaType.create("text", "plain");
        assertThat(mediaType.withoutParameters().toString()).isEqualTo("text/plain");
    }

    @Test
    void isMatchesWildcardType() {
        var json = MediaType.parse("application/json");
        var anyType = MediaType.ANY_TYPE;
        assertThat(json.matches(anyType)).isTrue();
    }

    @Test
    void isMatchesWildcardSubtype() {
        var json = MediaType.parse("application/json");
        var anyApplication = MediaType.parse("application/*");
        assertThat(json.matches(anyApplication)).isTrue();
    }

    @Test
    void isDoesNotMatchDifferentType() {
        var json = MediaType.parse("application/json");
        var textPlain = MediaType.parse("text/plain");
        assertThat(json.matches(textPlain)).isFalse();
    }

    @Test
    void isDoesNotMatchDifferentSubtype() {
        var json = MediaType.parse("application/json");
        var xml = MediaType.parse("application/xml");
        assertThat(json.matches(xml)).isFalse();
    }

    @Test
    void isMatchesExactSameType() {
        var json = MediaType.parse("application/json");
        var alsoJson = MediaType.parse("application/json");
        assertThat(json.matches(alsoJson)).isTrue();
    }

    @Test
    void equalsAndHashCodeForEqualTypes() {
        var a = MediaType.parse("application/json; charset=utf-8");
        var b = MediaType.parse("application/json; charset=utf-8");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equalsIsCaseInsensitive() {
        var a = MediaType.parse("Application/JSON; charset=UTF-8");
        var b = MediaType.parse("application/json; charset=utf-8");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void notEqualWhenDifferentCharset() {
        var a = MediaType.parse("text/html; charset=utf-8");
        var b = MediaType.parse("text/html; charset=iso-8859-1");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void notEqualWhenOneHasCharset() {
        var a = MediaType.parse("text/html; charset=utf-8");
        var b = MediaType.create("text", "html");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void predefinedConstantsHaveExpectedValues() {
        assertThat(MediaType.JSON_UTF_8.toString()).isEqualTo("application/json; charset=UTF-8");
        assertThat(MediaType.XML_UTF_8.toString()).isEqualTo("text/xml; charset=UTF-8");
        assertThat(MediaType.HTML_UTF_8.toString()).isEqualTo("text/html; charset=UTF-8");
        assertThat(MediaType.ANY_TYPE.toString()).isEqualTo("*/*");
    }
}
