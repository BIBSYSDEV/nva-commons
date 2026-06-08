package nva.commons.logutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnotherDummyClassForLogTesting {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AnotherDummyClassForLogTesting.class);

  public void logInfo(String message) {
    LOGGER.info(message);
  }

  public void logError(String message, Throwable cause) {
    LOGGER.error(message, cause);
  }
}
