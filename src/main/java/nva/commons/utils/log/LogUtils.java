package nva.commons.utils.log;

import static java.util.Objects.nonNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Methods for testing logging when using Log4j2 as logging platform. Can be used when using SLF4J as well with Log4j2
 * as background logging platform.
 */
public final class LogUtils {

    public static final String PATTERN_ATTRIBUTE_NAME = "pattern";
    public static final String LOG_CLASS_NAME_AND_MESSAGE = "%c{1.}:  %msg";
    public static final String ROOT_NULL_APPENDER = "NullAppender";
    public static final String PACKAGES_WITH_HANDLERS = "nva.commons.utils.log";

    private LogUtils() {
    }

    /**
     * This method should ALWAYS be called before the creation of the object you want to test.
     *
     * @param clazz The class of the object under test.
     * @param <T>   The class of the object under test
     * @return a {@link TestAppender}
     */
    @SuppressWarnings("PMD.CloseResource")
    public static <T> TestAppender getTestingAppender(Class<T> clazz) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);

        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        avoidStatusMessagesWhileSettingUpConfiguration(builder);
        enableLog4jToFindOurAppender(builder);
        addTestAppenderToConfig(builder);
        silenceRoot(builder);
        addLoggerForTheInputClass(clazz, builder);

        Configurator.reconfigure(builder.build());

        return (TestAppender) createReferenceToTheAppenderInstanceForTestingAssertions(context);
    }

    private static Appender createReferenceToTheAppenderInstanceForTestingAssertions(LoggerContext context) {
        return context.getConfiguration().getAppender(TestAppender.APPENDER_NAME);
    }

    private static <T> void addLoggerForTheInputClass(Class<T> clazz,
                                                      ConfigurationBuilder<BuiltConfiguration> builder) {
        LoggerComponentBuilder loggerBuilder = builder.newLogger(clazz.getName(), Level.DEBUG);
        loggerBuilder.addAttribute("additivity", false);
        loggerBuilder.add(builder.newAppenderRef(TestAppender.APPENDER_NAME));
        builder.add(loggerBuilder);
    }

    private static void silenceRoot(ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.newAppender(ROOT_NULL_APPENDER, NullAppender.PLUGIN_NAME);
        RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.DEBUG);
        rootLogger.add(builder.newAppenderRef(ROOT_NULL_APPENDER));
        builder.add(rootLogger);
    }

    private static AppenderComponentBuilder addTestAppenderToConfig(ConfigurationBuilder<BuiltConfiguration> builder) {
        AppenderComponentBuilder appenderBuilder = builder.newAppender(TestAppender.APPENDER_NAME,
            TestAppender.PLUGIN_NAME);
        LayoutComponentBuilder classAndMessage = builder.newLayout("PatternLayout");
        classAndMessage.addAttribute(PATTERN_ATTRIBUTE_NAME, LOG_CLASS_NAME_AND_MESSAGE);
        appenderBuilder.add(classAndMessage);
        builder.add(appenderBuilder);
        return appenderBuilder;
    }

    private static void enableLog4jToFindOurAppender(ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setPackages(PACKAGES_WITH_HANDLERS);
    }

    private static ConfigurationBuilder<BuiltConfiguration> avoidStatusMessagesWhileSettingUpConfiguration(
        ConfigurationBuilder<BuiltConfiguration> builder) {
        return builder.setStatusLevel(Level.DEBUG);
    }

    /**
     * From LogManager.getLogger()
     *
     * @param cls the class
     * @return the logger name
     */
    public static String toLoggerName(final Class<?> cls) {
        final String canonicalName = cls.getCanonicalName();
        return nonNull(canonicalName) ? canonicalName : cls.getName();
    }
}
