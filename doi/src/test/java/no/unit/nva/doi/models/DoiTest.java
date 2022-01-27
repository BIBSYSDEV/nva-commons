package no.unit.nva.doi.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringStartsWith.startsWith;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URISyntaxException;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DoiTest {

    public static final int SECOND_CHARACTER = 1;
    public static final String EMPTY_FRAGMENT = null;
    public static final String DEFAULT_DOI_URI_PREFIX = "https://doi.org";
    public static final String URI_PATH_SEPARATOR = "/";

    @Test
    void shouldBeGeneratedFromUriAndBeThatUri() {
        var doiUri = randomDoi();
        var doi = Doi.fromUri(doiUri);
        assertThat(doi.getUri(), is(equalTo(doiUri)));
    }

    @Test
    void shouldReturnUriWhenCreatedWithHostAndDoiString() {
        var doiWithoutRootPath = UnixPath.of(randomDoi().getPath()).removeRoot().toString();
        assertThat(doiWithoutRootPath, not(startsWith(URI_PATH_SEPARATOR)));
        var doi = Doi.fromDoiIdentifier(Doi.DEFAULT_HOST, doiWithoutRootPath);
        assertThat(doi.getUri(),
                   is(equalTo(URI.create(DEFAULT_DOI_URI_PREFIX + URI_PATH_SEPARATOR + doiWithoutRootPath))));
    }

    @Test
    void shouldReturnUriWhenCreatedWithHostAndDoiPathExtractedFromJavasUriClass() {
        var doiUri = randomDoi();
        var extractedPathDirectlyFromUriContainingPathSeparator = doiUri.getPath();
        assertThat(extractedPathDirectlyFromUriContainingPathSeparator, startsWith(URI_PATH_SEPARATOR));
        var doi = Doi.fromDoiIdentifier(Doi.DEFAULT_HOST, extractedPathDirectlyFromUriContainingPathSeparator);
        var expectedUri = URI.create(DEFAULT_DOI_URI_PREFIX + extractedPathDirectlyFromUriContainingPathSeparator);
        assertThat(doi.getUri(), is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnNonUriFormOfDoi() {
        var doiUri = randomDoi();
        var expectedDoiString = removeRoot(doiUri.getPath());
        var actualDoiString = Doi.fromUri(doiUri).toIdentifier();
        assertThat(actualDoiString, is(equalTo(expectedDoiString)));
    }

    @ParameterizedTest(name = "should transform non standard doi formats to the standard doi format")
    @ValueSource(strings = {
        "http://doi.org/10.1000/12234",
        "http://dx.doi.org/10.1000/12234",
        "https://dx.doi.org/10.1000/12234",
        "https://example.com/10.1000/12234",
    })
    void shouldTransformNonStandardDoiFormatsToStandardDoiFormat(String nonStandardUri) throws URISyntaxException {
        var uri = URI.create(nonStandardUri);
        var doi = Doi.fromUri(uri);
        assertThat(doi.getUri(), is(equalTo(uri)));
        var expectedStandardizedUri = new URI("https", Doi.DEFAULT_HOST, uri.getPath(), EMPTY_FRAGMENT);
        assertThat(doi.getStandardizedUri(), is(equalTo(expectedStandardizedUri)));
    }

    @Test
    void shouldBeDeserializedFromStringAndSerializedAsString() throws JsonProcessingException {
        var expectedDoiString = randomDoi().toString();
        var jsonString = "\"" + expectedDoiString + "\"";
        var doi = JsonUtils.dtoObjectMapper.readValue(jsonString, Doi.class);
        assertThat(doi.toString(), is(equalTo(expectedDoiString)));

        var serializedDoi = JsonUtils.dtoObjectMapper.writeValueAsString(doi);
        assertThat(serializedDoi, is(equalTo(jsonString)));
    }

    @Test
    void shouldReturnStandardFormatOfDoiUriWhenCreatedOnlyWithIdentifier() {
        var expectedDoi = randomDoi();
        assertThat(expectedDoi.toString(), startsWith(DEFAULT_DOI_URI_PREFIX));
        var actualDoi = Doi.fromDoiIdentifier(expectedDoi.getPath()).getUri();
        assertThat(actualDoi, is(equalTo(expectedDoi)));
    }

    @Test
    void shouldReturnStandardUriWithCustomHostWhenCustomHostIsSupplied() throws URISyntaxException {
        var seedValue = randomDoi();
        var hostOfSandboxEnvironment = "example.org";
        var expectedDoiUri = createExpectedSandboxDoiUri(seedValue, hostOfSandboxEnvironment);
        assertThat(expectedDoiUri.toString(), startsWith("https://example.org/"));
        var actualDoi = Doi.fromUri(seedValue).changeHost(hostOfSandboxEnvironment).getUri();
        assertThat(actualDoi, is(equalTo(expectedDoiUri)));
    }

    @Test
    void shouldReturnExpectedUriWhenCustomHostPrefixAndSuffixAreSupplied() {
        var host = randomString();
        var prefix = UnixPath.of(randomDoi().getPath()).removeRoot().getParent().orElseThrow().toString();
        var suffix = UnixPath.of(randomDoi().getPath()).getFilename();
        var expectedUri = URI.create(String.format("https://%s/%s/%s", host, prefix, suffix));
        var actualUri = Doi.fromPrefixAndSuffix(host, prefix, suffix).getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnStandardHttpsUriWhenInputIsDoiUri() {
        var inputDoi = URI.create("doi:10.1000/182");
        var parsedDoi = Doi.fromUri(inputDoi);
        var expectedDoi = URI.create(DEFAULT_DOI_URI_PREFIX + URI_PATH_SEPARATOR + "10.1000/182");
        assertThat(parsedDoi.getStandardizedUri(), is(equalTo(expectedDoi)));
    }

    private URI createExpectedSandboxDoiUri(URI seedValue, String hostOfSandboxEnvironment) throws URISyntaxException {
        return new URI(UriWrapper.HTTPS, hostOfSandboxEnvironment, seedValue.getPath(), DoiTest.EMPTY_FRAGMENT);
    }

    private String removeRoot(String path) {
        return path.startsWith(URI_PATH_SEPARATOR) ? path.substring(SECOND_CHARACTER) : path;
    }
}