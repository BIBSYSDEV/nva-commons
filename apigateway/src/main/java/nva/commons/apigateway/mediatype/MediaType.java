package nva.commons.apigateway.mediatype;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable, already-validated media type such as {@code text/html; charset=utf-8}.
 *
 * <p>This is a pure value type: it does no parsing and no validation. All parsing, normalization
 * and security checking lives in {@link MediaTypeParser}. By the time a {@code MediaType} exists,
 * its parameter values are unquoted/unescaped and its names are lower-cased.
 *
 * <p>Type, subtype and parameter <em>names</em> are lower-cased so that {@code Text/HTML} equals
 * {@code text/html} (RFC 9110 / RFC 6838 case-insensitivity). Parameter <em>values</em> keep their
 * original case.
 */
public record MediaType(String type, String subtype, Map<String, String> parameters) {

  public static final String WILDCARD = "*";
  private static final char SUFFIX_SEPARATOR = '+';
  private static final Pattern WHITESPACE_SPLITTER = Pattern.compile("\\s+");
  private static final char TYPE_SEPARATOR = '/';
  private static final String PARAMETER_SEPARATOR = "; ";
  private static final char PARAMETER_ASSIGN = '=';
  private static final char DQUOTE = '"';
  private static final char BACKSLASH = '\\';
  private static final String CHARSET = "charset";
  private static final String QUALITY = "q";
  private static final String PROFILE = "profile";
  private static final double DEFAULT_QUALITY = 1.0;
  private static final double MINIMUM_QUALITY = 0.0;

  /** Canonical constructor: normalizes names and defensively copies the map. */
  public MediaType {
    type = normalizeName(type);
    subtype = normalizeName(subtype);
    parameters = normalizeParameters(parameters);
  }

  public MediaType(String type, String subtype) {
    this(type, subtype, Collections.emptyMap());
  }

  /** {@code "type/subtype"} with no parameters — the value browsers compare on. */
  public String essence() {
    return type + TYPE_SEPARATOR + subtype;
  }

  /**
   * The structured syntax suffix without the {@code +}, e.g. {@code json} for {@code
   * application/foo+json}.
   */
  public Optional<String> structuredSyntaxSuffix() {
    int plus = subtype.lastIndexOf(SUFFIX_SEPARATOR);
    return plus < 0 ? Optional.empty() : Optional.of(subtype.substring(plus + 1));
  }

  /**
   * Raw {@code charset} parameter value, if present (not resolved to a {@link
   * java.nio.charset.Charset}).
   */
  public Optional<String> charsetName() {
    return Optional.ofNullable(parameters.get(CHARSET));
  }

  /**
   * The {@code q} weight (RFC 9110 §12.4.2), clamped to the valid range [0,1]. Absent or
   * unparseable weights default to {@code 1.0}; the parser separately flags malformed weights.
   */
  public double quality() {
    return Optional.ofNullable(parameters.get(QUALITY))
        .map(MediaType::parseQuality)
        .orElse(DEFAULT_QUALITY);
  }

  private static double parseQuality(String qualityValue) {
    try {
      return clampedQuality(Double.parseDouble(qualityValue));
    } catch (NumberFormatException ignored) {
      return MINIMUM_QUALITY;
    }
  }

  private static double clampedQuality(double value) {
    return (Double.isNaN(value) || Double.isInfinite(value))
        ? MINIMUM_QUALITY
        : Math.clamp(value, MINIMUM_QUALITY, DEFAULT_QUALITY);
  }

  /**
   * The {@code profile} parameter as a list of URIs (RFC 6906): a space-separated list. Best-effort
   * — unparseable tokens are skipped. The parser performs strict validation.
   */
  public List<URI> profiles() {
    return Optional.ofNullable(parameters.get(PROFILE)).stream()
        .flatMap(raw -> WHITESPACE_SPLITTER.splitAsStream(raw.strip()))
        .filter(Predicate.not(String::isEmpty))
        .flatMap(token -> parseUri(token).stream())
        .distinct()
        .toList();
  }

  private static Optional<URI> parseUri(String token) {
    try {
      return Optional.of(URI.create(token));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  public boolean isWildcardType() {
    return WILDCARD.equals(type);
  }

  public boolean isWildcardSubtype() {
    return WILDCARD.equals(subtype);
  }

  /**
   * Type/subtype specificity score used in RFC 9110 §12.5.1 range matching: 2 = fully specified, 1
   * = {@code type/*}, 0 = {@code *}{@code /*}. Does not account for parameters.
   */
  public int typeSpecificity() {
    if (isWildcardType()) {
      return 0;
    }
    return isWildcardSubtype() ? 1 : 2;
  }

  /**
   * True if this concrete type is matched by {@code range}, honouring {@code *} wildcards in the
   * range.
   */
  public boolean matchedBy(MediaType range) {
    boolean typeOk = range.isWildcardType() || range.type.equals(type);
    boolean subtypeOk = range.isWildcardSubtype() || range.subtype.equals(subtype);
    return typeOk && subtypeOk;
  }

  private static String normalizeName(String value) {
    return Objects.isNull(value) || value.isBlank()
        ? WILDCARD
        : value.strip().toLowerCase(Locale.ROOT);
  }

  private static Map<String, String> normalizeParameters(Map<String, String> source) {
    return Objects.isNull(source)
        ? Collections.emptyMap()
        : source.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    entry -> entry.getKey().toLowerCase(Locale.ROOT), Map.Entry::getValue));
  }

  @Override
  public String toString() {
    return Stream.concat(
            Stream.of(type + TYPE_SEPARATOR + subtype),
            parameters.entrySet().stream()
                .map(entry -> entry.getKey() + PARAMETER_ASSIGN + maybeQuote(entry.getValue())))
        .collect(Collectors.joining(PARAMETER_SEPARATOR));
  }

  private static String maybeQuote(String value) {
    return needsQuoting(value) ? quote(value) : value;
  }

  private static boolean needsQuoting(String value) {
    return value.isEmpty()
        || value.chars().anyMatch(character -> !MediaTypeLexer.isTokenChar((char) character));
  }

  private static String quote(String value) {
    return DQUOTE
        + value
            .chars()
            .mapToObj(character -> escape((char) character))
            .collect(Collectors.joining())
        + DQUOTE;
  }

  private static String escape(char character) {
    return character == DQUOTE || character == BACKSLASH
        ? String.valueOf(BACKSLASH) + character
        : String.valueOf(character);
  }
}
