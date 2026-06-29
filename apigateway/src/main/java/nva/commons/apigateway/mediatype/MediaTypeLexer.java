package nva.commons.apigateway.mediatype;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Low-level lexical utilities for RFC 9110 / RFC 6838 media-type grammar. Pure functions with no
 * parser state — extracted from {@link MediaTypeParser} so that cohesion metrics stay meaningful.
 */
final class MediaTypeLexer {

  private static final char DOUBLE_QUOTE = '"';
  private static final char BACKSLASH = '\\';
  private static final int MAX_ASCII = 0x7F;
  private static final int ESCAPE_PAIR_LENGTH = 2;
  private static final int QUOTED_CONTENT_START = 1;
  private static final String TCHAR_EXTRA_CHARS = "!#$%&'*+-.^_`|~";
  private static final String RESTRICTED_NAME_EXTRA_CHARS = "!#$&-^_.+";

  private MediaTypeLexer() {
    // NO-OP
  }

  /**
   * Splits on {@code separator}, ignoring separators inside double quotes (honouring {@code \}
   * escapes).
   */
  static List<String> split(String input, char separator) {
    var segments = new ArrayList<String>();
    var currentSegment = new StringBuilder();
    boolean inQuotes = false;
    int characterIndex = 0;
    while (characterIndex < input.length()) {
      char character = input.charAt(characterIndex);
      if (isEscapeStart(inQuotes, character, characterIndex, input)) {
        currentSegment.append(character).append(input.charAt(characterIndex + 1));
        characterIndex += ESCAPE_PAIR_LENGTH;
      } else {
        if (character == DOUBLE_QUOTE) {
          inQuotes = !inQuotes;
          currentSegment.append(character);
        } else if (isSeparatorOutsideQuotes(character, separator, inQuotes)) {
          segments.add(currentSegment.toString());
          currentSegment.setLength(0);
        } else {
          currentSegment.append(character);
        }
        characterIndex++;
      }
    }
    segments.add(currentSegment.toString());
    return segments;
  }

  private static boolean isEscapeStart(
      boolean inQuotes, char character, int characterIndex, String input) {
    return inQuotes
        && character == BACKSLASH
        && characterIndex + ESCAPE_PAIR_LENGTH <= input.length();
  }

  private static boolean isSeparatorOutsideQuotes(
      char character, char separator, boolean inQuotes) {
    return character == separator && !inQuotes;
  }

  /** Unquotes a quoted-string, undoing quoted-pair escapes. Returns empty if malformed. */
  static Optional<String> unquote(String quotedString) {
    var result = new StringBuilder();
    boolean closed = false;
    int characterIndex = QUOTED_CONTENT_START;
    while (characterIndex < quotedString.length()) {
      char character = quotedString.charAt(characterIndex);
      if (character == BACKSLASH && characterIndex + 1 < quotedString.length()) {
        result.append(quotedString.charAt(characterIndex + 1));
        characterIndex += ESCAPE_PAIR_LENGTH;
      } else if (character == DOUBLE_QUOTE) {
        if (characterIndex != quotedString.length() - 1) {
          return Optional.empty();
        }
        closed = true;
        break;
      } else {
        result.append(character);
        characterIndex++;
      }
    }
    return closed ? Optional.of(result.toString()) : Optional.empty();
  }

  static boolean isToken(String input) {
    return !input.isEmpty() && input.chars().allMatch(character -> isTokenChar((char) character));
  }

  /** RFC 9110 tchar. */
  static boolean isTokenChar(char character) {
    return isAlnum(character) || TCHAR_EXTRA_CHARS.indexOf(character) >= 0;
  }

  /** RFC 6838 restricted-name: stricter than tchar. */
  static boolean isRestrictedName(String input) {
    return !input.isEmpty()
        && isAlnum(input.charAt(0))
        && input.chars().skip(1).allMatch(character -> isRestrictedNameChar((char) character));
  }

  private static boolean isRestrictedNameChar(char character) {
    return isAlnum(character) || RESTRICTED_NAME_EXTRA_CHARS.indexOf(character) >= 0;
  }

  static boolean containsObsText(String input) {
    return input.chars().anyMatch(character -> character > MAX_ASCII);
  }

  private static boolean isAlnum(char character) {
    return character >= 'a' && character <= 'z'
        || character >= 'A' && character <= 'Z'
        || character >= '0' && character <= '9';
  }
}
