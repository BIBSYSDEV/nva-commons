package nva.commons.core.useragent;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

/**
 * When sending HTTP requests, applying a User-Agent header allows the recipient to know who is executing the request.
 */
public final class UserAgent {

    /**
     * For convenience, we provide the header.
     */
    public static final String USER_AGENT = "User-Agent";
    private final String userAgentString;

    private UserAgent(String userAgentString) {
        this.userAgentString = userAgentString;
    }

    /**
     * Creates a new UserAgentBuilder.
     * @return a UserAgentBuilder.
     */
    public static UserAgentBuilder newBuilder() {
        return new UserAgentBuilder();
    }

    /**
     * Creates the actual string value.
     * @return The User-Agent string.
     */
    @Override
    public String toString() {
        return userAgentString;
    }

    /**
     * Allows the building of a valid UserAgent string.
     */
    public static final class UserAgentBuilder {
        public static final String USER_AGENT_TEMPLATE = "%s-%s/%s (%s; mailto:%s)";
        public static final String NULL_VALUE_ERROR = "No value for user agent builder may be null";
        private String clientName;
        private String versionName;
        private URI uri;
        private String emailAddress;
        private String environmentName;

        private UserAgentBuilder() {
            // NO-OP
        }

        /**
         * The class that is calling, typically the Handler class that initiates the call.
         * @param aClass The root caller class that initiates the call.
         * @return UserAgentBuilder
         */
        public UserAgentBuilder client(Class<?> aClass) {
            this.clientName = aClass.getSimpleName();
            return this;
        }

        /**
         * The version of Handler class that initiates the call.
         * @param version The version string.
         * @return UserAgentBuilder
         */
        public UserAgentBuilder version(String version) {
            this.versionName = version;
            return this;
        }

        /**
         * The repository for the code of the calling class.
         * @param uri The URI of the repository.
         * @return UserAgentBuilder
         */
        public UserAgentBuilder repository(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * The contact email (typically support@somewhere) that recipients can contact in case of an issue.
         * @param email The root caller class that initiates the call.
         * @return UserAgentBuilder
         */
        public UserAgentBuilder email(String email) {
            this.emailAddress = email;
            return this;
        }

        /**
         * The environment that is calling, typically dev, test, sandbox, e2e or prod.
         * @param environment The environment.
         * @return UserAgentBuilder
         */
        public UserAgentBuilder environment(String environment) {
            this.environmentName = environment;
            return this;
        }

        /**
         * Builds the UserAgent.
         * @return UserAgent.
         */
        public UserAgent build() {
            validate(uri, clientName, environmentName, versionName, uri, emailAddress);
            var userAgentString = String.format(USER_AGENT_TEMPLATE,
                                                clientName,
                                                environmentName,
                                                versionName,
                                                uri,
                                                emailAddress);
            return new UserAgent(userAgentString);
        }

        private void validate(Object... objects) {
            if (Arrays.stream(objects).anyMatch(Objects::isNull)) {
                throw new InvalidUserAgentException(NULL_VALUE_ERROR);
            }
        }
    }
}
