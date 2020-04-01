package nva.commons.utils;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.nio.charset.StandardCharsets;

/**
 * Logger class to be used in testing for testing logging. Examples in test class.
 */
@SuppressWarnings("PMD.AvoidStringBufferField")
public class TestLogger implements LambdaLogger {

    private final StringBuilder logs = new StringBuilder();

    @Override
    public void log(String message) {
        logs.append(message);
        logs.append(System.lineSeparator());
    }

    @Override
    public void log(byte[] message) {
        log(new String(message, StandardCharsets.UTF_8));
    }

    public String getLogs() {
        return logs.toString();
    }
}
