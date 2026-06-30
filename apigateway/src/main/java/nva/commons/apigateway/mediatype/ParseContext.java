package nva.commons.apigateway.mediatype;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nva.commons.apigateway.mediatype.MediaTypeParseResult.Severity;
import nva.commons.apigateway.mediatype.MediaTypeParseResult.Violation;

/**
 * Mutable per-call parsing context. Accumulates results and violations, drives the top-level parse
 * pipeline, and delegates to {@link InputGuard} for pre-parse validation and {@link
 * ParameterParser} for parameter parsing. Created fresh for each call to {@link
 * MediaTypeParser#parse} or {@link MediaTypeParser#parseList}.
 *
 * <p>Package-private: not part of the public API.
 */
final class ParseContext implements ParameterHandler.Context {

  // Violation codes
  private static final String UNEXPECTED_LIST = "unexpected_list";
  private static final String TOO_MANY_ELEMENTS = "too_many_elements";
  private static final String MISSING_SUBTYPE = "missing_subtype";
  private static final String WILDCARD_IN_CONTENT_TYPE = "wildcard_in_content_type";
  private static final String TYPE_NOT_ALLOWED = "type_not_allowed";
  private static final String NAME_TOO_LONG = "name_too_long";
  private static final String NAME_OVER_SOFT_LIMIT = "name_over_soft_limit";

  // Syntax chars
  private static final char COMMA_SEPARATOR = ',';
  private static final char PARAMETER_SEPARATOR = ';';
  private static final char TYPE_SUBTYPE_SEPARATOR = '/';

  // Limits
  private static final int MAX_CONTENT_TYPE_COUNT = 1;
  private static final int MAX_NAME_LENGTH = 127;
  private static final int SOFT_NAME_LENGTH = 64;

  // Role labels used in dynamically-constructed violation codes and messages
  private static final String TYPE_ROLE = "type";
  private static final String SUBTYPE_ROLE = "subtype";

  private final ParserConfig configuration;
  private final boolean accept;
  private final List<MediaType> types = new ArrayList<>();
  private final List<Violation> violations = new ArrayList<>();
  private final InputGuard inputGuard;
  private final ParameterParser parameterParser;
  private int rejections;

  ParseContext(ParserConfig configuration, boolean accept) {
    this.configuration = configuration;
    this.accept = accept;
    this.inputGuard = new InputGuard(configuration, this);
    this.parameterParser = new ParameterParser(configuration, this);
  }

  private record NameValue(String name, String value) {}

  @Override
  public boolean isAcceptList() {
    return accept;
  }

  int rejectionCount() {
    return rejections;
  }

  void add(MediaType mediaType) {
    types.add(mediaType);
  }

  @Override
  public void reject(String code, String detail) {
    rejections++;
    violations.add(new Violation(Severity.REJECTED, code, detail));
  }

  @Override
  public void normalize(String code, String detail) {
    violations.add(new Violation(Severity.NORMALIZED, code, detail));
  }

  @Override
  public void warn(String code, String detail) {
    violations.add(new Violation(Severity.WARNING, code, detail));
  }

  MediaTypeParseResult result() {
    return new MediaTypeParseResult(types, violations);
  }

  MediaTypeParseResult parse(String input) {
    if (inputGuard.validate(input)) {
      var nonBlank =
          MediaTypeLexer.split(input.strip(), COMMA_SEPARATOR).stream()
              .filter(not(String::isBlank))
              .toList();
      if (nonBlank.size() > MAX_CONTENT_TYPE_COUNT) {
        reject(
            UNEXPECTED_LIST, "Content-Type must be a single media type, found " + nonBlank.size());
      } else if (!nonBlank.isEmpty()) {
        parseOne(nonBlank.getFirst());
      }
    }
    return result();
  }

  MediaTypeParseResult parseList(String input) {
    if (inputGuard.validate(input)) {
      var elements = MediaTypeLexer.split(input.strip(), COMMA_SEPARATOR);
      if (elements.size() > configuration.maxListElements()) {
        reject(
            TOO_MANY_ELEMENTS,
            "List has " + elements.size() + " entries, limit " + configuration.maxListElements());
      } else {
        elements.stream().filter(not(String::isBlank)).forEach(this::parseOne);
      }
    }
    return result();
  }

  void parseOne(String segment) {
    var parts = MediaTypeLexer.split(segment, PARAMETER_SEPARATOR);
    var typeSubtype = extractTypeSubtype(parts.getFirst().strip());
    if (typeSubtype.isEmpty()) {
      return;
    }
    var type = typeSubtype.get().name();
    var subtype = typeSubtype.get().value();
    if (!validateForContext(type, subtype)) {
      return;
    }
    var params = parameterParser.buildParameters(parts);
    if (params.isEmpty()) {
      return;
    }
    var candidate = new MediaType(type, subtype, params.get());
    if (!isAllowedType(candidate)) {
      return;
    }
    add(candidate);
  }

  private Optional<NameValue> extractTypeSubtype(String fullType) {
    int slash = fullType.indexOf(TYPE_SUBTYPE_SEPARATOR);
    if (slash < 0) {
      reject(MISSING_SUBTYPE, "No '/' in media type");
      return Optional.empty();
    }
    var type = fullType.substring(0, slash).strip();
    var subtype = fullType.substring(slash + 1).strip();
    if (nameIsInvalid(type, TYPE_ROLE) || nameIsInvalid(subtype, SUBTYPE_ROLE)) {
      return Optional.empty();
    }
    return Optional.of(new NameValue(type, subtype));
  }

  private boolean validateForContext(String type, String subtype) {
    if (!accept && (MediaType.WILDCARD.equals(type) || MediaType.WILDCARD.equals(subtype))) {
      reject(WILDCARD_IN_CONTENT_TYPE, "Wildcards are not valid in a Content-Type");
      return false;
    }
    return true;
  }

  private boolean isAllowedType(MediaType candidate) {
    if (!accept
        && !configuration.allowedTypes().isEmpty()
        && configuration.allowedTypes().stream().noneMatch(candidate::matchedBy)) {
      reject(TYPE_NOT_ALLOWED, "Type not in allow-list: " + candidate.essence());
      return false;
    }
    return true;
  }

  private boolean violatesGrammar(String name) {
    return configuration.enforceRestrictedNames()
        ? !MediaTypeLexer.isRestrictedName(name)
        : !MediaTypeLexer.isToken(name);
  }

  boolean nameIsInvalid(String name, String role) {
    if (name.isEmpty()) {
      reject("empty_" + role, "Empty " + role);
      return true;
    }
    if (MediaType.WILDCARD.equals(name)) {
      return false;
    }
    if (name.length() > MAX_NAME_LENGTH) {
      reject(NAME_TOO_LONG, role + " exceeds " + MAX_NAME_LENGTH + " chars");
      return true;
    }
    if (violatesGrammar(name)) {
      reject("invalid_" + role, "Not a valid " + role + " name");
      return true;
    }
    if (name.length() > SOFT_NAME_LENGTH) {
      warn(
          NAME_OVER_SOFT_LIMIT, role + " exceeds the RFC 6838 SHOULD limit of " + SOFT_NAME_LENGTH);
    }
    return false;
  }
}
