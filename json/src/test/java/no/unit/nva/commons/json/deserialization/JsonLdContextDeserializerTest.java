package no.unit.nva.commons.json.deserialization;

import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonLdContext;
import no.unit.nva.commons.json.JsonLdContextDeserializer;
import no.unit.nva.commons.json.JsonLdContextUri;
import no.unit.nva.commons.json.JsonLdInlineContext;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JsonLdContextDeserializerTest {

    public static final ObjectMapper DTO_OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

    public static Stream<Named<String>> jsonLdContextProvider() {
        return Stream.of(Named.of("URI context", generateJsonLdContextUri()),
                         Named.of("Inline context", generateJsonLdInlineContext()));
    }

    public static Stream<Named<String>> jsonLdProvider() {
        return Stream.of(Named.of("Inline context", generateWrappedJsonLdInlineContext()),
                         Named.of("URI context", generateWrappedJsonLdUriContext()));

    }

    private static String generateWrappedJsonLdUriContext() {
        return "{\n"
               + "  \"@context\": \"https://example.org/jsonld\",\n"
               + "  \"type\": \"Thing\"\n"
               + "}";
    }

    private static String generateWrappedJsonLdInlineContext() {
        return "{\n"
               + "  \"@context\": {\n"
               + "    \"@vocab\": \"https://example.org/vocab\"\n"
               + "  },\n"
               + "  \"type\": \"Thing\"\n"
               + "}";
    }

    @ParameterizedTest
    @DisplayName("should deserialize contexts with DTO-mapper")
    @MethodSource("jsonLdContextProvider")
    void shouldDeserializeJsonLdContextsWithDtoMapper(String jsonLdContext) {
        assertDoesNotThrow(() -> DTO_OBJECT_MAPPER.readValue(jsonLdContext, JsonLdContext.class));
    }

    @ParameterizedTest
    @DisplayName("Should deserialize context used in class")
    @MethodSource("jsonLdProvider")
    void shouldDeserializeWrappedInlineContext(String json) {
        assertDoesNotThrow(() -> DTO_OBJECT_MAPPER.readValue(json, Thing.class));
    }

    private static String generateJsonLdInlineContext() {
        var contextNode = DTO_OBJECT_MAPPER.createObjectNode();
        contextNode.put("@vocab", "https://example.org/vocab");
        var value = new JsonLdInlineContext(contextNode);
        return asString(value);
    }

    private static String generateJsonLdContextUri() {
        var value = new JsonLdContextUri(URI.create("https://example.org/jsonldcontext"));
        return asString(value);
    }

    private static String asString(JsonLdContext value) {
        return attempt(() -> DTO_OBJECT_MAPPER.writeValueAsString(value)).orElseThrow();
    }
}
