package nva.commons.utils;

public final class StringUtils {

    public static final String DOUBLE_WHITESPACE = "\\s\\s";
    public static final String SPACE = " ";

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
}
