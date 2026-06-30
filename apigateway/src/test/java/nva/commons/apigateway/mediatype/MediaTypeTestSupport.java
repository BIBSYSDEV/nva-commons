package nva.commons.apigateway.mediatype;

import nva.commons.apigateway.mediatype.MediaTypeParseResult.Severity;

/** Shared test helpers for media-type tests. */
final class MediaTypeTestSupport {

  private static final MediaTypeParser PARSER = MediaTypeParser.defaultParser();

  private MediaTypeTestSupport() {
    // NO-OP
  }

  static boolean rejected(MediaTypeParseResult result, String code) {
    return result.violations().stream()
        .anyMatch(
            violation ->
                violation.severity() == Severity.REJECTED && violation.code().equals(code));
  }

  static boolean hasCode(MediaTypeParseResult result, String code) {
    return result.violations().stream().anyMatch(violation -> violation.code().equals(code));
  }

  static MediaType parseMediaType(String input) {
    return PARSER.parse(input).first().orElseThrow();
  }
}
