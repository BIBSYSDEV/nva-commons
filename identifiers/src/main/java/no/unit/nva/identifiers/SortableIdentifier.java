package no.unit.nva.identifiers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import nva.commons.core.StringUtils;

/**
 * Generates ids of the form "0176f264a5ad-446893d8-3c02-4f64-936b-2997fec34e98". where the first part is an Instant
 * encoded in 12 hex digits (enough until the year 10889) and the rest is a UUID. The timestamp is should be used only
 * for sorting and not for identifying creation date.
 */
@JsonSerialize(using = SortableIdentifierSerializer.class)
@JsonDeserialize(using = SortableIdentifierDeserializer.class)
public final class SortableIdentifier implements Comparable<SortableIdentifier> {

    public static final int TIMESTAMP_LENGTH = 12;

    public static final String PATH_DELIMITER = "/";
    public static final String INVALID_URI_ERROR_MESSAGE = "The URI %s does not contain a valid Sortable identifier:";
    public static final String BLANK_IDENTIFIER_ERROR = "Identifier string cannot be null or blank";
    private static final String IDENTIFIER_FORMATTING = "%0" + TIMESTAMP_LENGTH + "x-%s";
    private final String identifier;

    //User the static method fromString
    @Deprecated
    public SortableIdentifier(String identifier) {
        validate(identifier);
        this.identifier = identifier;
    }

    public static SortableIdentifier next() {
        return new SortableIdentifier(newIdentifierString());
    }

    public static SortableIdentifier fromUri(URI uri) {
        try {
            String path = uri.getPath();
            String identifierString = path.substring(path.lastIndexOf(PATH_DELIMITER) + 1);
            return new SortableIdentifier(identifierString);
        } catch (Exception exception) {
            throw invalidUriError(uri);
        }
    }

    public static SortableIdentifier fromString(String identifier) {
        return new SortableIdentifier(identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SortableIdentifier that = (SortableIdentifier) o;
        return Objects.equals(toString(), that.toString());
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public int compareTo(SortableIdentifier o) {
        return this.toString().compareTo(o.toString());
    }

    private static IllegalArgumentException invalidUriError(URI uri) {
        return new IllegalArgumentException(String.format(INVALID_URI_ERROR_MESSAGE, uri.toString()));
    }

    private static String newIdentifierString() {
        return String.format(IDENTIFIER_FORMATTING, System.currentTimeMillis(), UUID.randomUUID());
    }

    private void validate(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException(BLANK_IDENTIFIER_ERROR);
        }
    }
}
