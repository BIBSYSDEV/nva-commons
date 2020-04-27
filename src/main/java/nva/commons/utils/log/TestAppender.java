package nva.commons.utils.log;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

@SuppressWarnings("PMD.AvoidStringBufferField")
@Plugin(name = TestAppender.PLUGIN_NAME, category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE,
    printObject = true)
public class TestAppender extends AbstractAppender {

    public static final String PLUGIN_NAME = "TestAppender";
    public static final String APPENDER_NAME = "Test";

    private final StringBuilder stringBuffer = new StringBuilder();

    /**
     * Builder class that follows log4j2 plugin convention.
     *
     * @param <B> Generic Builder class.
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<TestAppender> {

        /**
         * creates a new TestAppender.
         *
         * @return a new TestAppender
         */
        @Override
        public TestAppender build() {
            return new TestAppender(super.getName(), super.getFilter(), super.getOrCreateLayout(),
                super.isIgnoreExceptions());
        }
    }

    /**
     * Method used by log4j2 to access this appender.
     *
     * @param <B> Generic Builder class.
     * @return LambdaAppender Builder.
     */
    @PluginBuilderFactory
    public static <B extends TestAppender.Builder<B>> B newBuilder() {
        return new TestAppender.Builder<B>().asBuilder();
    }

    /**
     * Constructor method following AbstractAppender convention.
     *
     * @param name             name of appender.
     * @param filter           filter specified in xml.
     * @param layout           layout specified in xml.
     * @param ignoreExceptions whether to show exceptions or not specified in xml.
     */
    public TestAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }

    /**
     * Append log to StringBuilder.
     *
     * @param event log4j event
     */
    @Override
    public void append(LogEvent event) {
        byte[] bytes = super.getLayout().toByteArray(event);
        String logLine = new String(bytes, StandardCharsets.UTF_8);
        stringBuffer.append(logLine);
    }

    /**
     * Get the log messages.
     *
     * @return the messages.
     */
    public String getMessages() {
        return stringBuffer.toString();
    }
}
