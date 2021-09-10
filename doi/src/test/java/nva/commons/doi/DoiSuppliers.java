package nva.commons.doi;

import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.doi.Constants.HTTPS_DOI_ORG;
import static nva.commons.doi.Constants.HTTPS_DX_DOI_ORG;
import static nva.commons.doi.Constants.HTTP_DOI_ORG;
import static nva.commons.doi.Constants.HTTP_DX_DOI_ORG;
import static nva.commons.doi.DoiConverter.DOI_HOST;
import static nva.commons.doi.DoiConverter.EMPTY_FRAGMENT;
import static nva.commons.doi.DoiConverter.HTTPS;
import static nva.commons.doi.DoiConverter.PATH_SEPARATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringStartsWith.startsWith;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import nva.commons.core.ioutils.IoUtils;
import org.apache.commons.lang3.RandomStringUtils;

public final class DoiSuppliers {

    public static final int MIN_ARBITRARY_LENGTH = 3;
    public static final int MAX_ARBITRARY_LENGTH = 10;
    public static final String DOI_SUBPART_DELIMITER = ".";
    public static final Random RANDOM = new Random();
    private static final String STANDARD_FIRST_PART_OF_DOI_PREFIX = "10";
    private static final String CUSTOMARY_DOI_PREFIX = "doi:";

    private DoiSuppliers() {
    }

    public static Stream<String> resolvableDois() {
        return IoUtils.linesfromResource(Path.of("knownDois.txt")).stream().map(String::strip);
    }

    // used in parametrized tests
    public static Stream<DoiInput> validDois() throws URISyntaxException {
        String doiValue = randomDoiString();

        final URI expectedUri = new URI(HTTPS, DOI_HOST, doiValueToPath(doiValue), EMPTY_FRAGMENT);
        assertThat(expectedUri.toString(), is(equalTo("https://doi.org/" + doiValue)));

        DoiInput nonCustomaryPrefixNonUriDoi = createDoiInput(doiValue, expectedUri);
        assertThat(nonCustomaryPrefixNonUriDoi.getDoi(), startsWith("10."));

        DoiInput withCustomaryPrefixNonUriDoi = createDoiInput(CUSTOMARY_DOI_PREFIX + doiValue, expectedUri);
        assertThat(withCustomaryPrefixNonUriDoi.getDoi(), startsWith("doi:10."));

        DoiInput withUpperCaseCustomaryPrefixNonUriDoi =
            createDoiInput(CUSTOMARY_DOI_PREFIX.toUpperCase(Locale.ROOT) + doiValue, expectedUri);
        assertThat(withUpperCaseCustomaryPrefixNonUriDoi.getDoi(), startsWith("DOI:10."));

        DoiInput httpDoiDx = createDoiInput(HTTP_DX_DOI_ORG + doiValue, expectedUri);
        assertThat(httpDoiDx.getDoi(), startsWith("http://dx.doi.org/10."));

        DoiInput httpsDoiDx = createDoiInput(HTTPS_DX_DOI_ORG + doiValue, expectedUri);
        assertThat(httpsDoiDx.getDoi(), startsWith("https://dx.doi.org/10."));

        DoiInput httpDoiOrg = createDoiInput(HTTP_DOI_ORG + doiValue, expectedUri);
        assertThat(httpDoiOrg.getDoi(), startsWith("http://doi.org/10."));

        DoiInput httpsDoiOrg = createDoiInput(HTTPS_DOI_ORG + doiValue, expectedUri);
        assertThat(httpsDoiOrg.getDoi(), startsWith("https://doi.org/10."));

        return Stream.of(nonCustomaryPrefixNonUriDoi,
                         withCustomaryPrefixNonUriDoi,
                         withUpperCaseCustomaryPrefixNonUriDoi,
                         httpDoiDx,
                         httpsDoiDx,
                         httpDoiOrg,
                         httpsDoiOrg);
    }

    private static DoiInput createDoiInput(String doiString, URI expectedUri) {
        return attempt(() -> new DoiInput(doiString, expectedUri)).orElseThrow();
    }

    private static String doiValueToPath(String doiValue) {
        return DoiConverter.ROOT_PATH + doiValue;
    }

    private static String randomDoiString() {
        String prefixSecondPart = RandomStringUtils.randomAlphanumeric(MIN_ARBITRARY_LENGTH, MAX_ARBITRARY_LENGTH);
        String suffix = randomDoiSuffix();
        return STANDARD_FIRST_PART_OF_DOI_PREFIX
               + DOI_SUBPART_DELIMITER
               + prefixSecondPart
               + PATH_SEPARATOR
               + suffix;
    }

    private static String randomDoiSuffix() {
        return IntStream.range(1, 2 + RANDOM.nextInt(4)).boxed()
            .map(ignored -> RandomStringUtils.randomAlphanumeric(MIN_ARBITRARY_LENGTH, MAX_ARBITRARY_LENGTH))
            .collect(Collectors.joining(DOI_SUBPART_DELIMITER));
    }

    public static class DoiInput {

        private final String doi;
        private final URI expectedUri;

        public DoiInput(String doi, URI expectedUri) {
            this.doi = doi;
            this.expectedUri = expectedUri;
        }

        public String getDoi() {
            return doi;
        }

        public URI getExpectedUri() {
            return expectedUri;
        }

        public String toString() {
            return doi;
        }
    }
}
