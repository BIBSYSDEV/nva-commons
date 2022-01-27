package no.unit.nva.doi.models;

import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
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

    @Test
    void shouldBeGeneratedFromUriAndBeThatUri() {
        var doiUri = randomDoi();
        var doi = Doi.fromUri(doiUri);
        assertThat(doi.getUri(), is(equalTo(doiUri)));
    }

    @Test
    void shouldReturnUriWhenCratedWithHostAndDoiString() {
        var doiWithoutRootPath = UnixPath.of(randomDoi().getPath()).removeRoot().toString();
        assertThat(doiWithoutRootPath, not(startsWith("/")));
        var doi = Doi.fromDoiIdentifier(Doi.DEFAULT_HOST, doiWithoutRootPath);
        assertThat(doi.getUri(), is(equalTo(URI.create("https://doi.org/" + doiWithoutRootPath))));
    }

    @Test
    void shouldReturnUriWhenCratedWithHostAndDoiPathExtractedFromJavasUriClass() {
        var doiUri = randomDoi();
        var extractedPathDirectlyFromUri = doiUri.getPath();
        assertThat(extractedPathDirectlyFromUri, startsWith("/"));
        var doi = Doi.fromDoiIdentifier(Doi.DEFAULT_HOST, extractedPathDirectlyFromUri);
        assertThat(doi.getUri(), is(equalTo(URI.create("https://doi.org" + extractedPathDirectlyFromUri))));
    }

    @Test
    void shouldReturnNonUriFormOfDoi() {
        var doiUri = randomDoi();
        var expectedDoiString = removeRoot(doiUri.getPath());
        var actualDoiString = new Doi(doiUri).toIdentifier();
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
        assertThat(expectedDoi.toString(), startsWith("https://doi.org"));
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

    private URI createExpectedSandboxDoiUri(URI seedValue, String hostOfSandboxEnvironment) throws URISyntaxException {
        return new URI(UriWrapper.HTTPS, hostOfSandboxEnvironment, seedValue.getPath(), DoiTest.EMPTY_FRAGMENT);
    }

    private String removeRoot(String path) {
        return path.startsWith("/")
                   ? path.substring(SECOND_CHARACTER)
                   : path;
    }
}