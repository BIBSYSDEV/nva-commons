package nva.commons.utils;

public final class StringUtils {

    public static final String DOUBLE_WHITESPACE = "\\s\\s";
    public static final String WHITESPACE = "\\s";
    public static final String SPACE = " ";
    public static final String EMPTY_STRING = "";

    private StringUtils() {
    }

    /**
     * Replaces multiple consecutive whitespaces with a single whitespace.
     *
     * @param input A string with or without multiple consecutive whitespaces.
     * @return A string without multiple consecutive whitespaces where are whitespaces have been replaced by a space.
     */
    public static String removeMultipleWhiteSpaces(String input) {
        String buffer = input.trim();
        String result = buffer.replaceAll(DOUBLE_WHITESPACE, SPACE);
        while (!result.equals(buffer)) {
            buffer = result;
            result = buffer.replaceAll(DOUBLE_WHITESPACE, SPACE);
        }
        return result;
    }

    /**
     * Remove all whitespaces.
     *
     * @param input A string with or without multiple consecutive whitespaces.
     * @return a string without spaces.
     */
    public static String removeWhiteSpaces(String input) {
        String buffer = input.trim();
        String result = buffer.replaceAll(WHITESPACE, EMPTY_STRING);
        while (!result.equals(buffer)) {
            buffer = result;
            result = buffer.replaceAll(WHITESPACE, EMPTY_STRING);
        }
        return result;
    }
}
