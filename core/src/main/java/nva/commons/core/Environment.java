package nva.commons.core;

import java.util.Optional;

public class Environment {

    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";

    /**
     * Read an Environment variable.
     *
     * @param variableName the Env variable name.
     * @return the value of the variable or an empty Optional if the variable does not exists.
     */
    public Optional<String> readEnvOpt(String variableName) {
        return Optional.ofNullable(System.getenv().get(variableName)).filter(value -> !value.isBlank());
    }

    /**
     * Read an Environment variable.
     *
     * @param variableName the Env variable name.
     * @return the value of the variable or throw {@IllegalStateException} if the variable does not exists.
     */
    public String readEnv(String variableName) {
        return readEnvOpt(variableName).orElseThrow(() -> variableNotSetException(variableName));
    }

    private IllegalStateException variableNotSetException(String variableName) {
        return new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + variableName);
    }
}

