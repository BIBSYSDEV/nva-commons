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
import nva.commons.apigateway.mediatype.MediaTypeParseResult.Severity;
import nva.commons.apigateway.mediatype.MediaTypeParseResult.Violation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * API/behaviour suite, organised by public type. This is the JUnit replacement for the hand-rolled
 * MediaTypeParserDemo harness; it leans on parameterised tests so that each behaviour is expressed
 * as a data table rather than a wall of one-off assertions.
 */
@DisplayName("Media type stack — API behaviour")
class MediaTypeApiTest {

  private static final MediaTypeParser PARSER = MediaTypeParser.defaultParser();
  private static final ContentNegotiator NEGOTIATOR = ContentNegotiator.defaultNegotiator();

  // ============================================================ MediaType
  @Nested
  @DisplayName("MediaType — value semantics")
  class MediaTypeValue {

    @ParameterizedTest(name = "{0}/{1} -> essence {2}")
    @CsvSource({
      "Text,        HTML, text/html",
      "APPLICATION, JSON, application/json",
      "text,        ,     text/*",
      ",            ,     */*"
    })
    void shouldNormalizeEssence(String type, String subtype, String expected) {
      assertThat(new MediaType(type, subtype).essence()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "blank type normalizes to wildcard")
    @MethodSource("blankTypeNormalizationCases")
    void shouldNormalizeBlankTypeToWildcard(MediaType mediaType, String expected) {
      assertThat(mediaType.essence()).isEqualTo(expected);
    }

    static Stream<Arguments> blankTypeNormalizationCases() {
      return Stream.of(
          Arguments.of(new MediaType("  ", "html"), "*/html"),
          Arguments.of(new MediaType("text", "  "), "text/*"));
    }

    @ParameterizedTest(name = "null parameters normalizes to empty map")
    @MethodSource("nullParametersCases")
    void shouldNormalizeNullParametersToEmptyMap(MediaType mediaType) {
      assertThat(mediaType.parameters()).isEmpty();
    }

    static Stream<Arguments> nullParametersCases() {
      return Stream.of(Arguments.of(new MediaType("text", "html", null)));
    }

    @ParameterizedTest(name = "subtype {0} -> suffix {1}")
    @CsvSource({
      "vnd.api+json, json",
      "svg+xml,      xml",
      "senml+cbor,   cbor",
      "a+b+baz,      baz",
      "cbor,         NONE"
    })
    void shouldExtractStructuredSyntaxSuffix(String subtype, String expected) {
      assertThat(new MediaType("application", subtype).structuredSyntaxSuffix().orElse("NONE"))
          .isEqualTo(expected);
    }

    @ParameterizedTest(name = "q={0} -> quality {1}")
    @CsvSource({
      "1, 1.0",
      "0.5, 0.5",
      "0.333, 0.333",
      "1.5, 1.0",
      "-1, 0.0",
      "abc, 0.0",
      "NaN, 0.0",
      "Infinity, 0.0",
      "+Infinity, 0.0"
    })
    void shouldClampQualityToValidRange(String qValue, double expected) {
      assertThat(new MediaType("a", "b", Map.of("q", qValue)).quality()).isEqualTo(expected);
    }

    @Test
    void shouldDefaultAbsentQualityToOne() {
      assertThat(new MediaType("a", "b").quality()).isEqualTo(1.0);
    }

    @ParameterizedTest(name = "text/html matchedBy {0}/{1} == {2}")
    @CsvSource({
      "text,        *,    true",
      "*,           *,    true",
      "text,        html, true",
      "application, *,    false",
      "text,        plain,false"
    })
    void shouldHonourWildcardsInMatchedBy(String rangeType, String rangeSub, boolean expected) {
      assertThat(new MediaType("text", "html").matchedBy(new MediaType(rangeType, rangeSub)))
          .isEqualTo(expected);
    }

    @ParameterizedTest(name = "profiles case: {1} URI(s)")
    @MethodSource("profilesCases")
    void shouldParseProfilesAsUriList(MediaType mediaType, int expectedCount) {
      assertThat(mediaType.profiles()).hasSize(expectedCount);
    }

    static Stream<Arguments> profilesCases() {
      return Stream.of(
          Arguments.of(new MediaType("a", "b", Map.of("profile", "https://a https://b")), 2),
          Arguments.of(new MediaType("a", "b", Map.of("profile", "https://a")), 1),
          Arguments.of(new MediaType("a", "b", Map.of("profile", "")), 0),
          Arguments.of(new MediaType("a", "b", Map.of("profile", "[invalid")), 0));
    }

    @Test
    void shouldBeEqualRegardlessOfCase() {
      assertThat(new MediaType("Text", "HTML")).isEqualTo(new MediaType("text", "html"));
    }

    @ParameterizedTest(name = "round-trips: {0}")
    @MethodSource("roundTripValues")
    void shouldRoundTripThroughToString(MediaType value) {
      assertThat(PARSER.parse(value.toString()).first().orElseThrow()).isEqualTo(value);
    }

    static Stream<Arguments> roundTripValues() {
      return Stream.of(
          Arguments.of(new MediaType("text", "html")),
          Arguments.of(new MediaType("text", "html", Map.of("charset", "utf-8"))),
          Arguments.of(new MediaType("text", "plain", Map.of("note", "a, b; c"))),
          Arguments.of(
              new MediaType("application", "ld+json", Map.of("profile", "https://schema.org"))));
    }

    @Test
    @DisplayName("backslash in parameter value is escaped in toString")
    void shouldEscapeBackslashInParameterValue() {
      var mediaType = new MediaType("text", "plain", Map.of("note", "a\\b"));

      String serialised = mediaType.toString();

      assertThat(serialised).contains("\\\\");
      assertThat(serialised).isEqualTo("text/plain; note=\"a\\\\b\"");
    }

    @Test
    @DisplayName("double-quote in parameter value is escaped in toString")
    void shouldEscapeDoubleQuoteInParameterValue() {
      var mediaType = new MediaType("text", "plain", Map.of("note", "a\"b"));

      String serialised = mediaType.toString();

      assertThat(serialised).contains("\\\"");
    }
  }

  // ============================================================ Parser
  @Nested
  @DisplayName("MediaTypeParser — parsing & rejection")
  class Parsing {

    @ParameterizedTest(name = "[{2}] rejected for: {0}")
    @MethodSource("rejectionCases")
    void shouldProduceViolationForMalformedInput(String input, boolean list, String code) {
      var result = list ? PARSER.parseList(input) : PARSER.parse(input);

      assertThat(rejected(result, code))
          .as(() -> "expected " + code + " for '" + input + "', got " + result.violations())
          .isTrue();
    }

    static Stream<Arguments> rejectionCases() {
      return Stream.of(
          Arguments.of("text/html\r\nSet-Cookie: x", false, "control_character"),
          Arguments.of("text/html\u0000", false, "control_character"),
          Arguments.of("text/" + "a".repeat(9000), false, "input_too_long"),
          Arguments.of("x/y,".repeat(70), true, "too_many_elements"),
          Arguments.of("text", false, "missing_subtype"),
          Arguments.of("text/*", false, "wildcard_in_content_type"),
          Arguments.of("text/html, application/json", false, "unexpected_list"),
          Arguments.of(
              "text/html; charset=utf-8; charset=iso-8859-1", false, "duplicate_parameter"),
          Arguments.of("text/plain; note=\"unterminated", false, "malformed_quoted_string"),
          Arguments.of("text/plain; charset=definitely-not-a-charset", false, "unknown_charset"),
          Arguments.of("application/ld+json; profile=\"/relative\"", false, "relative_profile_uri"),
          Arguments.of("text/" + "a".repeat(128), false, "name_too_long"),
          Arguments.of("application/ld+json; profile=\"[\"", false, "invalid_profile_uri"),
          Arguments.of("text/plain; key", false, "invalid_token_value"),
          Arguments.of("text/plain; note=\"\"", false, "obs_text"),
          Arguments.of("text/html\u0001", false, "control_character"),
          Arguments.of(null, false, "null_input"),
          Arguments.of(null, true, "null_input"),
          Arguments.of("/html", false, "empty_type"),
          Arguments.of("text/", false, "empty_subtype"),
          Arguments.of("text/html\u007f", false, "control_character"),
          Arguments.of("text/plain; @key=value", false, "invalid_parameter_name"));
    }

    @Test
    @DisplayName("blank elements in Accept list are silently skipped")
    void shouldSkipBlankElementsInAcceptList() {
      assertThat(PARSER.parseList("text/html, , application/json").mediaTypes()).hasSize(2);
    }

    @Test
    @DisplayName("strict parser rejects whitespace around '='")
    void shouldRejectSpaceAroundEqualsInStrictMode() {
      var strict = MediaTypeParser.builder().build();

      assertThat(rejected(strict.parse("text/plain; charset = utf-8"), "space_around_equals"))
          .isTrue();
    }

    @ParameterizedTest(name = "tolerated (acceptable): {0}")
    @ValueSource(
        strings = {
          "text/plain; charset=utf-8;",
          "text/plain; charset = utf-8",
          "text/plain;",
          "application/ld+json; profile=https://schema.org/x",
          "text/plain; futureparam=opaque-value"
        })
    void shouldAcceptLenientInputs(String input) {
      assertThat(PARSER.parse(input).isAcceptable())
          .as(() -> PARSER.parse(input).violations().toString())
          .isTrue();
    }

    @ParameterizedTest(name = "{0} -> essence {1}, charset {2}")
    @CsvSource({
      "Text/HTML; charset=UTF-8, text/html, UTF-8",
      "APPLICATION/Json,         application/json, NONE",
      "text/plain; CharSet=ascii, text/plain, ascii"
    })
    void shouldNormalizeTypeAndCharset(String input, String essence, String charset) {
      var mediaType = parseMediaType(input);

      assertThat(mediaType.essence()).isEqualTo(essence);
      assertThat(mediaType.charsetName().orElse("NONE")).isEqualTo(charset);
    }

    @Test
    @DisplayName("quoted-pair escaping and embedded separators survive")
    void shouldHandleQuotedValues() {
      assertThat(parseMediaType("text/plain; note=\"a\\\"b\"").parameters().get("note"))
          .isEqualTo("a\"b");
      assertThat(parseMediaType("text/plain; note=\"a, b; c\"").parameters().get("note"))
          .isEqualTo("a, b; c");
    }

    @Test
    void shouldReturnAllEntriesFromParseList() {
      assertThat(
              PARSER
                  .parseList("text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8")
                  .mediaTypes())
          .hasSize(4);
    }

    @ParameterizedTest(name = "[{2}] parser-specific rejection: {1}")
    @MethodSource("parserSpecificRejectionCases")
    void shouldProduceViolationForParserSpecificRejection(
        MediaTypeParser parser, String input, String code) {
      assertThat(rejected(parser.parse(input), code)).isTrue();
    }

    static Stream<Arguments> parserSpecificRejectionCases() {
      return Stream.of(
          Arguments.of(
              MediaTypeParser.builder().build(),
              "text/plain; charset= utf-8",
              "space_around_equals"),
          Arguments.of(
              MediaTypeParser.builder().maxParametersPerType(1).build(),
              "text/plain; a=1; b=2",
              "too_many_parameters"),
          Arguments.of(
              MediaTypeParser.builder().allowedProfiles(URI.create("https://ok.example")).build(),
              "application/ld+json; profile=\"https://other.example\"",
              "profile_not_allowed"));
    }

    @ParameterizedTest(name = "accepted with config: {1}")
    @MethodSource("parserSpecificAcceptanceCases")
    void shouldAcceptParserSpecificInput(MediaTypeParser parser, String input) {
      assertThat(parser.parse(input).isAcceptable()).isTrue();
    }

    static Stream<Arguments> parserSpecificAcceptanceCases() {
      return Stream.of(
          Arguments.of(
              MediaTypeParser.builder().allowObsText(true).build(), "text/plain; note=\"\""),
          Arguments.of(
              MediaTypeParser.builder().allowedProfiles(URI.create("https://ok.example")).build(),
              "application/ld+json; profile=\"https://ok.example\""));
    }

    @Test
    @DisplayName("tab character in input is allowed (not a control character)")
    void shouldAllowTabCharacter() {
      var result = PARSER.parse("text/html;\tcharset=utf-8");

      assertThat(result.isAcceptable())
          .as(() -> "Expected tab to be allowed: " + result.violations())
          .isTrue();
    }

    @Test
    @DisplayName("empty parameter is rejected when acceptEmptyParameters is false")
    void shouldRejectEmptyParameterWhenNotConfigured() {
      var strict = MediaTypeParser.builder().build();

      assertThat(rejected(strict.parse("text/plain;"), "empty_parameter")).isTrue();
    }

    @Test
    @DisplayName("lenient parser normalizes whitespace on value side only")
    void shouldNormalizeValueSideSpaceInLenientMode() {
      var lenient = MediaTypeParser.builder().lenientWhitespaceAroundEquals().build();

      var result = lenient.parse("text/plain; charset= utf-8");

      assertThat(result.isAcceptable()).isTrue();
      assertThat(hasCode(result, "space_around_equals")).isTrue();
    }
  }

  // ============================================================ Builder
  @Nested
  @DisplayName("MediaTypeParser.Builder — configuration")
  class BuilderOptions {

    private final MediaTypeParser restrictedCharset =
        MediaTypeParser.builder()
            .allowedCharsets(StandardCharsets.UTF_8, StandardCharsets.US_ASCII)
            .build();

    @ParameterizedTest(name = "charset {0} acceptable == {1}")
    @CsvSource({"utf-8, true", "us-ascii, true", "iso-8859-1, false", "utf-16, false"})
    void shouldEnforceCharsetAllowList(String charset, boolean acceptable) {
      assertThat(restrictedCharset.parse("text/plain; charset=" + charset).isAcceptable())
          .isEqualTo(acceptable);
    }

    @Test
    void shouldRejectTcharWhenRestrictedNamesEnforced() {
      var parser = MediaTypeParser.builder().enforceRestrictedNames(true).build();

      assertThat(rejected(parser.parse("text/foo~bar"), "invalid_subtype")).isTrue();
    }

    @Test
    void shouldEnforceMaxProfileUrisLimit() {
      var parser =
          MediaTypeParser.builder().acceptSingleUnquotedProfileString().maxProfileUris(2).build();

      assertThat(
              rejected(
                  parser.parse("application/ld+json; profile=\"https://a https://b https://c\""),
                  "too_many_profiles"))
          .isTrue();
    }

    @ParameterizedTest(name = "unquoted profile acceptable == {1}")
    @MethodSource("unquotedProfileParsers")
    void shouldRespectUnquotedProfileStringFlag(MediaTypeParser parser, boolean acceptable) {
      assertThat(
              parser
                  .parse("application/ld+json; profile=https://w3.org/ns/expanded")
                  .isAcceptable())
          .isEqualTo(acceptable);
    }

    static Stream<Arguments> unquotedProfileParsers() {
      return Stream.of(
          Arguments.of(MediaTypeParser.defaultParser(), true),
          Arguments.of(MediaTypeParser.builder().build(), false));
    }

    @Test
    void shouldEnforceAllowedTypes() {
      var parser = MediaTypeParser.builder().allowedTypes(new MediaType("text", "html")).build();

      assertThat(parser.parse("text/html").isAcceptable()).isTrue();
      assertThat(rejected(parser.parse("application/json"), "type_not_allowed")).isTrue();
    }

    @Test
    void shouldEnforceAllowedProfiles() {
      var parser =
          MediaTypeParser.builder().allowedProfiles(URI.create("https://allowed.example")).build();

      assertThat(
              rejected(
                  parser.parse("application/ld+json; profile=\"https://other.example\""),
                  "profile_not_allowed"))
          .isTrue();
    }

    @Test
    void shouldAcceptAnyCharsetWithoutBuiltinHandlers() {
      var parser = MediaTypeParser.builder().withoutBuiltinHandlers().build();

      assertThat(parser.parse("text/plain; charset=definitely-not-a-charset").isAcceptable())
          .isTrue();
    }

    @Test
    void shouldNormalizeDuplicateParametersWhenRejectionDisabled() {
      var parser = MediaTypeParser.builder().rejectDuplicateParameters(false).build();

      var result = parser.parse("text/html; charset=utf-8; charset=iso-8859-1");

      assertThat(result.isAcceptable()).isTrue();
      assertThat(hasCode(result, "duplicate_parameter")).isTrue();
    }
  }

  // ============================================================ Handlers
  @Nested
  @DisplayName("ParameterHandler — pluggable parameter semantics")
  class Handlers {

    private final MediaTypeParser versioned =
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
                      context.reject("invalid_version", "dotted digits required");
                    }
                  }
                })
            .build();

    @ParameterizedTest(name = "version={0} acceptable == {1}")
    @CsvSource({"2.1, true", "10.0.3, true", "banana, false", "1.x, false"})
    void shouldValidateValueWithCustomHandler(String version, boolean acceptable) {
      assertThat(versioned.parse("application/widget; version=" + version).isAcceptable())
          .isEqualTo(acceptable);
    }

    @Test
    @DisplayName("a custom handler overrides the built-in of the same name")
    void shouldOverrideBuiltinHandlerWithCustomHandler() {
      var utf8Only =
          MediaTypeParser.builder()
              .parameterHandler(
                  new ParameterHandler() {
                    @Override
                    public String name() {
                      return "charset";
                    }

                    @Override
                    public void validate(String value, ParameterHandler.Context context) {
                      if (!value.equalsIgnoreCase("utf-8")) {
                        context.reject("charset_locked", "utf-8 only");
                      }
                    }
                  })
              .build();

      assertThat(utf8Only.parse("text/plain; charset=utf-8").isAcceptable()).isTrue();
      assertThat(rejected(utf8Only.parse("text/plain; charset=utf-16"), "charset_locked")).isTrue();
    }

    @Test
    void shouldTreatUnknownParameterAsOpaqueByDefault() {
      assertThat(parseMediaType("text/plain; futureparam=kept").parameters().get("futureparam"))
          .isEqualTo("kept");
    }

    @Test
    void shouldRejectUnknownParameterInStrictMode() {
      var parser = MediaTypeParser.builder().rejectUnknownParameters(true).build();

      assertThat(rejected(parser.parse("text/plain; futureparam=x"), "unknown_parameter")).isTrue();
    }
  }

  // ============================================================ Result
  @Nested
  @DisplayName("MediaTypeParseResult — outcome surface")
  class Results {

    @ParameterizedTest(name = "{0} -> valid={1}, acceptable={2}")
    @CsvSource({
      "text/html,                  true,  true",
      "text/*,                     false, false",
      "text/html; charset=nope-xx, false, false",
      "text/html; charset = utf-8, true,  true"
    })
    void shouldReportCorrectValidityFlags(String input, boolean valid, boolean acceptable) {
      var result = PARSER.parse(input);

      assertThat(result.isValid()).isEqualTo(valid);
      assertThat(result.isAcceptable()).isEqualTo(acceptable);
    }

    @Test
    void shouldHaveNoMediaTypesWhenRejected() {
      assertThat(PARSER.parse("text/*").first()).isEmpty();
    }

    @ParameterizedTest(name = "Violation.toString starts with {0}")
    @EnumSource(Severity.class)
    void shouldLeadViolationToStringWithSeverity(Severity severity) {
      assertThat(new Violation(severity, "some_code", "detail").toString())
          .startsWith(severity.name());
    }

    @Test
    void shouldNotBeAcceptableWhenResultHasNoMediaTypes() {
      var result = PARSER.parse("  ");

      assertThat(result.isValid()).isTrue();
      assertThat(result.isAcceptable()).isFalse();
    }

    @Test
    void shouldCollectProfileUrisFromAllMediaTypes() {
      var result =
          PARSER.parseList("application/ld+json; profile=\"https://schema.org\", text/html");

      assertThat(result.profiles()).hasSize(1);
    }

    @Test
    void shouldSortPreferenceOrderByQualityThenSpecificity() {
      var result = PARSER.parseList("*/*;q=0.1, text/*;q=0.5, text/html");
      var order = result.preferenceOrder();

      assertThat(order.get(0).essence()).isEqualTo("text/html");
      assertThat(order.get(1).essence()).isEqualTo("text/*");
      assertThat(order.get(2).essence()).isEqualTo("*/*");
    }

    @Test
    @DisplayName("text/* has specificity 1 (between */* and text/html)")
    void shouldAssignSpecificityOneToWildcardSubtype() {
      var result = PARSER.parseList("*/*;q=0.5, text/*;q=0.5, text/html;q=0.5");
      var order = result.preferenceOrder();

      assertThat(order.get(1).essence()).isEqualTo("text/*");
    }
  }

  // ============================================================ Negotiator
  @Nested
  @DisplayName("ContentNegotiator — negotiation & ranking")
  class Negotiation {

    private static final String RFC_ACCEPT =
        "text/*;q=0.3, text/html;q=0.7, text/html;level=1, text/html;level=2;q=0.4, */*;q=0.5";

    @ParameterizedTest(name = "{0} -> effective q={1}")
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
      var ranked = NEGOTIATOR.rank(accept, List.of(parseMediaType(representation)));

      double quality = ranked.isEmpty() ? 0.0 : ranked.getFirst().quality();

      assertThat(quality).isEqualTo(expectedQuality);
    }

    @ParameterizedTest(name = "best({0}) == {2}")
    @MethodSource("negotiationCases")
    void shouldSelectBestMatch(String accept, List<MediaType> offered, String expectedEssence) {
      String got =
          NEGOTIATOR.best(PARSER.parseList(accept), offered).map(MediaType::essence).orElse("NONE");

      assertThat(got).isEqualTo(expectedEssence);
    }

    static Stream<Arguments> negotiationCases() {
      var json = new MediaType("application", "json");
      var xml = new MediaType("application", "xml");
      var html = new MediaType("text", "html");
      return Stream.of(
          Arguments.of("application/xml;q=0.9, */*;q=0.8", List.of(json, xml), "application/xml"),
          Arguments.of("application/json;q=0, text/plain", List.of(json), "NONE"),
          Arguments.of("", List.of(html, json), "text/html"),
          Arguments.of("text/html;level=1", List.of(html), "NONE"));
    }

    @Test
    @DisplayName("charset ignored by default, a hard constraint under strict()")
    void shouldIgnoreCharsetByDefaultAndRespectUnderStrict() {
      var accept = PARSER.parseList("text/html;charset=utf-8");
      var html = new MediaType("text", "html");

      assertThat(NEGOTIATOR.best(accept, List.of(html))).isPresent();
      assertThat(ContentNegotiator.strict().best(accept, List.of(html))).isEmpty();
    }

    @ParameterizedTest(name = "profile order/dup insignificant: [{0}] -> score 2")
    @ValueSource(
        strings = {
          "https://schema.org http://www.w3.org/ns/json-ld#compacted",
          "http://www.w3.org/ns/json-ld#compacted https://schema.org",
          "https://schema.org http://www.w3.org/ns/json-ld#compacted https://schema.org"
        })
    void shouldTreatProfileAsUnorderedSetPreference(String profileValue) {
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
    @DisplayName("honor-recognized: unknown profile ignored, recognized one wins")
    void shouldHonourRecognisedProfiles() {
      var plain = new MediaType("application", "ld+json");
      var schema = new MediaType("application", "ld+json", Map.of("profile", "https://schema.org"));
      var accept =
          PARSER.parseList(
              "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted"
                  + " https://schema.org\"");

      var best = NEGOTIATOR.best(accept, List.of(plain, schema)).orElseThrow();

      assertThat(best).isEqualTo(schema);
      assertThat(NEGOTIATOR.rank(accept, List.of(schema)).getFirst().honoredProfiles())
          .isEqualTo(List.of(URI.create("https://schema.org")));
    }

    @Test
    @DisplayName("equal quality preserves the server's offered order")
    void shouldPreferServerOrderWhenQualityTied() {
      var offered = List.of(new MediaType("text", "html"), new MediaType("application", "json"));

      assertThat(NEGOTIATOR.best(PARSER.parseList("*/*"), offered).orElseThrow().essence())
          .isEqualTo("text/html");
    }

    @Test
    void shouldNotBeAcceptableWhenPreferenceQualityIsZero() {
      var preference =
          new ContentNegotiator.Preference(
              new MediaType("text", "html"), new MediaType("*", "*"), 0.0);

      assertThat(preference.isAcceptable()).isFalse();
    }

    @Test
    void shouldExposeCharsetAndProfilesFromRepresentation() {
      var rep = new MediaType("application", "ld+json", Map.of("profile", "https://schema.org"));
      var accept = PARSER.parseList("application/ld+json; profile=\"https://schema.org\"");

      var preference = NEGOTIATOR.rank(accept, List.of(rep)).getFirst();

      assertThat(preference.charsetName()).isEmpty();
      assertThat(preference.profiles()).hasSize(1);
      assertThat(preference.profiles().getFirst()).isEqualTo(URI.create("https://schema.org"));
    }

    @Test
    void shouldExposeRequestedProfilesFromMatchedRange() {
      var rep = new MediaType("application", "ld+json");
      var accept = PARSER.parseList("application/ld+json; profile=\"https://schema.org\"");

      var preference = NEGOTIATOR.rank(accept, List.of(rep)).getFirst();

      assertThat(preference.requestedProfiles()).hasSize(1);
      assertThat(preference.requestedProfiles().getFirst())
          .isEqualTo(URI.create("https://schema.org"));
    }

    @Test
    @DisplayName("ignoring() excludes the named parameter from matching")
    void shouldExcludeIgnoredParameterFromMatching() {
      var negotiator = ContentNegotiator.ignoring("level");
      var html = new MediaType("text", "html");

      assertThat(negotiator.best(PARSER.parseList("text/html;level=1"), List.of(html))).isPresent();
    }

    @Test
    @DisplayName("profileOverlap returns 0 when representation has no matching profiles")
    void shouldReturnZeroProfileOverlapWhenNoMatch() {
      var noProfile = new MediaType("application", "ld+json");
      var accept = PARSER.parseList("application/ld+json; profile=\"https://schema.org\"");

      assertThat(NEGOTIATOR.best(accept, List.of(noProfile))).isPresent();
      assertThat(NEGOTIATOR.rank(accept, List.of(noProfile)).getFirst().profileMatchScore())
          .isEqualTo(0);
    }

    @Test
    @DisplayName("profileOverlap is used as tiebreaker when specificity and quality are equal")
    void shouldUseProfileOverlapAsTiebreaker() {
      var schemaLd =
          new MediaType("application", "ld+json", Map.of("profile", "https://schema.org"));
      var accept =
          PARSER.parseList(
              "application/ld+json," + " application/ld+json; profile=\"https://schema.org\"");

      var ranked = NEGOTIATOR.rank(accept, List.of(schemaLd));

      assertThat(ranked.getFirst().profileMatchScore()).isEqualTo(1);
    }

    @Test
    @DisplayName("representation matched only by q=0 range is excluded from results")
    void shouldReturnEmptyWhenBestMatchRangeHasQualityZero() {
      var json = new MediaType("application", "json");
      var accept = PARSER.parseList("application/json;q=0");

      assertThat(NEGOTIATOR.best(accept, List.of(json))).isEmpty();
      assertThat(NEGOTIATOR.rank(accept, List.of(json))).isEmpty();
    }

    @Test
    @DisplayName("q=0 on more-specific range wins over q>0 on wildcard — RFC 9110 §12.5.1")
    void shouldExcludeRepresentationWhenMostSpecificMatchingRangeHasQualityZero() {
      var html = new MediaType("text", "html");
      var accept = PARSER.parseList("text/*;q=1.0, text/html;q=0");

      assertThat(NEGOTIATOR.best(accept, List.of(html))).isEmpty();
      assertThat(NEGOTIATOR.rank(accept, List.of(html))).isEmpty();
    }

    @Test
    @DisplayName("preference with quality > 0 is acceptable")
    void shouldBeAcceptableWhenPreferenceQualityIsPositive() {
      var accept = PARSER.parseList("text/html");
      var html = new MediaType("text", "html");

      var preference = NEGOTIATOR.rank(accept, List.of(html)).getFirst();

      assertThat(preference.isAcceptable()).isTrue();
    }
  }

  // ============================================================ Quoted-string parsing
  @Nested
  @DisplayName("Parser — quoted-string handling")
  class QuotedStringHandling {

    @Test
    @DisplayName("comma inside quoted parameter value does not split the Accept list")
    void shouldNotSplitOnCommaInsideQuotedParameterValue() {
      assertThat(PARSER.parseList("text/html; p=\"a,b\"").mediaTypes()).hasSize(1);
    }

    @ParameterizedTest(name = "malformed quoted string is rejected: {0}")
    @ValueSource(strings = {"text/html; p=\"abc\"junk", "text/html; p=\"abc", "text/html; p=\"a\\"})
    void shouldRejectMalformedQuotedString(String input) {
      assertThat(rejected(PARSER.parse(input), "malformed_quoted_string")).isTrue();
    }
  }

  // ============================================================ Restricted-name grammar (RFC 6838)
  @Nested
  @DisplayName("Parser — restricted name grammar (RFC 6838)")
  class RestrictedNameGrammar {

    private static final MediaTypeParser RESTRICTED_PARSER =
        MediaTypeParser.builder().enforceRestrictedNames(true).build();

    @ParameterizedTest(name = "type ''{0}'' violates restricted-name grammar")
    @ValueSource(strings = {"!a", "a?b"})
    void shouldRejectTypeViolatingRestrictedNameGrammar(String type) {
      assertThat(rejected(RESTRICTED_PARSER.parse(type + "/html"), "invalid_type")).isTrue();
    }

    @ParameterizedTest(name = "type ''{0}'' satisfies restricted-name grammar")
    @ValueSource(strings = {"abc", "Abc", "1bc", "a.b"})
    void shouldAcceptTypeFollowingRestrictedNameGrammar(String type) {
      assertThat(RESTRICTED_PARSER.parse(type + "/html").mediaTypes()).hasSize(1);
    }
  }
}
