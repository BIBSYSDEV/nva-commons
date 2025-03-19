package no.unit.nva.clients.cristin;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("Person")
public record CristinPersonDto(URI id, Set<TypedValue> identifiers, Set<TypedValue> names,
                               Set<Affiliation> affiliations, boolean verified) {

    public static final String FIRST_NAME = "FirstName";
    public static final String LAST_NAME = "LastName";
    public static final String FULL_NAME_PATTERN = "%s %s";

    public Optional<String> firstName() {
        return names().stream().filter(CristinPersonDto::isFirstName).map(TypedValue::value).findFirst();
    }

    public Optional<String> lastName() {
        return names().stream().filter(CristinPersonDto::isLastName).map(TypedValue::value).findFirst();
    }

    public String fullName() {
        var firstName = firstName().orElse(null);
        var lastName = lastName().orElse(null);
        return FULL_NAME_PATTERN.formatted(firstName, lastName);
    }

    private static boolean isFirstName(TypedValue typedValue) {
        return FIRST_NAME.equals(typedValue.type());
    }

    private static boolean isLastName(TypedValue typedValue) {
        return LAST_NAME.equals(typedValue.type());
    }
}
