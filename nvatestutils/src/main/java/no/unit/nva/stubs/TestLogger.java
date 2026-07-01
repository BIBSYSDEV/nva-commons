package no.unit.nva.stubs;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.nio.charset.StandardCharsets;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
@JacocoGenerated
public class TestLogger implements LambdaLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestLogger.class);

  @Override
  public void log(String message) {
    LOGGER.info(message);
  }

  @Override
  public void log(byte[] message) {
    LOGGER.info(new String(message, StandardCharsets.UTF_8));
  }
}
