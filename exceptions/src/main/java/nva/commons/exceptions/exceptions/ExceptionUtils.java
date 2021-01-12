package nva.commons.exceptions.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;
import nva.commons.commons.StringUtils;
import nva.commons.singletoncollector.SingletonCollector;

public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    /**
     * Returns the stacktrace in one line. It replaces all whitespaces with space and removes multiple whitespaces.
     *
     * @param e the Exception
     * @return the Stacktrace String.
     */
    public static String stackTraceInSingleLine(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionString = sw.toString();
        return Stream.of(exceptionString)
            .map(StringUtils::removeMultipleWhiteSpaces)
            .map(StringUtils::replaceWhiteSpacesWithSpace)
            .collect(SingletonCollector.collect());
    }
}
