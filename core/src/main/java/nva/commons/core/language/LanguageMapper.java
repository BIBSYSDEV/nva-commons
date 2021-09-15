package nva.commons.core.language;

import java.net.URI;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LanguageMapper {

    public static final Map<String, String> IS02_TO_ISO3_CODES = hardCodedMappings();
    public static final String LEXVO_URI_PREFIX = "http://lexvo.org/id/iso639-3/";
    public static final URI LEXVO_URI_UNDEFINED = URI.create(LEXVO_URI_PREFIX + "und");
    public static final String ERROR_MESSAGE_MISSING_RESOURCE_EXCEPTION = "Failing to retrieve URI for the "
                                                                          + "language code ";
    private static final Logger logger = LoggerFactory.getLogger(LanguageMapper.class);

    private LanguageMapper() {

    }

    public static URI toUri(String languageCode) {
        return toIso3Code(languageCode)
                   .map(iso3 -> URI.create(LEXVO_URI_PREFIX + iso3))
                   .orElse(LEXVO_URI_UNDEFINED);
    }

    private static Map<String, String> hardCodedMappings() {
        return Stream.of(
            new AbstractMap.SimpleEntry<>("alb", "sqi"),
            new AbstractMap.SimpleEntry<>("arm", "hye"),
            new AbstractMap.SimpleEntry<>("baq", "eus"),
            new AbstractMap.SimpleEntry<>("bur", "mya"),
            new AbstractMap.SimpleEntry<>("chi", "zho"),
            new AbstractMap.SimpleEntry<>("cze", "ces"),
            new AbstractMap.SimpleEntry<>("dut", "nld"),
            new AbstractMap.SimpleEntry<>("fre", "fra"),
            new AbstractMap.SimpleEntry<>("geo", "kat"),
            new AbstractMap.SimpleEntry<>("ger", "deu"),
            new AbstractMap.SimpleEntry<>("gre", "ell"),
            new AbstractMap.SimpleEntry<>("ice", "isl"),
            new AbstractMap.SimpleEntry<>("mac", "mkd"),
            new AbstractMap.SimpleEntry<>("mao", "mri"),
            new AbstractMap.SimpleEntry<>("may", "msa"),
            new AbstractMap.SimpleEntry<>("per", "fas"),
            new AbstractMap.SimpleEntry<>("rum", "ron"),
            new AbstractMap.SimpleEntry<>("slo", "slk"),
            new AbstractMap.SimpleEntry<>("tib", "bod"),
            new AbstractMap.SimpleEntry<>("wel", "cym"))
                   .collect(Collectors.toUnmodifiableMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    private static Optional<String> toIso3Code(String languageCode) {
        try {
            return Optional.ofNullable(languageCode)
                       .filter(StringUtils::isNotBlank)
                       .map(LanguageMapper::returnHardCodedMappingIfExistsOrSameValue)
                       .map(Locale::new)
                       .map(Locale::getISO3Language);
        } catch (MissingResourceException e) {
            logger.warn(ERROR_MESSAGE_MISSING_RESOURCE_EXCEPTION + languageCode, e);
            return Optional.empty();
        }
    }

    private static String returnHardCodedMappingIfExistsOrSameValue(String code) {
        return IS02_TO_ISO3_CODES.getOrDefault(code, code);
    }
}
