package nva.commons.apigateway;

import static java.util.function.Predicate.not;
import static nva.commons.apigateway.AccessRight.USER;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;

public class AccessRightEntry {

    public static final int ACCESS_RIGHT_INDEX = 0;

    public static final String ENTRIIES_DELIMITER = ",";
    private static final String AT = "@";
    private static final int CUSTOMER_ID_INDEX = 1;
    private final String group;
    private final URI customerId;

    public AccessRightEntry(String right, URI customerId) {
        this.group = formatAccessRightString(right.split(AT)[ACCESS_RIGHT_INDEX]);
        this.customerId = customerId;
    }

    public static AccessRightEntry fromString(String accessRightAtCustomer) {
        var list = accessRightAtCustomer.split(AT);
        var accessRight = formatAccessRightString(list[ACCESS_RIGHT_INDEX]);
        var customerId = URI.create(list[CUSTOMER_ID_INDEX]);
        return new AccessRightEntry(accessRight, customerId);
    }

    public static Stream<AccessRightEntry> fromCsv(String csv) {
        return Arrays.stream(csv.split(ENTRIIES_DELIMITER))
            .filter(not(String::isBlank))
            .map(AccessRightEntry::fromString);
    }

    public static AccessRightEntry createUserAtCustomerGroup(URI customerId) {
        return new AccessRightEntry(USER.toPersistedString(), customerId);
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public String getAccessRight() {
        return group;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(group, customerId);
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
        return Objects.equals(group, that.group)
               && Objects.equals(customerId, that.customerId);
    }

    @Override
    public String toString() {
        return this.group + AT + customerId.toString();
    }

    public boolean describesCustomerUponLogin() {
        return USER.toPersistedString().equalsIgnoreCase(this.getAccessRight());
    }

    private static String formatAccessRightString(String accessRightWithoutCustomerId) {
        return accessRightWithoutCustomerId.toLowerCase(Locale.getDefault());
    }
}
