package no.unit.nva.commons.json.deserialization;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JsonLdContextDeserializerTest {

    public static final ObjectMapper DTO_OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

    public static Stream<Named<String>> jsonLdProvider() {
        return Stream.of(Named.of("Inline context", generateWrappedJsonLdInlineContext()),
                         Named.of("URI context", generateWrappedJsonLdUriContext()));

    }

    @ParameterizedTest
    @DisplayName("Should deserialize context used in class")
    @MethodSource("jsonLdProvider")
    void shouldRoundTripJsonLdContext(String json) {
        var deserialized = attempt(() -> DTO_OBJECT_MAPPER.readValue(json, Thing.class)).orElseThrow();
        var serialized = asString(deserialized);
        assertThat(serialized, is(equalTo(json)));
    }

    private static String generateWrappedJsonLdUriContext() {
        return "{\n"
               + "  \"type\" : \"Thing\",\n"
               + "  \"@context\" : \"https://example.org/jsonld\"\n"
               + "}";
    }

    private static String generateWrappedJsonLdInlineContext() {
        return "{\n"
               + "  \"type\" : \"Thing\",\n"
               + "  \"@context\" : {\n"
               + "    \"@vocab\" : \"https://example.org/vocab\"\n"
               + "  }\n"
               + "}";
    }

    private static String asString(Thing value) {
        return attempt(() -> DTO_OBJECT_MAPPER.writeValueAsString(value)).orElseThrow();
    }
}
