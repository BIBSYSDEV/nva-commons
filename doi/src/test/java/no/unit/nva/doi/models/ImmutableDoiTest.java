package no.unit.nva.doi.models;

import static no.unit.nva.doi.models.Doi.DEFAULT_DOI_PROXY;
import static no.unit.nva.doi.models.Doi.PATH_SEPARATOR;
import static no.unit.nva.doi.models.Doi.PATH_SEPARATOR_STRING;
import static no.unit.nva.doi.models.Doi.SCHEMA_SEPARATOR;
import static no.unit.nva.doi.models.Doi.VALID_PROXIES;
import static no.unit.nva.doi.models.Doi.VALID_SCHEMES;
import static no.unit.nva.doi.models.ImmutableDoi.CANNOT_BUILD_DOI_DOI_PREFIX_IS_NOT_VALID;
import static no.unit.nva.doi.models.ImmutableDoi.CANNOT_BUILD_DOI_PROXY_IS_NOT_A_VALID_PROXY;
import static no.unit.nva.doi.models.ImmutableDoi.ERROR_DOI_URI_INVALID_FORMAT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGeneratorNvaTest.ReplaceCamelCase.class)
class ImmutableDoiTest {

    public static final String NAME_FORMAT = "[{index}]  {displayName} with input: \"{0}\"";

    public static final URI DOI_PROXY = URI.create("https://doi.org/");
    public static final String EXAMPLE_SUFFIX = createRandomSuffix();
    public static final String DOI_IDENTIFIER_SEPARATOR = "/";
    public static final String REQUIRED_ATTRIBUTES_ARE_NOT_SET = "required attributes are not set";
    public static final String ERROR_STRICT_BUILDER = "Builder of Doi is strict, attribute is already set";
    public static final String URI_VALID_EMAILTO_BUT_INVALID_URL = "emailto:nope@example.net";
    public static final URI INVALID_PROXY_FTP = URI.create("ftp://doi.org/");
    public static final URI INVALID_PROXY_EXAMPLE_DOT_NET = URI.create("https://example.net");
    private static final URI STAGE_DOI_PROXY = URI.create("https://handle.stage.datacite.org/");
    private static final String DEMO_PREFIX = "10.5072";
    public static final String EXAMPLE_PREFIX = DEMO_PREFIX;
    public static final String EXAMPLE_IDENTIFIER = EXAMPLE_PREFIX + DOI_IDENTIFIER_SEPARATOR + EXAMPLE_SUFFIX;
    private static final URI EXAMPLE_PROXY = STAGE_DOI_PROXY;
    public static final URI EXAMPLE_DOI = URI.create(EXAMPLE_PROXY + EXAMPLE_IDENTIFIER);
    private static final String EXAMPLE_PREFIX_2 = "10.16903";
    private static final URI INVALID_PROXY = URI.create("https://doiproxy.invalid/");

    @Test
    void getProxyReturnsDefaultProxyWhenNotSpecified() {
        var doi = Doi.builder().withPrefix(EXAMPLE_PREFIX).withSuffix(EXAMPLE_SUFFIX).build();
        assertThat(doi.getProxy(), is(equalTo(DOI_PROXY)));
    }

    @Test
    void toStringReturnsPrefixAndSuffixAkaIdentifier() {
        assertThat(createDoi().toString(), is(equalTo(EXAMPLE_IDENTIFIER)));
    }

    @Test
    void hashCodeReturnsSameHashCodeForTwoDifferentInstancesButIdenticalObject() {
        var doi = createDoi();
        var doi2 = createDoi();

        assertThat(doi.hashCode(), is(equalTo(doi2.hashCode())));
    }

    @Test
    void equalsReturnsTrueForTwoDifferentInstancesButIdenticalObjects() {
        var doi = createDoi();
        var doi2 = createDoi();

        assertThat(doi, is(equalTo(doi2)));
        assertThat(doi, is(equalTo(doi)));
    }

    @Test
    void toIdentifierReturnsPrefixForwardSlashAndSuffix() {
        String randomSuffix = createRandomSuffix();
        Doi doi = createDoi(randomSuffix);
        assertThat(doi.toIdentifier(), is(equalTo(EXAMPLE_PREFIX + PATH_SEPARATOR_STRING + randomSuffix)));
    }

    @Test
    void toUriReturnsDoiWhichIsProxyPrefixAndSuffixAsUri() {
        String randomSuffix = createRandomSuffix();
        var doi = createDoi(randomSuffix);
        URI expectedUri = URI.create(EXAMPLE_PROXY + EXAMPLE_PREFIX + DOI_IDENTIFIER_SEPARATOR + randomSuffix);
        assertThat(doi.toUri(), is(equalTo(expectedUri)));
    }

    @Test
    void toUriReturnsDoiWhereBuilderWithProxyHasNoPath() {
        var randomSuffix = createRandomSuffix();
        var doi = ImmutableDoi.builder()
            .withProxy(URI.create("https://doi.org"))
            .withPrefix(DEMO_PREFIX)
            .withSuffix(randomSuffix)
            .build();
        assertThat(doi.toUri(),
            is(equalTo(URI.create(DEFAULT_DOI_PROXY + DEMO_PREFIX + DOI_IDENTIFIER_SEPARATOR + randomSuffix))));
    }

    @Test
    void builderBuildReturnsDoiWhenWithIdentifierPopulatesPrefixAndSuffix() {
        Doi doi = ImmutableDoi.builder().withIdentifier(EXAMPLE_IDENTIFIER).build();
        assertThat(doi.getPrefix(), is(equalTo(EXAMPLE_PREFIX)));
        assertThat(doi.getSuffix(), is(equalTo(EXAMPLE_SUFFIX)));
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenWithProxyContainsInvalidUrlAsUri() {
        var actualException = assertThrows(IllegalStateException.class, () ->
            ImmutableDoi.builder()
                .withProxy(URI.create(URI_VALID_EMAILTO_BUT_INVALID_URL))
                .withIdentifier(EXAMPLE_IDENTIFIER)
                .build());
        assertThat(actualException.getMessage(), is(equalTo(CANNOT_BUILD_DOI_PROXY_IS_NOT_A_VALID_PROXY)));
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenMissingSuffix() {
        var actual = assertThrows(IllegalStateException.class,
            () -> ImmutableDoi.builder().withPrefix(EXAMPLE_PREFIX).build());
        assertThat(actual.getMessage(), containsString(REQUIRED_ATTRIBUTES_ARE_NOT_SET));
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenMissingPrefix() {
        var actual = assertThrows(IllegalStateException.class,
            () -> ImmutableDoi.builder()
                .withSuffix(createRandomSuffix()).build());
        assertThat(actual.getMessage(), containsString(REQUIRED_ATTRIBUTES_ARE_NOT_SET));
    }

    @Test
    void builderBuildThrowsNullPointerExceptionWhenWithIdentifierIsNull() {
        assertThrows(NullPointerException.class, () -> ImmutableDoi.builder().withIdentifier(null).build());
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenWithIdentifierIsMissingSuffix() {
        assertThrows(IllegalArgumentException.class,
            () -> ImmutableDoi.builder().withIdentifier(EXAMPLE_PREFIX).build());
    }

    @Test
    void builderBuildReturnDoiWithDoiPopulatesProxyPrefixAndSuffix() {
        var doi = ImmutableDoi.builder().withDoi(EXAMPLE_DOI).build();
        assertThat(doi.getProxy(), is(equalTo(EXAMPLE_PROXY)));
        assertThat(doi.getPrefix(), is(equalTo(EXAMPLE_PREFIX)));
        assertThat(doi.getSuffix(), is(equalTo(EXAMPLE_SUFFIX)));
    }

    @ParameterizedTest(name = NAME_FORMAT)
    @MethodSource("validSchemesAndProxyHosts")
    @DisplayName("builder.build() returns Doi when builder.withProxy(arg) contains valid combination of scheme and "
        + "proxyHost and identifier")
    void builderBuildReturnDoiWhenWithProxyInputContainsValidCombinationsOfSchemeAndProxyAndValidIdentifier(
        String scheme,
        String proxyHost) {
        var doi = ImmutableDoi.builder()
            .withProxy(URI.create(scheme + SCHEMA_SEPARATOR + proxyHost))
            .withIdentifier(EXAMPLE_IDENTIFIER)
            .build();
        assertThat(doi.getProxy().getScheme(), is(equalTo(scheme)));
        assertThat(doi.getProxy().getHost(), is(equalTo(proxyHost)));
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenWithDoiUriContainsInvalidIdentifierInPath() {
        URI invalidDoi = URI.create(
            EXAMPLE_PROXY + EXAMPLE_PREFIX + DOI_IDENTIFIER_SEPARATOR + EXAMPLE_SUFFIX + DOI_IDENTIFIER_SEPARATOR
                + createRandomUuid());
        var actualException = assertThrows(IllegalArgumentException.class,
            () -> ImmutableDoi.builder().withDoi(invalidDoi));
        assertThat(actualException.getMessage(),
            is(equalTo(ERROR_DOI_URI_INVALID_FORMAT.concat(invalidDoi.toASCIIString()))));
    }

    @ParameterizedTest(name = NAME_FORMAT)
    @MethodSource("badPrefixes")
    @DisplayName("builder.build() throws IllegalStateException when builder.withPrefix(arg) has invalid prefix")
    void builderBuildThrowsIllegalStateExceptionWhenWithPrefixHasInvalidPrefix(String invalidPrefix) {
        var actualException = assertThrows(IllegalStateException.class, () -> ImmutableDoi.builder()
            .withProxy(EXAMPLE_PROXY)
            .withPrefix(invalidPrefix)
            .withSuffix(createRandomSuffix())
            .build());
        assertThat(actualException.getMessage(), is(equalTo(CANNOT_BUILD_DOI_DOI_PREFIX_IS_NOT_VALID)));
    }

    @ParameterizedTest(name = NAME_FORMAT)
    @MethodSource("badPrefixes")
    @DisplayName("builder.build() throws IllegalStateException when builder.withIdentifier(arg) has invalid prefix")
    void builderBuildThrowsIllegalStateExceptionWhenWithIdentifierHasInvalidPrefix(String invalidPrefix) {
        String badIdentifier = invalidPrefix + PATH_SEPARATOR + createRandomSuffix();
        var actualException = assertThrows(IllegalStateException.class, () -> ImmutableDoi.builder()
            .withProxy(EXAMPLE_PROXY)
            .withIdentifier(badIdentifier)
            .build());
        assertThat(actualException.getMessage(), is(equalTo(CANNOT_BUILD_DOI_DOI_PREFIX_IS_NOT_VALID)));
    }

    @ParameterizedTest(name = NAME_FORMAT)
    @MethodSource("badProxies")
    @DisplayName("builder.Build() throws IllegalStateException when builder.withProxy(arg) has invalid proxy")
    void builderBuildThrowsIllegalStateExceptionWhenWithProxyWasInvalidProxy(URI badProxy) {
        var actualException = assertThrows(IllegalStateException.class,
            () -> ImmutableDoi.builder()
                .withProxy(badProxy)
                .withIdentifier(EXAMPLE_IDENTIFIER)
                .build());
        assertThat(actualException.getMessage(), is(equalTo(CANNOT_BUILD_DOI_PROXY_IS_NOT_A_VALID_PROXY)));
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenWithProxyCalledTwice() {
        var actualException = assertThrows(IllegalStateException.class, () ->
            Doi.builder()
                .withProxy(STAGE_DOI_PROXY)
                .withProxy(DOI_PROXY));
        assertThat(actualException.getMessage(), containsString(ERROR_STRICT_BUILDER));
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenWithSuffixCalledTwice() {
        var actualException = assertThrows(IllegalStateException.class, () ->
            Doi.builder()
                .withSuffix(createRandomSuffix())
                .withSuffix(createRandomSuffix()));
        assertThat(actualException.getMessage(), containsString(ERROR_STRICT_BUILDER));
    }

    @Test
    void builderBuildThrowsIllegalStateExceptionWhenWithPrefixCalledTwice() {
        var actualException = assertThrows(IllegalStateException.class, () ->
            Doi.builder()
                .withPrefix(EXAMPLE_PREFIX)
                .withPrefix(EXAMPLE_PREFIX_2));
        assertThat(actualException.getMessage(), containsString(ERROR_STRICT_BUILDER));
    }

    private static Stream<String> badPrefixes() {
        return Stream.of("local.irrigation", "wanderlust", "11.9821", "murkyWater");
    }

    private static Stream<URI> badProxies() {
        return Stream.of(INVALID_PROXY, INVALID_PROXY_EXAMPLE_DOT_NET, INVALID_PROXY_FTP);
    }

    private static String createRandomSuffix() {
        return createRandomUuid();
    }

    private static String createRandomUuid() {
        return UUID.randomUUID().toString();
    }

    private static Stream<Arguments> validSchemesAndProxyHosts() {
        List<Arguments> arguments = new ArrayList<>();
        for (String validScheme : VALID_SCHEMES) {
            for (String validProxy : VALID_PROXIES) {
                arguments.add(Arguments.of(validScheme, validProxy));
            }
        }
        return arguments.stream();
    }

    private ImmutableDoi createDoi(String suffix) {
        return Doi.builder()
            .withProxy(STAGE_DOI_PROXY)
            .withPrefix(EXAMPLE_PREFIX)
            .withSuffix(suffix)
            .build();
    }

    private ImmutableDoi createDoi() {
        return Doi.builder()
            .withProxy(STAGE_DOI_PROXY)
            .withPrefix(EXAMPLE_PREFIX)
            .withSuffix(EXAMPLE_SUFFIX)
            .build();
    }
}