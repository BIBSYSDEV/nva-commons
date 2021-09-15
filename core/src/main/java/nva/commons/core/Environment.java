package nva.commons.core;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Environment {

    private static final Logger logger = LoggerFactory.getLogger(Environment.class);
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
     * @return the value of the variable or throw IllegalStateException if the variable does not exists.
     */
    public String readEnv(String variableName) {
        return readEnvOpt(variableName).orElseThrow(() -> variableNotSetException(variableName));
    }

    private IllegalStateException variableNotSetException(String variableName) {
        String message = ENVIRONMENT_VARIABLE_NOT_SET + variableName;
        logger.error(message);
        return new IllegalStateException(message);
    }
}

