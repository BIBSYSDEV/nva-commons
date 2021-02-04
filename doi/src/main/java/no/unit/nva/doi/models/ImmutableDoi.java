package no.unit.nva.doi.models;

import static java.util.Objects.hash;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Immutable implementation of {@link Doi}.
 *
 * <p>Use the builder to create immutable instances: {@code ImmutableDoi.builder()}.
 */
@SuppressWarnings("PMD.UnnecessaryModifier") // methods with final.
public final class ImmutableDoi extends Doi {

    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PROXY = "proxy";
    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PREFIX = "prefix";
    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_SUFFIX = "suffix";
    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_IDENTIFIER = "identifier";
    public static final String MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_DOI = "doi";
    public static final String ERROR_DOI_URI_INVALID_FORMAT =
        "DOI does not look like a valid format following ".concat(DOI_URI_SYNTAX).concat(". It was: ");
    public static final String CANNOT_BUILD_DOI_PROXY_IS_NOT_A_VALID_PROXY =
        "Cannot build Doi, proxy is not a valid proxy.";
    public static final String CANNOT_BUILD_DOI_DOI_PREFIX_IS_NOT_VALID =
        "Cannot build Doi, prefix must start with ".concat(HANDLE_DOI_PREFIX).concat(" and contain some repository id");
    public static final String BUILDER_OF_DOI_IS_STRICT_ATTRIBUTE_IS_ALREADY_SET =
        "Builder of Doi is strict, attribute is already set: ";
    private final URI proxy;
    private final String prefix;
    private final String suffix;

    @SuppressWarnings("PMD.CallSuperInConstructor")
    private ImmutableDoi(ImmutableDoi.Builder builder) {
        this.prefix = builder.prefix;
        this.suffix = builder.suffix;
        this.proxy = builder.proxyIsSet()
            ? builder.proxy
            : requireNonNull(super.getProxy(), MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PROXY);
    }

    /**
     * Creates a builder for {@link ImmutableDoi ImmutableDoi}.
     * <pre>
     * ImmutableDoi.builder()
     *    .proxy(String) // optional {@link Doi#getProxy() proxy}
     *    .prefix(String) // required {@link Doi#getPrefix()} () prefix}
     *    .suffix(String) // required {@link Doi#getSuffix()} () suffix}
     *    .build();
     * </pre>
     *
     * @return A new ImmutableDoi builder
     */
    public static ImmutableDoi.Builder builder() {
        return new ImmutableDoi.Builder();
    }

    /**
     * Retrieve the value of the ${@code proxy} attribute.
     *
     * @return The value of the {@code proxy} attribute
     */
    @Override
    public URI getProxy() {
        return proxy;
    }

    /**
     * Retrieve the value of the ${@code prefix} attribute.
     *
     * @return The value of the {@code prefix} attribute
     */
    @Override
    public String getPrefix() {
        return prefix;
    }

    /**
     * Retrieve the value of the ${@code suffix} attribute.
     *
     * @return The value of the {@code suffix} attribute
     */
    @Override
    public String getSuffix() {
        return suffix;
    }

    /**
     * This instance is equal to all instances of {@code ImmutableDoi} that have equal attribute values.
     *
     * @return {@code true} if {@code this} is equal to {@code another} instance
     */
    @Override
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        return another instanceof ImmutableDoi
            && equalTo((ImmutableDoi) another);
    }

    /**
     * Computes a hash code from attributes: {@code proxy}, {@code prefix}, {@code suffix}.
     *
     * @return hashCode value
     */
    @Override
    public int hashCode() {
        return hash(proxy, prefix, suffix);
    }

    /**
     * Prints the immutable value {@code Doi} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
        return getPrefix() + PATH_SEPARATOR + getSuffix();
    }

    private static URI createDoiUriWithoutIdentifier(URI value) {
        try {
            return new URI(value.getScheme(),
                value.getUserInfo(),
                value.getHost(),
                value.getPort(),
                PATH_SEPARATOR_STRING,
                null, null);
        } catch (Exception e) {
            throw new IllegalStateException("Could not reconstruct URI and strip path, query and fragment arguments");
        }
    }

    private boolean equalTo(ImmutableDoi another) {
        return proxy.equals(another.proxy)
            && prefix.equals(another.prefix)
            && suffix.equals(another.suffix);
    }

    /**
     * Builds instances of type {@link ImmutableDoi ImmutableDoi}. Initialize attributes and then invoke the {@link
     * #build()} method to create an immutable instance.
     *
     * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
     * but instead used immediately to create instances.</em>
     */
    public static final class Builder {

        private URI proxy;
        private String prefix;
        private String suffix;

        private Builder() {
        }

        /**
         * Initializes the value for the {@link Doi#getProxy() proxy} attribute.
         *
         * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link
         * Doi#getProxy() proxy}.</em>
         *
         * @param proxy The value for proxy
         * @return {@code this} builder for use in a chained invocation
         */

        public final Builder withProxy(URI proxy) {
            checkNotIsSet(proxyIsSet(), "proxy");
            this.proxy = requireNonNull(proxy, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PROXY);
            return this;
        }

        /**
         * Initializes the value for the {@link Doi#getPrefix()} () prefix} attribute.
         *
         * @param prefix The value for prefix
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder withPrefix(String prefix) {
            checkNotIsSet(prefixIsSet(), "prefix");
            this.prefix = requireNonNull(prefix, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_PREFIX);
            return this;
        }

        /**
         * Initializes the value for the {@link Doi#getSuffix()}  suffix} attribute.
         *
         * @param suffix The value for suffix
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder withSuffix(String suffix) {
            checkNotIsSet(suffixIsSet(), "suffix");
            this.suffix = requireNonNull(suffix, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_SUFFIX);
            return this;
        }

        /**
         * Initializes the value for the {@link Doi#getPrefix()} and {@link Doi#getSuffix()} attributes.
         *
         * @param identifier The value (doi identifier: prefix/suffix) that can be parsed into prefix and suffix.
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder withIdentifier(String identifier) {
            requireNonNull(identifier, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_IDENTIFIER);
            int indexOfDivider = identifier.indexOf(PATH_SEPARATOR);
            if (indexOfDivider == -1) {
                throw new IllegalArgumentException("Invalid DOI identifier");
            }
            withPrefix(identifier.substring(0, indexOfDivider));
            withSuffix(identifier.substring(++indexOfDivider));
            return this;
        }

        /**
         * Initializes the value for {@link Doi#getProxy()}, {@link Doi#getPrefix()} and {@link Doi#getSuffix()}
         * attributes.
         *
         * @param doi Teh value (Doi Id: https://proxy/prefix/suffix) that can be parsed into proxy, prefix and suffix.
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder withDoi(URI doi) {
            requireNonNull(doi, MESSAGE_NON_NULL_ARGUMENT_FOR_PARAMETER_DOI);
            if (containsOnlyLeadingForwardSlashAndSlashBetweenPrefixAndSuffix(doi)) {
                throw new IllegalArgumentException(ERROR_DOI_URI_INVALID_FORMAT.concat(doi.toASCIIString()));
            }
            withIdentifier(extractDoiPathWithoutLeadingForwardSlash(doi));
            withProxy(doi);
            return this;
        }

        /**
         * Builds a new {@link ImmutableDoi ImmutableDoi}.
         *
         * @return An immutable instance of Doi
         * @throws java.lang.IllegalStateException if any required attributes are missing
         */
        public ImmutableDoi build() {
            if (hasPathInProxy()) {
                proxy = createDoiUriWithoutIdentifier(proxy);
            }
            checkRequiredAttributes();
            validateProxy();
            validatePrefix();
            return new ImmutableDoi(this);
        }

        public boolean isNotHypertextTransferProtocol() {
            return !VALID_SCHEMES.contains(proxy.getScheme().toLowerCase(Locale.US));
        }

        private static String extractDoiPathWithoutLeadingForwardSlash(URI doi) {
            return doi.getPath().charAt(0) == PATH_SEPARATOR ? doi.getPath().substring(1) : doi.getPath();
        }

        private static boolean containsOnlyLeadingForwardSlashAndSlashBetweenPrefixAndSuffix(URI doi) {
            return doi.getPath().chars().filter(ch -> ch == PATH_SEPARATOR).count() != 2;
        }

        private static void checkNotIsSet(boolean isSet, String name) {
            if (isSet) {
                throw new IllegalStateException(BUILDER_OF_DOI_IS_STRICT_ATTRIBUTE_IS_ALREADY_SET.concat(name));
            }
        }

        private boolean hasPathInProxy() {
            if (proxyIsSet()) {
                return isEmptyPathOrContainOnlyPathSeparator(proxy.getPath());
            }
            return false;
        }

        private boolean isEmptyPathOrContainOnlyPathSeparator(String proxyPath) {
            return proxyPath != null
                &&
                (proxyPath.length() > 1
                    || proxyPath.length() > 0 && proxyPath.charAt(0) != PATH_SEPARATOR);
        }

        private void validatePrefix() {
            if (!prefix.startsWith(HANDLE_DOI_PREFIX) || prefix.length() <= HANDLE_DOI_PREFIX.length()) {
                throw new IllegalStateException(CANNOT_BUILD_DOI_DOI_PREFIX_IS_NOT_VALID);
            }
        }

        private void validateProxy() {
            if (proxyIsSet() && (isNotHypertextTransferProtocol() || isNotValidProxy())) {
                throw new IllegalStateException(CANNOT_BUILD_DOI_PROXY_IS_NOT_A_VALID_PROXY);
            }
        }

        private boolean isNotValidProxy() {
            return !VALID_PROXIES.contains(proxy.getHost().toLowerCase(Locale.US));
        }

        private boolean proxyIsSet() {
            return nonNull(proxy);
        }

        private boolean prefixIsSet() {
            return nonNull(prefix);
        }

        private boolean suffixIsSet() {
            return nonNull(suffix);
        }

        private void checkRequiredAttributes() {
            if (!prefixIsSet() || !suffixIsSet()) {
                throw new IllegalStateException(formatRequiredAttributesMessage());
            }
        }

        private String formatRequiredAttributesMessage() {
            List<String> attributes = new ArrayList<>();
            if (!prefixIsSet()) {
                attributes.add("prefix");
            }
            if (!suffixIsSet()) {
                attributes.add("suffix");
            }
            return "Cannot build Doi, some of required attributes are not set " + attributes;
        }
    }
}
