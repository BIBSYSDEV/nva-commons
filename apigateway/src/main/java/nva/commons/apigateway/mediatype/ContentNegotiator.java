package nva.commons.apigateway.mediatype;

import static java.util.Collections.emptySet;

import java.net.URI;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Content negotiation: matches a server's offered representations against a parsed {@code Accept}
 * result and ranks them. Separate from the parser by design — it is the only part that needs to
 * know what the server can actually produce.
 *
 * <p>Matching (RFC 9110 §12.5.1): a representation's quality comes from the <em>most specific</em>
 * matching range. Range parameters other than {@code q} are hard match constraints ({@code
 * text/html;level=1} only matches a representation declaring {@code level=1}), with two exceptions:
 *
 * <ul>
 *   <li>{@code charset} (and any name passed to {@link #ignoring}) is dropped from matching and
 *       specificity — it is observed on {@code Accept} in the wild but is not a useful negotiation
 *       key.
 *   <li>{@code profile} is never a gate. Per RFC 6906 it is an <em>unordered set</em> of URIs
 *       (order and duplicates are insignificant), and per the {@code application/ld+json}
 *       registration a server honors the profiles it recognizes and ignores the rest. So {@code
 *       profile} is treated as a <em>preference</em>: a representation is scored by how many
 *       requested profiles it honors (the intersection size), and that score breaks ties after
 *       {@code q}. A profile that no representation honors never causes a 406.
 * </ul>
 *
 * An empty {@code Accept} means "anything".
 */
public final class ContentNegotiator {

  private static final double EXCLUDED_QUALITY = 0.0;

  private static final Comparator<Preference> MOST_PREFERRED_FIRST =
      Comparator.comparingDouble(Preference::quality)
          .reversed()
          .thenComparing(Comparator.comparingInt(Preference::profileMatchScore).reversed());

  private static final int SPECIFICITY_SCALE = 100;
  private static final int EMPTY_PROFILE_SET = 0;

  private final Set<String> ignoredMatchParameters;

  private ContentNegotiator(Set<String> ignoredMatchParameters) {
    this.ignoredMatchParameters =
        ignoredMatchParameters.stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Sensible default: {@code charset} is ignored when matching; {@code profile} is a preference.
   */
  public static ContentNegotiator defaultNegotiator() {
    return new ContentNegotiator(Set.of(ParameterHandler.CHARSET));
  }

  /** Every parameter except {@code q} and {@code profile} is a hard match constraint. */
  public static ContentNegotiator strict() {
    return new ContentNegotiator(emptySet());
  }

  /** Ignore exactly the named parameters when matching (case-insensitive). */
  public static ContentNegotiator ignoring(String... parameterNames) {
    return new ContentNegotiator(Set.of(parameterNames));
  }

  /**
   * One acceptable representation: what to serve, how desirable, and the client range it satisfied.
   */
  public record Preference(MediaType representation, MediaType matchedRange, double quality) {

    public boolean isAcceptable() {
      return quality > EXCLUDED_QUALITY;
    }

    /** Charset of the chosen representation, if it declares one. */
    public Optional<String> charsetName() {
      return representation.charsetName();
    }

    /** Profiles of the chosen representation. */
    public List<URI> profiles() {
      return representation.profiles();
    }

    /**
     * Profiles the client requested on the matched range (order as written; semantically a set).
     */
    public List<URI> requestedProfiles() {
      return matchedRange.profiles();
    }

    /** The requested profiles this representation actually honors (the intersection). */
    public List<URI> honoredProfiles() {
      var representationProfiles = new HashSet<>(representation.profiles());
      return matchedRange.profiles().stream()
          .distinct()
          .filter(representationProfiles::contains)
          .toList();
    }

    /** How many requested profiles this representation honors — the profile preference score. */
    public int profileMatchScore() {
      return honoredProfiles().size();
    }
  }

  /**
   * Rank {@code offered} against a parsed {@code Accept} result, most-preferred first. Excluded:
   * representations matching no range, or whose best match has q=0. Order: quality desc, then
   * profile-overlap desc, then the server's order in {@code offered}.
   */
  public List<Preference> rank(MediaTypeParseResult accept, List<MediaType> offered) {
    var ranges = accept.effectiveRanges();
    return offered.stream()
        .map(representation -> bestMatchingRange(ranges, representation))
        .flatMap(Optional::stream)
        .sorted(MOST_PREFERRED_FIRST)
        .toList();
  }

  private Optional<Preference> bestMatchingRange(List<MediaType> ranges, MediaType representation) {
    return ranges.stream()
        .filter(range -> matches(range, representation))
        .max(rangePrecedenceFor(representation))
        .filter(this::hasPositiveQuality)
        .map(range -> new Preference(representation, range, range.quality()));
  }

  private boolean hasPositiveQuality(MediaType range) {
    return range.quality() > EXCLUDED_QUALITY;
  }

  private Comparator<MediaType> rangePrecedenceFor(MediaType representation) {
    return Comparator.comparingInt(this::specificity)
        .thenComparingDouble(MediaType::quality)
        .thenComparingInt(range -> profileOverlap(range, representation));
  }

  /**
   * The single best representation to serve, or empty if none is acceptable (caller should send
   * 406).
   */
  public Optional<MediaType> best(MediaTypeParseResult accept, List<MediaType> offered) {
    var ranked = rank(accept, offered);
    return ranked.isEmpty() ? Optional.empty() : Optional.of(ranked.getFirst().representation());
  }

  private boolean matches(MediaType range, MediaType representation) {
    return representation.matchedBy(range) && hardConstraintParametersMatch(range, representation);
  }

  private boolean hardConstraintParametersMatch(MediaType range, MediaType representation) {
    return range.parameters().entrySet().stream()
        .filter(entry -> isHardConstraint(entry.getKey()))
        .allMatch(
            entry -> entry.getValue().equals(representation.parameters().get(entry.getKey())));
  }

  /**
   * Higher is more specific: type/subtype dominates wildcards, then more constraining parameters.
   */
  private int specificity(MediaType range) {
    return range.typeSpecificity() * SPECIFICITY_SCALE
        + (int) range.parameters().keySet().stream().filter(this::isHardConstraint).count();
  }

  /** Size of the intersection between the range's requested profiles and the representation's. */
  private static int profileOverlap(MediaType range, MediaType representation) {
    var requested = new HashSet<>(range.profiles());
    if (requested.isEmpty()) {
      return EMPTY_PROFILE_SET;
    }
    requested.retainAll(representation.profiles());
    return requested.size();
  }

  private boolean isHardConstraint(String parameterName) {
    return !ParameterHandler.QUALITY.equals(parameterName)
        && !ParameterHandler.PROFILE.equals(parameterName)
        && !ignoredMatchParameters.contains(parameterName);
  }
}
