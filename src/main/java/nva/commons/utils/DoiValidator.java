package nva.commons.utils;

import static java.util.Objects.isNull;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DoiValidator {

    public static final Pattern DOI_URL_PATTERN =
        Pattern.compile("^https?://(dx\\.)?doi\\.org/(10(?:\\.[0-9]+)+)/(.+)$",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern DOI_STRING_PATTERN =
        Pattern.compile("^(doi:)?(10(?:\\.[0-9]+)+)/(.+)$", Pattern.CASE_INSENSITIVE);

    private DoiValidator() {
    }

    /**
     * Validates a DOI against URL and String patterns.
     *
     * @param doi the DOI to validate
     * @return true if DOI is valid
     */
    public static boolean validate(String doi) {
        if (isNull(doi)) {
            return false;
        }
        Matcher urlMatcher = DOI_URL_PATTERN.matcher(doi);
        Matcher stringMatcher = DOI_STRING_PATTERN.matcher(doi);
        return urlMatcher.find() || stringMatcher.find();
    }

    /**
     * Validates a DOI against URL and String patterns.
     *
     * @param doi the DOI to validate
     * @return true if DOI is valid
     */
    public static Boolean validate(URL doi) {
        if (isNull(doi)) {
            return false;
        }
        return validate(doi.toString());
    }
}