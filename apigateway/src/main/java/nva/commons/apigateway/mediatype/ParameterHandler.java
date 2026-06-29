package nva.commons.apigateway.mediatype;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Pluggable validation for a single media-type parameter. The parser handles the fixed lexical
 * grammar (splitting, quoting, tokens) and then hands each parameter's already unquoted/unescaped
 * value to the handler registered for its name. Parameters with no registered handler are accepted
 * as opaque key/value pairs, so unknown or future-spec parameters pass through without code
 * changes.
 *
 * <p>Register custom handlers via {@link MediaTypeParser.Builder#parameterHandler}.
 */
public interface ParameterHandler {

  String CHARSET = "charset";
  String QUALITY = "q";
  String PROFILE = "profile";

  /** Lower-case parameter name this handler is responsible for. */
  String name();

  /**
   * Validate (and optionally flag) the value. Record findings via {@code ctx}; a REJECTED finding
   * drops the type.
   */
  void validate(String value, Context ctx);

  /** The slice of parser state a handler is allowed to see and affect. */
  interface Context {
    /** True when parsing an {@code Accept}-style list, false for a single {@code Content-Type}. */
    boolean isAcceptList();

    void reject(String code, String detail);

    void warn(String code, String detail);

    void normalise(String code, String detail);
  }

  static ParameterHandler charsetHandler(List<Charset> allowed) {
    return new ParameterHandler() {
      private static final String UNKNOWN_CHARSET = "unknown_charset";
      private static final String CHARSET_NOT_ALLOWED = "charset_not_allowed";

      @Override
      public String name() {
        return CHARSET;
      }

      @Override
      public void validate(String value, Context ctx) {
        try {
          var resolved = Charset.forName(value);
          if (!allowed.isEmpty() && !allowed.contains(resolved)) {
            ctx.reject(CHARSET_NOT_ALLOWED, "Charset not in allow-list: " + resolved.name());
          }
        } catch (IllegalArgumentException ignored) {
          ctx.reject(UNKNOWN_CHARSET, "Unresolvable charset");
        }
      }
    };
  }

  static ParameterHandler qualityHandler() {
    return new ParameterHandler() {
      private static final String Q_IN_CONTENT_TYPE = "q_in_content_type";
      private static final String INVALID_QVALUE = "invalid_qvalue";
      private static final Pattern QVALUE_PATTERN =
          Pattern.compile("(?:0(?:\\.\\d{0,3})?|1(?:\\.0{0,3})?)");

      @Override
      public String name() {
        return QUALITY;
      }

      @Override
      public void validate(String value, Context ctx) {
        if (!ctx.isAcceptList()) {
          ctx.warn(Q_IN_CONTENT_TYPE, "'q' weight is only meaningful in Accept");
        }
        if (!QVALUE_PATTERN.matcher(value).matches()) {
          ctx.warn(
              INVALID_QVALUE,
              "q value violates the qvalue grammar (0-1, <=3 decimals); clamped to [0,1]");
        }
      }
    };
  }

  static ParameterHandler profileHandler(int maxUris, List<URI> allowed) {
    return new ParameterHandler() {
      private static final String TOO_MANY_PROFILES = "too_many_profiles";
      private static final String INVALID_PROFILE_URI = "invalid_profile_uri";
      private static final String RELATIVE_PROFILE_URI = "relative_profile_uri";
      private static final String PROFILE_NOT_ALLOWED = "profile_not_allowed";
      private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

      @Override
      public String name() {
        return PROFILE;
      }

      @Override
      public void validate(String value, Context ctx) {
        var tokens = tokenise(value);
        if (tokens.size() > maxUris) {
          ctx.reject(TOO_MANY_PROFILES, tokens.size() + " profile URIs, limit " + maxUris);
          return;
        }
        for (var token : tokens) {
          if (!isValidProfileUri(token, ctx)) {
            return;
          }
        }
      }

      private List<String> tokenise(String value) {
        return WHITESPACE_PATTERN
            .splitAsStream(value.strip())
            .filter(Predicate.not(String::isEmpty))
            .toList();
      }

      private boolean isValidProfileUri(String token, Context ctx) {
        return parseUri(token, ctx).map(uri -> validateProfileUri(uri, ctx)).orElse(false);
      }

      private Optional<URI> parseUri(String token, Context ctx) {
        try {
          return Optional.of(new URI(token));
        } catch (URISyntaxException ignored) {
          ctx.reject(INVALID_PROFILE_URI, "Not a valid URI");
          return Optional.empty();
        }
      }

      private boolean validateProfileUri(URI uri, Context ctx) {
        if (!uri.isAbsolute()) {
          ctx.reject(RELATIVE_PROFILE_URI, "Profile URI must be absolute");
          return false;
        }
        if (!allowed.isEmpty() && !allowed.contains(uri)) {
          ctx.reject(PROFILE_NOT_ALLOWED, "Profile not in allow-list");
          return false;
        }
        return true;
      }
    };
  }
}
