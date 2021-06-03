package nva.commons.core.language;

import java.net.URI;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguageMapper {

    public static final String LEXVO_URI_PREFIX = "http://lexvo.org/id/iso639-3/";
    public static final URI LEXVO_URI_UNDEFINED = URI.create(LEXVO_URI_PREFIX + "und");
    public static final String ERROR_MESSAGE_MISSING_RESOURCE_EXCEPTION = "Failing to retrieve UIR for the "
        + "language code ";
    private static final Logger logger = LoggerFactory.getLogger(LanguageMapper.class);

    public static Optional<URI> toUriOpt(String languageCode) {
        try {
            return Optional.ofNullable(languageCode)
                    .filter(StringUtils::isNotBlank)
                    .map(Locale::new)
                    .map(Locale::getISO3Language)
                    .map(iso3 -> URI.create(LEXVO_URI_PREFIX + iso3));
        } catch (MissingResourceException e) {
            logger.warn(ERROR_MESSAGE_MISSING_RESOURCE_EXCEPTION + languageCode, e);
            return Optional.empty();
        }
    }

    public static URI toUri(String languageCode) {
        return toUriOpt(languageCode).orElse(LEXVO_URI_UNDEFINED);
    }
}
