package nva.commons.utils;

import java.util.Objects;
import java.util.Optional;

public class Environment {

    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";


    public Optional<String> readEnvOpt(String variableName) {
        return Optional.ofNullable(System.getenv().get(variableName)).filter(value -> !value.isEmpty());
    }

    public String readEnv(String variableName) {
        String value = System.getenv().get(variableName);
        Objects.requireNonNull(value, ENVIRONMENT_VARIABLE_NOT_SET + variableName);
        return value;
    }
}

