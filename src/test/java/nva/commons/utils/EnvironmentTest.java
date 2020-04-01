package nva.commons.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class EnvironmentTest {

    private static final  String EXISTING_ENV_VARIABLE = "PATH";
    private static final  String NON_EXISTING_ENV_VARIABLE = "SOMETHING_ELSE";

    private Environment environment = new Environment();

    @Test
    public void readEnvOptShouldReturnAnEmptyOptionalForNonExistingEnvVariable() {
        Optional<String> value = environment.readEnvOpt(NON_EXISTING_ENV_VARIABLE);
        assertTrue(value.isEmpty());
    }

    @Test
    public void readEnvOptShouldReturnAnOptionalForNonExistingEnvVariable() {
        Optional<String> value = environment.readEnvOpt(NON_EXISTING_ENV_VARIABLE);
        assertTrue(value.isEmpty());
    }
}
