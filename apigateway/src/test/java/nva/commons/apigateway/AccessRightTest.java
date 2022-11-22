package nva.commons.apigateway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.exceptions.InvalidAccessRightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class AccessRightTest {

    public static final String APPROVE_DOI_REQUEST_STRING = "\"APPROVE_DOI_REQUEST\"";

    @Test
    void fromStringParsesStringCaseInsensitive() {
        String variableCase = "apProve_DOi_reQueST";
        AccessRight actualValue = AccessRight.fromString(variableCase);
        assertThat(actualValue, is(equalTo(AccessRight.APPROVE_DOI_REQUEST)));
    }

    @Test
    void fromStringThrowsExceptionWhenInputIsInvalidAccessRight() {
        String invalidAccessRight = "invalidAccessRight";
        Executable action = () -> AccessRight.fromString(invalidAccessRight);
        InvalidAccessRightException exception = assertThrows(InvalidAccessRightException.class, action);
        assertThat(exception.getMessage(), containsString(invalidAccessRight));
    }

    @Test
    void accessRightIsSerializedUpperCase() throws IOException {
        String value = JsonUtils.dtoObjectMapper.writeValueAsString(AccessRight.APPROVE_DOI_REQUEST);
        assertThat(value, is(equalTo(APPROVE_DOI_REQUEST_STRING)));
    }

    @Test
    void accessRightIsDeserializedFromString() throws IOException {
        AccessRight accessRight = JsonUtils.dtoObjectMapper.readValue(APPROVE_DOI_REQUEST_STRING, AccessRight.class);
        assertThat(accessRight, is(equalTo(AccessRight.APPROVE_DOI_REQUEST)));
    }
}