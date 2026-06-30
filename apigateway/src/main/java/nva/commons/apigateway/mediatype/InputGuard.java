package nva.commons.apigateway.mediatype;

import static java.util.Objects.isNull;

/**
 * Pre-parse security checks applied to every input string before structural parsing begins.
 * Enforces null safety, length limits, and control-character exclusion.
 *
 * <p>Package-private: not part of the public API.
 */
final class InputGuard {

  private static final String NULL_INPUT = "null_input";
  private static final String INPUT_TOO_LONG = "input_too_long";
  private static final String CONTROL_CHARACTER = "control_character";

  private static final char TAB = '\t';
  private static final char CR = '\r';
  private static final char LF = '\n';
  private static final char NUL = '\0';
  private static final char LOWEST_PRINTABLE_ASCII = 0x20;
  private static final char DELETE_CONTROL_CHARACTER = 0x7F;

  private final ParserConfig configuration;
  private final ParameterHandler.Context context;

  InputGuard(ParserConfig configuration, ParameterHandler.Context context) {
    this.configuration = configuration;
    this.context = context;
  }

  boolean validate(String input) {
    return notNull(input) && withinLengthLimit(input) && freeOfControlCharacters(input);
  }

  private boolean notNull(String input) {
    if (isNull(input)) {
      context.reject(NULL_INPUT, "Input was null");
      return false;
    }
    return true;
  }

  private boolean withinLengthLimit(String input) {
    if (input.length() > configuration.maxInputChars()) {
      context.reject(
          INPUT_TOO_LONG,
          "Input length "
              + input.length()
              + " chars exceeds limit of "
              + configuration.maxInputChars());
      return false;
    }
    return true;
  }

  private boolean freeOfControlCharacters(String input) {
    for (int characterIndex = 0; characterIndex < input.length(); characterIndex++) {
      char character = input.charAt(characterIndex);
      if (isControlCharacter(character)) {
        context.reject(
            CONTROL_CHARACTER,
            "Forbidden " + describeControlChar(character) + " at index " + characterIndex);
        return false;
      }
    }
    return true;
  }

  private static boolean isControlCharacter(char character) {
    return character < LOWEST_PRINTABLE_ASCII && character != TAB
        || character == DELETE_CONTROL_CHARACTER;
  }

  private static String describeControlChar(char character) {
    return switch (character) {
      case CR -> "CR";
      case LF -> "LF";
      case NUL -> "NUL";
      default -> "control char 0x" + Integer.toHexString(character);
    };
  }
}
