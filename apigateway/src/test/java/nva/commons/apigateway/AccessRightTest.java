package nva.commons.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import nva.commons.apigateway.exceptions.InvalidAccessRightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AccessRightTest {

    @Test
    void fromStringThrowsExceptionWhenInputIsInvalidAccessRight() {
        String invalidAccessRight = "invalidAccessRight";
        Executable action = () -> AccessRight.fromPersistedString(invalidAccessRight);
        InvalidAccessRightException exception = assertThrows(InvalidAccessRightException.class, action);
        assertThat(exception.getMessage(), containsString(invalidAccessRight));
    }

    @ParameterizedTest
    @EnumSource(AccessRight.class)
    void shouldPersistBackAndForthToSameEnum(AccessRight accessRight) {
        var string = accessRight.toPersistedString();
        var backAsEnum = AccessRight.fromPersistedString(string);

        assertThat(backAsEnum, is(equalTo(accessRight)));

    }
}