package nva.commons.apigateway.mediatype;

import static nva.commons.apigateway.mediatype.MediaTypeTestSupport.hasCode;
import static nva.commons.apigateway.mediatype.MediaTypeTestSupport.parseMediaType;
import static nva.commons.apigateway.mediatype.MediaTypeTestSupport.rejected;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Compliance suite for the media-type stack, organised by the spec clause each group exercises: RFC
 * 9110 (HTTP semantics: token, quoted-string, media-type, qvalue, Accept matching), RFC 6838
 * (registration / restricted-name), RFC 6839 (structured syntax suffix), RFC 2978 (charset), and
 * RFC 6906 + the application/ld+json registration (profile).
 */
@DisplayName("Media type stack — RFC compliance")
class MediaTypeComplianceTest {

  private static final MediaTypeParser PARSER = MediaTypeParser.defaultParser();
  private static final ContentNegotiator NEGOTIATOR = ContentNegotiator.defaultNegotiator();

  private static double effectiveQuality(MediaTypeParseResult accept, MediaType rep) {
    var ranked = NEGOTIATOR.rank(accept, List.of(rep));
    return ranked.isEmpty() ? 0.0 : ranked.getFirst().quality();
  }

  @Nested
  @DisplayName("RFC 9110 §5.6.2/§5.6.4/§8.3 — lexical grammar")
  class LexicalGrammar {

    @ParameterizedTest(name = "valid subtype token: {0}")
    @ValueSource(
        strings = {"plain", "html", "vnd.api+json", "x-custom", "a", "foo123", "a!b", "a#b", "a~b"})
    void shouldAcceptValidTokens(String subtype) {
      assertThat(PARSER.parse("text/" + subtype).isAcceptable()).isTrue();
    }

    @ParameterizedTest(name = "invalid subtype token: {0}")
    @ValueSource(strings = {"a b", "a/b", "a@b", "a:b", "a(b)c", "a;b"})
    void shouldRejectNonTokenSubtypes(String subtype) {
      var result = PARSER.parse("text/" + subtype);

      assertThat(result.isAcceptable()).isFalse();
    }

    @ParameterizedTest(name = "{0} == text/html (case-insensitive)")
    @ValueSource(strings = {"Text/HTML", "TEXT/HTML", "text/Html"})
    void shouldTreatTypeAndSubtypeAsCaseInsensitive(String input) {
      assertThat(parseMediaType(input).essence()).isEqualTo("text/html");
    }

    @Test
    void shouldUnescapeQuotedPairInParameterValue() {
      assertThat(parseMediaType("text/plain; note=\"a\\\"b\"").parameters().get("note"))
          .isEqualTo("a\"b");
    }

    @Test
    void shouldPreserveSeparatorsInsideQuotedParameterValue() {
      assertThat(parseMediaType("text/plain; note=\"a, b; c\"").parameters().get("note"))
          .isEqualTo("a, b; c");
    }

    @Test
    void shouldUnescapeBackslashInParameterValue() {
      assertThat(parseMediaType("text/plain; note=\"c:\\\\path\"").parameters().get("note"))
          .isEqualTo("c:\\path");
    }

    @Test
    void shouldRejectUnterminatedQuotedString() {
      var result = PARSER.parse("text/plain; note=\"unterminated");

      assertThat(rejected(result, "malformed_quoted_string")).isTrue();
    }

    @Test
    void shouldFoldParameterNameCase() {
      var mediaType = parseMediaType("text/plain; Charset=UTF-8");

      assertThat(mediaType.parameters().get("charset")).isEqualTo("UTF-8");
      assertThat(mediaType.charsetName().orElseThrow()).isEqualTo("UTF-8");
    }
  }

  @Nested
  @DisplayName("Defensive parsing — security floor")
  class SecurityFloor {

    @ParameterizedTest(name = "control char U+{0} rejected before parsing")
    @ValueSource(chars = {'\r', '\n', '\u0000', '\u007f', '\u0001', '\u001f'})
    void shouldRejectControlCharacters(char character) {
      var result = PARSER.parse("text/html" + character);

      assertThat(rejected(result, "control_character")).isTrue();
      assertThat(result.mediaTypes()).as("nothing should be salvaged").isEmpty();
    }

    @Test
    void shouldBlockCrlfInjection() {
      var result = PARSER.parse("text/html\r\nSet-Cookie: x=1");

      assertThat(result.isValid()).isFalse();
      assertThat(result.mediaTypes()).isEmpty();
    }

    @Test
    void shouldRejectOverLengthInput() {
      var result = PARSER.parse("text/" + "a".repeat(9000));

      assertThat(rejected(result, "input_too_long")).isTrue();
    }

    @Test
    void shouldRejectTooManyListElements() {
      var result = PARSER.parseList("x/y,".repeat(70));

      assertThat(rejected(result, "too_many_elements")).isTrue();
    }

    @Test
    void shouldNotCountBlankSlotsAgainstListElementCap() {
      var input = "text/html" + ",".repeat(64);

      var result = PARSER.parseList(input);

      assertThat(rejected(result, "too_many_elements")).isFalse();
      assertThat(result.mediaTypes()).hasSize(1);
    }

    @Test
    void shouldRejectTooManyParameters() {
      var input = new StringBuilder("text/plain");
      for (int parameterIndex = 0; parameterIndex < 40; parameterIndex++) {
        input.append(";p").append(parameterIndex).append("=v");
      }

      assertThat(rejected(PARSER.parse(input.toString()), "too_many_parameters")).isTrue();
    }
  }

  @Nested
  @DisplayName("RFC 6838 — registration & restricted names")
  class Rfc6838 {

    @Test
    void shouldAllowFullTcharSetInDefaultMode() {
      assertThat(PARSER.parse("text/foo~bar").isAcceptable()).isTrue();
    }

    @Test
    void shouldRejectTcharsOutsideRestrictedNameInStrictMode() {
      var strict = MediaTypeParser.builder().enforceRestrictedNames(true).build();

      assertThat(rejected(strict.parse("text/foo~bar"), "invalid_subtype")).isTrue();
    }

    @Test
    void shouldAcceptNamesWithinSoftLimit() {
      assertThat(PARSER.parse("text/" + "a".repeat(60)).isAcceptable()).isTrue();
    }

    @ParameterizedTest(name = "name length {0} warns but is still acceptable")
    @ValueSource(ints = {65, 127})
    void shouldWarnOnNamesExceedingSoftLimit(int length) {
      var result = PARSER.parse("text/" + "a".repeat(length));

      assertThat(result.isAcceptable()).isTrue();
      assertThat(hasCode(result, "name_over_soft_limit")).isTrue();
    }

    @Test
    void shouldRejectNamesExceedingHardLimit() {
      var result = PARSER.parse("text/" + "a".repeat(128));

      assertThat(rejected(result, "name_too_long")).isTrue();
    }

    @Test
    void shouldRejectDuplicateParameter() {
      var result = PARSER.parse("text/html; charset=utf-8; charset=iso-8859-1");

      assertThat(rejected(result, "duplicate_parameter")).isTrue();
    }
  }

  @Nested
  @DisplayName("RFC 6839 — structured syntax suffix")
  class Rfc6839 {

    @ParameterizedTest(name = "{0} -> suffix {1}")
    @CsvSource({
      "application/vnd.api+json, json",
      "image/svg+xml, xml",
      "application/senml+cbor, cbor",
      "application/a+b+baz, baz",
      "application/cbor, NONE"
    })
    void shouldExtractStructuredSyntaxSuffix(String type, String expected) {
      var suffix = parseMediaType(type).structuredSyntaxSuffix().orElse("NONE");

      assertThat(suffix).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("RFC 2978 / RFC 9110 — charset & parameters")
  class Parameters {

    @Test
    void shouldAcceptResolvableCharset() {
      assertThat(PARSER.parse("text/plain; charset=utf-8").isAcceptable()).isTrue();
    }

    @Test
    void shouldRejectUnknownCharset() {
      var result = PARSER.parse("text/plain; charset=not-a-charset");

      assertThat(rejected(result, "unknown_charset")).isTrue();
    }

    @Test
    void shouldAcceptCharsetOnAllowList() {
      var parser =
          MediaTypeParser.builder()
              .allowedCharsets(StandardCharsets.UTF_8, StandardCharsets.US_ASCII)
              .build();

      assertThat(parser.parse("text/plain; charset=utf-8").isAcceptable()).isTrue();
    }

    @Test
    void shouldRejectCharsetNotOnAllowList() {
      var parser =
          MediaTypeParser.builder()
              .allowedCharsets(StandardCharsets.UTF_8, StandardCharsets.US_ASCII)
              .build();

      assertThat(rejected(parser.parse("text/plain; charset=iso-8859-1"), "charset_not_allowed"))
          .isTrue();
    }

    @Test
    @DisplayName("whitespace around '=' is tolerated and flagged as normalized")
    void shouldTolerateSpacesAroundEquals() {
      var result = PARSER.parse("text/plain; charset = utf-8");

      assertThat(result.isAcceptable()).isTrue();
      assertThat(result.first().orElseThrow().charsetName().orElseThrow()).isEqualTo("utf-8");
      assertThat(hasCode(result, "space_around_equals")).isTrue();
    }

    @Test
    void shouldTolerateTrailingSemicolon() {
      assertThat(PARSER.parse("text/plain; charset=utf-8;").isAcceptable()).isTrue();
    }

    @Test
    void shouldTreatUnknownParameterAsOpaque() {
      assertThat(
              parseMediaType("text/plain; futureparam=somevalue").parameters().get("futureparam"))
          .isEqualTo("somevalue");
    }

    @Test
    void shouldRejectUnknownParametersInStrictMode() {
      var parser = MediaTypeParser.builder().rejectUnknownParameters(true).build();

      assertThat(rejected(parser.parse("text/plain; futureparam=x"), "unknown_parameter")).isTrue();
    }

    @Nested
    @DisplayName("pluggable handler validation")
    class PluggableHandler {

      private static final MediaTypeParser VERSION_PARSER =
          MediaTypeParser.builder()
              .parameterHandler(
                  new ParameterHandler() {
                    @Override
                    public String name() {
                      return "version";
                    }

                    @Override
                    public void validate(String value, ParameterHandler.Context context) {
                      if (!value.matches("\\d+(\\.\\d+)*")) {
                        context.reject("invalid_version", "must be dotted digits");
                      }
                    }
                  })
              .build();

      @Test
      void shouldAcceptValidVersionParameter() {
        assertThat(VERSION_PARSER.parse("application/widget; version=2.1").isAcceptable()).isTrue();
      }

      @Test
      void shouldRejectInvalidVersionParameter() {
        var result = VERSION_PARSER.parse("application/widget; version=banana");

        assertThat(rejected(result, "invalid_version")).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("RFC 9110 §12.4.2 — qvalue")
  class QValue {

    @ParameterizedTest(name = "valid q={0} -> quality {1}, no warning")
    @CsvSource({"0, 0.0", "1, 1.0", "0.5, 0.5", "0.333, 0.333", "1.000, 1.0", "0.0, 0.0"})
    void shouldAcceptValidQValues(String qValue, double expected) {
      var result = PARSER.parseList(acceptWithQuality(qValue));

      assertThat(result.first().orElseThrow().quality()).isEqualTo(expected);
      assertThat(hasCode(result, "invalid_qvalue")).isFalse();
    }

    @ParameterizedTest(name = "invalid q={0} -> warned, clamped to {1}")
    @CsvSource({"1.5, 1.0", "2, 1.0", "-1, 0.0", "0.5555, 0.5555", "abc, 0.0"})
    void shouldWarnAndClampInvalidQValues(String qValue, double expectedClamped) {
      var result = PARSER.parseList(acceptWithQuality(qValue));

      assertThat(hasCode(result, "invalid_qvalue")).isTrue();
      assertThat(result.first().orElseThrow().quality()).isEqualTo(expectedClamped);
    }

    @Test
    void shouldWarnAboutQInContentType() {
      var result = PARSER.parse(acceptWithQuality("0.5"));

      assertThat(hasCode(result, "q_in_content_type")).isTrue();
    }

    private static String acceptWithQuality(String qValue) {
      return "text/html;q=" + qValue;
    }
  }

  @Nested
  @DisplayName("Content-Type vs Accept semantics")
  class ContentTypeVsAccept {

    @ParameterizedTest(name = "wildcard {0} rejected as a Content-Type")
    @ValueSource(strings = {"*/*", "text/*"})
    void shouldRejectWildcardInContentType(String input) {
      assertThat(rejected(PARSER.parse(input), "wildcard_in_content_type")).isTrue();
    }

    @Test
    void shouldAllowWildcardsInAccept() {
      assertThat(PARSER.parseList("text/*, */*;q=0.5").isAcceptable()).isTrue();
    }

    @Test
    void shouldRejectListAsContentType() {
      assertThat(rejected(PARSER.parse("text/html, application/json"), "unexpected_list")).isTrue();
    }
  }

  @Nested
  @DisplayName("RFC 6906 / application/ld+json — profile parameter parsing")
  class ProfileParsing {

    @Test
    void shouldParseQuotedMultiUri() {
      var mediaType =
          parseMediaType(
              "application/ld+json; profile=\"https://w3.org/ns/a https://w3.org/ns/b\"");

      assertThat(mediaType.profiles()).hasSize(2);
    }

    @Test
    void shouldTolerateUnquotedSingleProfileByDefault() {
      var result = PARSER.parse("application/ld+json; profile=https://w3.org/ns/expanded");

      assertThat(result.isAcceptable()).isTrue();
      assertThat(hasCode(result, "unquoted_profile")).isTrue();
      assertThat(result.first().orElseThrow().profiles()).hasSize(1);
    }

    @Test
    void shouldRejectUnquotedProfileWithoutFlag() {
      var parser = MediaTypeParser.builder().build();
      var result = parser.parse("application/ld+json; profile=https://w3.org/ns/expanded");

      assertThat(rejected(result, "invalid_token_value")).isTrue();
    }

    @Test
    void shouldRejectRelativeProfileUri() {
      var result = PARSER.parse("application/ld+json; profile=\"/relative\"");

      assertThat(rejected(result, "relative_profile_uri")).isTrue();
    }
  }

  @Nested
  @DisplayName("RFC 9110 §12.5.1 — negotiation matching")
  class NegotiationMatching {

    private static final String RFC_ACCEPT =
        "text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5";

    @ParameterizedTest(name = "{0} resolves to q={1}")
    @CsvSource({
      "text/html;level=1, 1.0",
      "text/html,         0.7",
      "text/plain,        0.3",
      "image/jpeg,        0.5",
      "text/html;level=2, 0.4",
      "text/html;level=3, 0.7"
    })
    void shouldSelectMostSpecificMatch(String representation, double expectedQuality) {
      var accept = PARSER.parseList(RFC_ACCEPT);

      assertThat(effectiveQuality(accept, parseMediaType(representation)))
          .isEqualTo(expectedQuality);
    }

    @Test
    void shouldPerformParameterAwareMatching() {
      var accept = PARSER.parseList("text/html;level=1");

      assertThat(NEGOTIATOR.best(accept, List.of(new MediaType("text", "html")))).isEmpty();
    }

    @Test
    void shouldIgnoreCharsetByDefaultInNegotiation() {
      var accept = PARSER.parseList("text/html;charset=utf-8");
      var bareHtml = new MediaType("text", "html");

      assertThat(NEGOTIATOR.best(accept, List.of(bareHtml))).isPresent();
    }

    @Test
    void shouldHonourCharsetAsHardConstraintWhenStrict() {
      var accept = PARSER.parseList("text/html;charset=utf-8");
      var bareHtml = new MediaType("text", "html");

      assertThat(ContentNegotiator.strict().best(accept, List.of(bareHtml))).isEmpty();
    }

    @Test
    void shouldAcceptAnythingWhenAcceptHeaderEmpty() {
      var any = PARSER.parseList("");

      var best =
          NEGOTIATOR.best(
              any, List.of(new MediaType("text", "html"), new MediaType("application", "json")));

      assertThat(best.orElseThrow().essence()).isEqualTo("text/html");
    }

    @Test
    void shouldTreatQZeroAsNotAcceptable() {
      var accept = PARSER.parseList("application/json;q=0, text/plain");

      assertThat(NEGOTIATOR.best(accept, List.of(new MediaType("application", "json")))).isEmpty();
    }
  }

  @Nested
  @DisplayName("Negotiation ranking order")
  class Ranking {

    @Test
    void shouldSelectHigherQualityRepresentation() {
      var accept = PARSER.parseList("application/xml;q=0.9, */*;q=0.8");
      var offered =
          List.of(new MediaType("application", "json"), new MediaType("application", "xml"));

      assertThat(NEGOTIATOR.best(accept, offered).orElseThrow().essence())
          .isEqualTo("application/xml");
    }

    @Test
    void shouldPreferServerOrderOnTie() {
      var accept = PARSER.parseList("*/*");
      var offered = List.of(new MediaType("text", "html"), new MediaType("application", "json"));

      assertThat(NEGOTIATOR.best(accept, offered).orElseThrow().essence()).isEqualTo("text/html");
    }
  }

  @Nested
  @DisplayName("RFC 6906 — profile as an unordered-set preference")
  class ProfilePreference {

    private final MediaType plainLd = new MediaType("application", "ld+json");
    private final MediaType schemaLd =
        new MediaType("application", "ld+json", Map.of("profile", "https://schema.org"));
    private final MediaType expandedLd =
        new MediaType(
            "application", "ld+json", Map.of("profile", "http://www.w3.org/ns/json-ld#expanded"));
    private final MediaType compactedLd =
        new MediaType(
            "application", "ld+json", Map.of("profile", "http://www.w3.org/ns/json-ld#compacted"));

    @Test
    void shouldHonourRecognizedProfileAndIgnoreRest() {
      var accept =
          PARSER.parseList(
              "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted"
                  + " https://schema.org\"");

      assertThat(NEGOTIATOR.best(accept, List.of(plainLd, schemaLd)).orElseThrow())
          .isEqualTo(schemaLd);
    }

    @Test
    @DisplayName("profile never causes a 406")
    void shouldNeverGateOnProfile() {
      var accept = PARSER.parseList("application/ld+json; profile=\"https://example.com/unknown\"");

      assertThat(NEGOTIATOR.best(accept, List.of(plainLd))).isPresent();
    }

    @ParameterizedTest(name = "order/duplicates insignificant: [{0}] -> score 2")
    @ValueSource(
        strings = {
          "https://schema.org http://www.w3.org/ns/json-ld#compacted",
          "http://www.w3.org/ns/json-ld#compacted https://schema.org",
          "https://schema.org http://www.w3.org/ns/json-ld#compacted https://schema.org"
        })
    void shouldTreatProfileOrderAndDuplicatesAsInsignificant(String profileValue) {
      var both =
          new MediaType(
              "application",
              "ld+json",
              Map.of("profile", "http://www.w3.org/ns/json-ld#compacted https://schema.org"));
      var accept = PARSER.parseList("application/ld+json; profile=\"" + profileValue + "\"");

      assertThat(NEGOTIATOR.rank(accept, List.of(both)).getFirst().profileMatchScore())
          .isEqualTo(2);
    }

    @Test
    void shouldResolveContradictoryProfilesToRecognized() {
      var accept =
          PARSER.parseList(
              "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted"
                  + " http://www.w3.org/ns/json-ld#expanded\"");

      var pick = NEGOTIATOR.best(accept, List.of(expandedLd, compactedLd));

      assertThat(pick.orElseThrow()).isEqualTo(expandedLd);
    }

    @Test
    void shouldReportHonouredSubset() {
      var accept =
          PARSER.parseList(
              "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted"
                  + " https://schema.org\"");

      var honouredProfiles =
          NEGOTIATOR.rank(accept, List.of(schemaLd)).getFirst().honoredProfiles();

      assertThat(honouredProfiles).isEqualTo(List.of(URI.create("https://schema.org")));
    }
  }

  @Nested
  @DisplayName("MediaType value semantics")
  class ValueSemantics {

    @Test
    void shouldBeEqualRegardlessOfCase() {
      assertThat(parseMediaType("text/html")).isEqualTo(parseMediaType("Text/HTML"));
    }

    @ParameterizedTest(name = "round-trips: {0}")
    @MethodSource("roundTripCases")
    void shouldRoundTripThroughToString(String input) {
      var parsed = parseMediaType(input);

      assertThat(parseMediaType(parsed.toString())).isEqualTo(parsed);
    }

    static Stream<Arguments> roundTripCases() {
      return Stream.of(
          Arguments.of("text/html"),
          Arguments.of("text/html; charset=utf-8"),
          Arguments.of("text/plain; note=\"a, b; c\""),
          Arguments.of("application/vnd.api+json; profile=\"https://schema.org\""));
    }
  }
}
