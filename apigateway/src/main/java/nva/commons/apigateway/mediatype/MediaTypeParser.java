package nva.commons.apigateway.mediatype;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reusable, immutable, thread-safe parser for media types and {@code Accept}-style lists.
 *
 * <p>Configuration (limits, leniency flags, allow-lists) is fixed at build time. The same parser
 * instance can be shared across requests. Parsing never throws; results carry both the salvaged
 * {@link MediaType}s and a list of {@link MediaTypeParseResult.Violation}s.
 *
 * <p>Security model: a small set of checks is a non-negotiable floor — control characters,
 * CR/LF/NUL, and length/count caps are rejected before any structural work, because the real risk
 * is <em>differential parsing</em> (your parser and a downstream component disagreeing on the same
 * bytes). Leniency flags only ever relax conformance, never the security floor.
 */
public final class MediaTypeParser {

  private static final int DEFAULT_MAX_INPUT_LENGTH = 8 * 1024;
  private static final int DEFAULT_MAX_LIST_ELEMENTS = 64;
  private static final int DEFAULT_MAX_PARAMETERS_PER_TYPE = 32;
  private static final int DEFAULT_MAX_PROFILE_URIS = 16;

  private final int maxInputLength;
  private final int maxListElements;
  private final int maxParametersPerType;
  private final boolean lenientWhitespaceAroundEquals;
  private final boolean acceptEmptyParameters;
  private final boolean acceptSingleUnquotedProfileString;
  private final boolean rejectDuplicateParameters;
  private final boolean rejectUnknownParameters;
  private final boolean allowObsText;
  private final boolean enforceRestrictedNames;
  private final Map<String, ParameterHandler> handlers;
  private final List<MediaType> allowedTypes;

  private MediaTypeParser(Builder builder) {
    this.maxInputLength = builder.maxInputLength;
    this.maxListElements = builder.maxListElements;
    this.maxParametersPerType = builder.maxParametersPerType;
    this.lenientWhitespaceAroundEquals = builder.lenientWhitespaceAroundEquals;
    this.acceptEmptyParameters = builder.acceptEmptyParameters;
    this.acceptSingleUnquotedProfileString = builder.acceptSingleUnquotedProfileString;
    this.rejectDuplicateParameters = builder.rejectDuplicateParameters;
    this.rejectUnknownParameters = builder.rejectUnknownParameters;
    this.allowObsText = builder.allowObsText;
    this.enforceRestrictedNames = builder.enforceRestrictedNames;
    this.handlers = Map.copyOf(builder.buildHandlers());
    this.allowedTypes = List.copyOf(builder.allowedTypes);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** A sensible, secure default: strong limits, web-friendly leniency, allow-lists left open. */
  public static MediaTypeParser defaultParser() {
    return builder()
        .maxInputLength(DEFAULT_MAX_INPUT_LENGTH)
        .maxListElements(DEFAULT_MAX_LIST_ELEMENTS)
        .maxParametersPerType(DEFAULT_MAX_PARAMETERS_PER_TYPE)
        .maxProfileUris(DEFAULT_MAX_PROFILE_URIS)
        .lenientWhitespaceAroundEquals()
        .acceptEmptyParameters()
        .acceptSingleUnquotedProfileString()
        .rejectDuplicateParameters(true)
        .allowObsText(false)
        .build();
  }

  /** Returns a configuration snapshot for use by {@link ParseContext}. */
  ParserConfig config() {
    return new ParserConfig(
        maxInputLength,
        maxListElements,
        maxParametersPerType,
        lenientWhitespaceAroundEquals,
        acceptEmptyParameters,
        acceptSingleUnquotedProfileString,
        rejectDuplicateParameters,
        rejectUnknownParameters,
        allowObsText,
        enforceRestrictedNames,
        handlers,
        allowedTypes);
  }

  /**
   * Parse a single value with {@code Content-Type} semantics: one entry, no wildcards, subtype
   * required.
   */
  public MediaTypeParseResult parse(String input) {
    return new ParseContext(config(), false).parse(input);
  }

  /** Parse an {@code Accept}-style list: comma-separated, wildcards and {@code q} permitted. */
  public MediaTypeParseResult parseList(String input) {
    return new ParseContext(config(), true).parseList(input);
  }

  @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
  public static final class Builder {
    private int maxInputLength = DEFAULT_MAX_INPUT_LENGTH;
    private int maxListElements = DEFAULT_MAX_LIST_ELEMENTS;
    private int maxParametersPerType = DEFAULT_MAX_PARAMETERS_PER_TYPE;
    private int maxProfileUris = DEFAULT_MAX_PROFILE_URIS;
    private boolean lenientWhitespaceAroundEquals;
    private boolean acceptEmptyParameters;
    private boolean acceptSingleUnquotedProfileString;
    private boolean rejectDuplicateParameters = true;
    private boolean rejectUnknownParameters;
    private boolean allowObsText;
    private boolean enforceRestrictedNames;
    private final List<Charset> allowedCharsets = new ArrayList<>();
    private final List<URI> allowedProfiles = new ArrayList<>();
    private final List<MediaType> allowedTypes = new ArrayList<>();
    private final Map<String, ParameterHandler> customHandlers = new LinkedHashMap<>();
    private boolean installBuiltins = true;

    private Builder() {}

    public Builder maxInputLength(int value) {
      this.maxInputLength = value;
      return this;
    }

    public Builder maxListElements(int value) {
      this.maxListElements = value;
      return this;
    }

    public Builder maxParametersPerType(int value) {
      this.maxParametersPerType = value;
      return this;
    }

    public Builder maxProfileUris(int value) {
      this.maxProfileUris = value;
      return this;
    }

    public Builder lenientWhitespaceAroundEquals() {
      this.lenientWhitespaceAroundEquals = true;
      return this;
    }

    public Builder acceptEmptyParameters() {
      this.acceptEmptyParameters = true;
      return this;
    }

    public Builder acceptSingleUnquotedProfileString() {
      this.acceptSingleUnquotedProfileString = true;
      return this;
    }

    public Builder rejectDuplicateParameters(boolean value) {
      this.rejectDuplicateParameters = value;
      return this;
    }

    public Builder rejectUnknownParameters(boolean value) {
      this.rejectUnknownParameters = value;
      return this;
    }

    public Builder allowObsText(boolean value) {
      this.allowObsText = value;
      return this;
    }

    public Builder enforceRestrictedNames(boolean value) {
      this.enforceRestrictedNames = value;
      return this;
    }

    /** Configure the built-in {@code charset} handler's allow-list. */
    public Builder allowedCharsets(Charset... charsets) {
      this.allowedCharsets.addAll(List.of(charsets));
      return this;
    }

    /** Configure the built-in {@code profile} handler's allow-list. */
    public Builder allowedProfiles(URI... uris) {
      this.allowedProfiles.addAll(List.of(uris));
      return this;
    }

    public Builder allowedTypes(MediaType... types) {
      this.allowedTypes.addAll(List.of(types));
      return this;
    }

    /** Register (or override, by name) one or more parameter handlers. */
    public Builder parameterHandler(ParameterHandler... handlers) {
      for (var handler : handlers) {
        this.customHandlers.put(handler.name(), handler);
      }
      return this;
    }

    /**
     * Drop the default charset/q/profile handlers (e.g. to define the parameter layer entirely
     * yourself).
     */
    public Builder withoutBuiltinHandlers() {
      this.installBuiltins = false;
      return this;
    }

    Map<String, ParameterHandler> buildHandlers() {
      var map = new LinkedHashMap<String, ParameterHandler>();
      if (installBuiltins) {
        var defaults =
            List.of(
                ParameterHandler.charsetHandler(List.copyOf(allowedCharsets)),
                ParameterHandler.qualityHandler(),
                ParameterHandler.profileHandler(maxProfileUris, List.copyOf(allowedProfiles)));
        defaults.forEach(handler -> map.put(handler.name(), handler));
      }
      map.putAll(customHandlers);
      return map;
    }

    public MediaTypeParser build() {
      return new MediaTypeParser(this);
    }
  }
}
