package no.unit.nva.testutils;

import java.io.PrintWriter;
import java.io.StringWriter;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class ExceptionUtils {

    public static String stackTraceToString(Exception e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        e.printStackTrace(printWriter);
        return stringWriter.toString();
    }
}
