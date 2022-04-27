package nva.commons.apigateway;

import static java.util.function.Predicate.not;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;

public class PersonGroup {

    public static final int ACCESS_RIGHT_INDEX = 0;
    public static final String USER_AT_CUSTOMER_GROUP = "USER";
    public static final String ENTRIIES_DELIMITER = ",";
    private static final String AT = "@";
    private static final int CUSTOMER_ID_INDEX = 1;
    private final String group;
    private final URI customerId;

    public PersonGroup(String right, URI customerId) {
        this.group = readAccessRightDiscardingPossibleCustomerId(right);
        this.customerId = customerId;
    }

    public static PersonGroup fromString(String input) {
        var list = input.split(AT);
        var accessRight = formatAccessRightString(list[ACCESS_RIGHT_INDEX]);
        var customerId = URI.create(list[CUSTOMER_ID_INDEX]);
        return new PersonGroup(accessRight, customerId);
    }

    public static Stream<PersonGroup> fromCsv(String csv) {
        return Arrays.stream(csv.split(ENTRIIES_DELIMITER))
            .filter(not(String::isBlank))
            .map(PersonGroup::fromString);
    }

    public static PersonGroup createUserAtCustomerGroup(URI customerId) {
        return new PersonGroup(USER_AT_CUSTOMER_GROUP, customerId);
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public String getGroup() {
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
        if (!(o instanceof PersonGroup)) {
            return false;
        }
        PersonGroup that = (PersonGroup) o;
        return Objects.equals(group, that.group) &&
               Objects.equals(customerId, that.customerId);
    }

    @Override
    public String toString() {
        return this.group + AT + customerId.toString();
    }

    public boolean describesCustomerUponLogin() {
        return USER_AT_CUSTOMER_GROUP.equalsIgnoreCase(this.getGroup());
    }

    private static String formatAccessRightString(String s) {
        return s.toLowerCase(Locale.getDefault());
    }

    private String readAccessRightDiscardingPossibleCustomerId(String right) {
        var accessRight = right.split(AT)[ACCESS_RIGHT_INDEX];
        return formatAccessRightString(accessRight);
    }
}
