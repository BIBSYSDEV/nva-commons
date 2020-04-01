package nva.commons.testutils;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.nio.charset.StandardCharsets;

public class TestLogger implements LambdaLogger {

    StringBuilder logs= new StringBuilder();

    @Override
    public void log(String message) {
        logs.append(message);
        logs.append(System.lineSeparator());
    }

    @Override
    public void log(byte[] message) {
        log(new String(message, StandardCharsets.UTF_8));
    }
}
