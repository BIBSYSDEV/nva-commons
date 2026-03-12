package nva.commons.apigateway;

import java.util.Locale;
import java.util.Objects;

public final class MediaType {

    private static final String UTF_8 = "utf-8";
    private static final String APPLICATION = "application";
    private static final String TEXT = "text";

    public static final MediaType ANY_TYPE = create("*", "*");
    public static final MediaType ANY_APPLICATION_TYPE = create(APPLICATION, "*");
    public static final MediaType ANY_TEXT_TYPE = create(TEXT, "*");
    public static final MediaType CSV_UTF_8 = create(TEXT, "csv", UTF_8);
    public static final MediaType JSON_UTF_8 = create(APPLICATION, "json", UTF_8);
    public static final MediaType XML_UTF_8 = create(TEXT, "xml", UTF_8);
    public static final MediaType HTML_UTF_8 = create(TEXT, "html", UTF_8);
    public static final MediaType XHTML_UTF_8 = create(APPLICATION, "xhtml+xml", UTF_8);
    public static final MediaType MICROSOFT_EXCEL = create(APPLICATION, "vnd.ms-excel");
    public static final MediaType OOXML_SHEET =
        create(APPLICATION, "vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final String WILDCARD = "*";
    private static final String TYPE_SEPARATOR = "/";
    private static final String PARAMETER_SEPARATOR = ";";
    private static final String CHARSET_PREFIX = "charset=";
    private static final int TYPE_AND_SUBTYPE = 2;
    private static final int HAS_PARAMETERS = 2;

    private final String type;
    private final String subtype;
    private final String charset;

    private MediaType(String type, String subtype, String charset) {
        this.type = type;
        this.subtype = subtype;
        this.charset = charset;
    }

    public static MediaType create(String type, String subtype) {
        return new MediaType(type, subtype, null);
    }

    private static MediaType create(String type, String subtype, String charset) {
        return new MediaType(type, subtype, charset);
    }

    public static MediaType parse(String input) {
        var parts = input.strip().split(PARAMETER_SEPARATOR, TYPE_AND_SUBTYPE);
        var typeParts = parts[0].strip().split(TYPE_SEPARATOR, TYPE_AND_SUBTYPE);
        var parsedType = typeParts[0].strip();
        var parsedSubtype = typeParts.length > 1 ? typeParts[1].strip() : WILDCARD;
        String parsedCharset = null;
        if (parts.length >= HAS_PARAMETERS) {
            parsedCharset = extractCharset(parts[1].strip());
        }
        return new MediaType(parsedType, parsedSubtype, parsedCharset);
    }

    private static String extractCharset(String parameters) {
        for (var param : parameters.split(PARAMETER_SEPARATOR)) {
            var trimmed = param.strip().toLowerCase(Locale.ROOT);
            if (trimmed.startsWith(CHARSET_PREFIX)) {
                return param.strip().substring(CHARSET_PREFIX.length()).strip();
            }
        }
        return null;
    }

    public MediaType withoutParameters() {
        return new MediaType(type, subtype, null);
    }

    public boolean matches(MediaType other) {
        if (WILDCARD.equals(other.type)) {
            return true;
        }
        if (!type.equalsIgnoreCase(other.type)) {
            return false;
        }
        return WILDCARD.equals(other.subtype) || subtype.equalsIgnoreCase(other.subtype);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(type).append(TYPE_SEPARATOR).append(subtype);
        if (charset != null) {
            sb.append("; charset=").append(charset);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaType other)) {
            return false;
        }
        return type.equalsIgnoreCase(other.type)
               && subtype.equalsIgnoreCase(other.subtype)
               && Objects.equals(
                   charset != null ? charset.toLowerCase(Locale.ROOT) : null,
                   other.charset != null ? other.charset.toLowerCase(Locale.ROOT) : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            type.toLowerCase(Locale.ROOT),
            subtype.toLowerCase(Locale.ROOT),
            charset != null ? charset.toLowerCase(Locale.ROOT) : null);
    }
}
