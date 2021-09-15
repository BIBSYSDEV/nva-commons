package nva.commons.apigateway;

import java.util.Locale;

public final class ContentTypes {

    public static final String APPLICATION_XML = "application/xml".toLowerCase(Locale.ROOT);
    public static final String WILDCARD = "*/*".toLowerCase(Locale.ROOT);
    public static final String APPLICATION_JSON = "application/json".toLowerCase(Locale.ROOT);
    public static final String APPLICATION_JSON_LD = "application/ld+json".toLowerCase(Locale.ROOT);

    private ContentTypes() {
    }
}
