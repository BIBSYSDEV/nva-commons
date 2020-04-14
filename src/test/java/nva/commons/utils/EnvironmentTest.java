package nva.commons.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EnvironmentTest {

    private static final String EXISTING_ENV_VARIABLE = "PATH";
    private static final String NON_EXISTING_ENV_VARIABLE = "SOMETHING_ELSE";

    private Environment environment = new Environment();

    @Test
    @DisplayName("readEnvOpt returns an empty optional when env variable does not exist")
    public void readEnvOptShouldReturnAnEmptyOptionalForNonExistingEnvVariable() {
        Optional<String> value = environment.readEnvOpt(NON_EXISTING_ENV_VARIABLE);
        assertTrue(value.isEmpty());
    }

    @Test
    @DisplayName("readEnvOpt returns an non empty optional when env variable  exists")
    public void readEnvOptShouldReturnAnNonEmptyOptionalForExistingEnvVariable() {
        Optional<String> value = environment.readEnvOpt(EXISTING_ENV_VARIABLE);
        assertTrue(value.isPresent());
    }
}
