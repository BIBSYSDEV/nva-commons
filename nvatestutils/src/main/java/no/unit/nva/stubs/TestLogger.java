package no.unit.nva.stubs;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
@JacocoGenerated
public class TestLogger implements LambdaLogger {

    public final Logger logger = LoggerFactory.getLogger(TestLogger.class);

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void log(byte[] message) {
        logger.info(new String(message));
    }
}
