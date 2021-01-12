package nva.commons.essentials;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class EnvironmentTest {

    private static final String EXISTING_ENV_VARIABLE = "EXISTING_NON_EMPTY_VARIABLE";
    private static final String EXISTING_EMPTY_VARIABLE = "EXISTING_EMPTY_VARIABLE";
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

    @Test
    @DisplayName("readEnvOpt returns an empty optional when env variable exists but it is blank")
    public void readEnvOptReturnsAnEmptyOptionalWhenEnvVariableExistsButItIsBlank() {
        Optional<String> value = environment.readEnvOpt(EXISTING_EMPTY_VARIABLE);
        assertTrue(value.isEmpty());
    }

    @Test
    @DisplayName("readEnv returns a String when an env variable exists and is not blank")
    public void readEnvReturnsAStringWhenAnEnvVariablexistsAndIsNotBlank() {
        String value = environment.readEnv(EXISTING_ENV_VARIABLE);
        assertNotNull(value);
    }

    @Test
    @DisplayName("readEnv throws Exception when the env variable does not exist")
    public void readEnvThrowsExceptionWhenTheEnvVariableDoesNotExist() {
        Executable action = () -> environment.readEnv(NON_EXISTING_ENV_VARIABLE);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), containsString(NON_EXISTING_ENV_VARIABLE));
    }

    @Test
    @DisplayName("readEnv throws Exception when the env variable is blank")
    public void readEnvThrowsExceptionWhenTheEnvVariableIsBlank() {
        Executable action = () -> environment.readEnv(EXISTING_EMPTY_VARIABLE);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), containsString(EXISTING_EMPTY_VARIABLE));
    }
}
