package nva.commons.apigateway.mediatype;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stateless-per-type parameter parser. Handles splitting, whitespace normalisation, value
 * resolution, and handler dispatch for a single media-type's parameter segment list.
 *
 * <p>Package-private: not part of the public API.
 */
final class ParameterParser {

  private static final String EMPTY_PARAMETER = "empty_parameter";
  private static final String TOO_MANY_PARAMETERS = "too_many_parameters";
  private static final String INVALID_PARAMETER_NAME = "invalid_parameter_name";
  private static final String DUPLICATE_PARAMETER = "duplicate_parameter";
  private static final String SPACE_AROUND_EQUALS = "space_around_equals";
  private static final String UNKNOWN_PARAMETER = "unknown_parameter";
  private static final String MALFORMED_QUOTED_STRING = "malformed_quoted_string";
  private static final String OBS_TEXT = "obs_text";
  private static final String UNQUOTED_PROFILE = "unquoted_profile";
  private static final String INVALID_TOKEN_VALUE = "invalid_token_value";

  private static final char EQUALS_SIGN = '=';
  private static final char OPENING_DQUOTE = '"';

  private final ParserConfig configuration;
  private final ParseContext context;

  ParameterParser(ParserConfig configuration, ParseContext context) {
    this.configuration = configuration;
    this.context = context;
  }

  private record NameValue(String name, String value) {}

  Optional<Map<String, String>> buildParameters(List<String> parts) {
    var params = new LinkedHashMap<String, String>();
    return parseParameters(parts, params) ? Optional.of(params) : Optional.empty();
  }

  boolean parseParameters(List<String> parts, Map<String, String> params) {
    for (var part : parts.subList(1, parts.size())) {
      var raw = part.strip();
      if (raw.isEmpty()) {
        if (!configuration.acceptEmptyParameters()) {
          context.reject(EMPTY_PARAMETER, "Empty parameter");
          return false;
        }
        continue;
      }
      if (params.size() >= configuration.maxParametersPerType()) {
        context.reject(
            TOO_MANY_PARAMETERS,
            "More than " + configuration.maxParametersPerType() + " parameters");
        return false;
      }
      if (!parseParameter(raw, params)) {
        return false;
      }
    }
    return true;
  }

  boolean parseParameter(String raw, Map<String, String> params) {
    var nameValue = splitOnEquals(raw);
    if (nameValue.isEmpty()) {
      return false;
    }
    var unpacked = nameValue.get();
    var lowerName = unpacked.name().toLowerCase(Locale.ROOT);
    return validateParameterName(lowerName, params)
        && resolveValue(lowerName, unpacked.value())
            .map(resolved -> applyHandler(lowerName, resolved, params))
            .orElse(false);
  }

  private boolean validateParameterName(String lowerName, Map<String, String> params) {
    if (!MediaTypeLexer.isToken(lowerName)) {
      context.reject(INVALID_PARAMETER_NAME, "Not a valid parameter name token");
      return false;
    }
    if (params.containsKey(lowerName)) {
      if (configuration.rejectDuplicateParameters()) {
        context.reject(DUPLICATE_PARAMETER, "Parameter specified more than once");
        return false;
      }
      context.normalise(DUPLICATE_PARAMETER, "Duplicate parameter, keeping last");
    }
    return true;
  }

  /**
   * Splits on {@code =} and normalises any whitespace around it.
   *
   * @return the name/value pair, or empty if rejected.
   */
  Optional<NameValue> splitOnEquals(String raw) {
    int equalsIndex = raw.indexOf(EQUALS_SIGN);
    if (equalsIndex < 0) {
      return Optional.of(new NameValue(raw, ""));
    }
    return resolveWhitespace(raw.substring(0, equalsIndex), raw.substring(equalsIndex + 1));
  }

  private Optional<NameValue> resolveWhitespace(String name, String value) {
    if (isStripped(name) && isStripped(value)) {
      return Optional.of(new NameValue(name, value));
    }
    if (configuration.lenientWhitespaceAroundEquals()) {
      context.normalise(SPACE_AROUND_EQUALS, "Trimmed whitespace around '='");
      return Optional.of(new NameValue(name.strip(), value.strip()));
    }
    context.reject(SPACE_AROUND_EQUALS, "Whitespace around '=' not allowed");
    return Optional.empty();
  }

  private static boolean isStripped(String value) {
    return value.equals(value.strip());
  }

  /**
   * Dispatches to the registered handler (if any) and stores the parameter.
   *
   * @return {@code true} if the parameter was accepted, {@code false} if rejected.
   */
  boolean applyHandler(String lowerName, String resolvedValue, Map<String, String> params) {
    if (!runHandler(lowerName, resolvedValue)) {
      return false;
    }
    params.put(lowerName, resolvedValue);
    return true;
  }

  private boolean runHandler(String lowerName, String resolvedValue) {
    var handler = configuration.handlers().get(lowerName);
    if (Objects.isNull(handler)) {
      return unknownParameterAccepted(lowerName);
    }
    return handlerAccepts(handler, resolvedValue);
  }

  private boolean unknownParameterAccepted(String lowerName) {
    if (configuration.rejectUnknownParameters()) {
      context.reject(UNKNOWN_PARAMETER, "No handler registered for parameter '" + lowerName + "'");
      return false;
    }
    return true;
  }

  private boolean handlerAccepts(ParameterHandler handler, String resolvedValue) {
    var before = context.rejectionCount();
    handler.validate(resolvedValue, context);
    return context.rejectionCount() == before;
  }

  /** Returns the unquoted/unescaped value, or empty if invalid (violation already recorded). */
  Optional<String> resolveValue(String name, String value) {
    if (!value.isEmpty() && value.charAt(0) == OPENING_DQUOTE) {
      return resolveQuotedValue(value);
    }
    return resolveUnquotedValue(name, value);
  }

  private Optional<String> resolveUnquotedValue(String name, String value) {
    if (!MediaTypeLexer.isToken(value) && !isUnquotedProfileException(name)) {
      context.reject(INVALID_TOKEN_VALUE, "Unquoted value is not a valid token");
      return Optional.empty();
    }
    if (isUnquotedProfileException(name)) {
      context.normalise(UNQUOTED_PROFILE, "Accepted unquoted profile value");
    }
    return Optional.of(value);
  }

  private boolean isUnquotedProfileException(String name) {
    return configuration.acceptSingleUnquotedProfileString()
        && ParameterHandler.PROFILE.equals(name);
  }

  private Optional<String> resolveQuotedValue(String value) {
    var unquoted = MediaTypeLexer.unquote(value);
    if (unquoted.isEmpty()) {
      context.reject(MALFORMED_QUOTED_STRING, "Unterminated or malformed quoted-string");
    } else if (containsRejectedObsText(unquoted.get())) {
      context.reject(OBS_TEXT, "Non-ASCII (obs-text) in parameter value");
      unquoted = Optional.empty();
    }
    return unquoted;
  }

  private boolean containsRejectedObsText(String value) {
    return !configuration.allowObsText() && MediaTypeLexer.containsObsText(value);
  }
}
