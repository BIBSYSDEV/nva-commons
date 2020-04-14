package nva.commons.utils;

import static java.util.Objects.isNull;

import java.util.Optional;

public class Environment {

    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";

    /**
     * Read an Environment variable.
     *
     * @param variableName the Env variable name.
     * @return the value of the variable or an empty Optional if the variable does not exists.
     */
    @JacocoGenerated
    public Optional<String> readEnvOpt(String variableName) {
        return Optional.ofNullable(System.getenv().get(variableName)).filter(value -> !value.isEmpty());
    }

    /**
     * Read an Environment variable.
     *
     * @param variableName the Env variable name.
     * @return the value of the variable or throw {@IllegalStateException} if the variable does not exists.
     */
    @JacocoGenerated
    public String readEnv(String variableName) {
        String value = System.getenv().get(variableName);
        if (isNull(value)) {
            throw new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + variableName);
        }
        return value;
    }
}

