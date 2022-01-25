package nva.commons.core;

import static java.util.Objects.isNull;

public final class StringUtils {

    public static final String DOUBLE_WHITESPACE = "\\s\\s";
    public static final String WHITESPACES = "\\s+";
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
        return input.replaceAll(WHITESPACES, EMPTY_STRING);
    }

    /**
     * Checks if string input is blank or null.
     *
     * @param input input string
     * @return <code>true</code> if blank.
     */
    public static boolean isBlank(String input) {
        return isNull(input) || input.isBlank();
    }

    /**
     * Checks if string input is empty or null.
     *
     * @param input input string
     * @return <code>true</code> if empty.
     */
    public static boolean isEmpty(String input) {
        return isNull(input) || input.isEmpty();
    }

    /**
     * Replaces  whitespaces with space.
     *
     * @param str input string.
     * @return string with all whitespaces replaced by spaces
     */
    public static String replaceWhiteSpacesWithSpace(String str) {
        return str.replaceAll("\\s", " ");
    }

    /**
     * Checks if string is neither null or empty.
     *
     * @param string string to check
     * @return <code>true</code> if string is neither null or empty.
     */
    public static boolean isNotEmpty(String string) {
        return !isEmpty(string);
    }

    /**
     * Checks if string is neither null or blank.
     *
     * @param string string to check
     * @return <code>true</code> if string is neither null or empty.
     */
    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }
}
