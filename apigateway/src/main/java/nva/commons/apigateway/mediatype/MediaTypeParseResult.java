package nva.commons.apigateway.mediatype;

import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Outcome of a parse. Parsing never throws: it returns what could be salvaged in {@link
 * #mediaTypes()} plus a record of everything non-conformant in {@link #violations()}.
 *
 * <p>This is the "lenient-with-flags" posture. A caller making a security decision (e.g. validating
 * a {@code Content-Type}) should insist on {@link #isAcceptable()}; a caller doing best-effort
 * {@code Accept} handling can use {@link #mediaTypes()} directly.
 */
public record MediaTypeParseResult(List<MediaType> mediaTypes, List<Violation> violations) {

  private static final MediaType WILDCARD_RANGE =
      new MediaType(MediaType.WILDCARD, MediaType.WILDCARD);
  private static final int WILDCARD_TYPE_SPECIFICITY = 0;
  private static final int WILDCARD_SUBTYPE_SPECIFICITY = 1;
  private static final int FULL_TYPE_SPECIFICITY = 2;
  private static final Comparator<MediaType> MOST_PREFERRED_RANGE_FIRST =
      Comparator.comparingDouble(MediaType::quality)
          .reversed()
          .thenComparing(MediaTypeParseResult::specificity, Comparator.reverseOrder());

  public MediaTypeParseResult {
    mediaTypes = List.copyOf(mediaTypes);
    violations = List.copyOf(violations);
  }

  public enum Severity {
    /** Fatal: the offending element was discarded (or the whole parse refused). */
    REJECTED,
    /** Tolerated and silently corrected (e.g. trimmed whitespace, lower-cased). */
    NORMALISED,
    /** Parsed as-is, but worth surfacing (e.g. name over 64 chars). */
    WARNING
  }

  /**
   * A single conformance/security finding. {@code code} is a stable machine token; {@code detail}
   * is human-facing.
   */
  public record Violation(Severity severity, String code, String detail) {
    @Override
    public String toString() {
      return severity + " [" + code + "] " + detail;
    }
  }

  /** No {@link Severity#REJECTED} findings — nothing dangerous or unrecoverable was seen. */
  public boolean isValid() {
    return violations.stream().noneMatch(v -> v.severity() == Severity.REJECTED);
  }

  /** Valid and produced at least one usable media type. */
  public boolean isAcceptable() {
    return isValid() && !mediaTypes.isEmpty();
  }

  /**
   * RFC 9110 §12.5.1: an absent or empty {@code Accept} header is equivalent to {@code *}{@code /*}
   * — the client accepts any representation.
   */
  public List<MediaType> effectiveRanges() {
    return mediaTypes.isEmpty() ? List.of(WILDCARD_RANGE) : mediaTypes;
  }

  public Optional<MediaType> first() {
    return mediaTypes.isEmpty() ? Optional.empty() : Optional.of(mediaTypes.getFirst());
  }

  public List<URI> profiles() {
    return mediaTypes.stream().map(MediaType::profiles).flatMap(Collection::stream).toList();
  }

  /** Accept ranges ordered most-preferred first: higher q wins, ties broken by specificity. */
  public List<MediaType> preferenceOrder() {
    return mediaTypes.stream().sorted(MOST_PREFERRED_RANGE_FIRST).toList();
  }

  /** 2 = fully specified, 1 = {@code type/*}, 0 = {@code *}/{@code *}. */
  private static int specificity(MediaType mediaType) {
    if (mediaType.isWildcardType()) {
      return WILDCARD_TYPE_SPECIFICITY;
    }
    return mediaType.isWildcardSubtype() ? WILDCARD_SUBTYPE_SPECIFICITY : FULL_TYPE_SPECIFICITY;
  }
}
