package nva.commons.apigateway;

import static java.util.function.Predicate.not;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;

public class AccessRightEntry {

    public static final int ACCESS_RIGHT_INDEX = 0;

    public static final String ENTRIIES_DELIMITER = ",";
    private static final String AT = "@";
    private static final int CUSTOMER_ID_INDEX = 1;
    private final AccessRight accessRight;
    private final URI customerId;

    public AccessRightEntry(AccessRight accessRight, URI customerId) {
        this.accessRight = accessRight;
        this.customerId = customerId;
    }

    public static AccessRightEntry fromString(String accessRightAtCustomer) {
        var list = accessRightAtCustomer.split(AT);
        var accessRight = AccessRight.fromPersistedString(list[ACCESS_RIGHT_INDEX]);
        var customerId = URI.create(list[CUSTOMER_ID_INDEX]);
        return new AccessRightEntry(accessRight, customerId);
    }

    public static AccessRightEntry fromStringForCustomer(String accessRightAtCustomer, URI customerId) {
        var list = accessRightAtCustomer.split(AT);
        var accessRight = AccessRight.fromPersistedString(list[ACCESS_RIGHT_INDEX]);
        return new AccessRightEntry(accessRight, customerId);
    }

    public static Stream<AccessRightEntry> fromCsv(String csv) {
        return Arrays.stream(csv.split(ENTRIIES_DELIMITER))
            .filter(not(String::isBlank))
            .map(AccessRightEntry::fromString);
    }

    public static Stream<AccessRightEntry> fromCsvForCustomer(String csv, URI customerId) {
        return Arrays.stream(csv.split(ENTRIIES_DELIMITER))
                   .filter(not(String::isBlank))
                   .map(accessRightEntryStr -> fromStringForCustomer(accessRightEntryStr, customerId));
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public AccessRight getAccessRight() {
        return accessRight;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(accessRight, customerId);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessRightEntry)) {
            return false;
        }
        AccessRightEntry that = (AccessRightEntry) o;
        return Objects.equals(accessRight, that.accessRight)
               && Objects.equals(customerId, that.customerId);
    }

    @Override
    public String toString() {
        return this.accessRight.toPersistedString() + AT + customerId.toString();
    }
}
