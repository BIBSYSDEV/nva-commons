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
        var doi = Doi.fromDoiString(Doi.DEFAULT_HOST, doiWithoutRootPath);
        assertThat(doi.getUri(), is(equalTo(URI.create("https://doi.org/" + doiWithoutRootPath))));
    }

    @Test
    void shouldReturnUriWhenCratedWithHostAndDoiPathExtractedFromJavasUriClass() {
        var doiUri = randomDoi();
        var extractedPathDirectlyFromUri = doiUri.getPath();
        assertThat(extractedPathDirectlyFromUri, startsWith("/"));
        var doi = Doi.fromDoiString(Doi.DEFAULT_HOST, extractedPathDirectlyFromUri);
        assertThat(doi.getUri(), is(equalTo(URI.create("https://doi.org" + extractedPathDirectlyFromUri))));
    }

    @Test
    void shouldReturnNonUriFormOfDoi() {
        var doiUri = randomDoi();
        var expectedDoiString = removeRoot(doiUri.getPath());
        var actualDoiString = new Doi(doiUri).getDoiString();
        assertThat(actualDoiString, is(equalTo(expectedDoiString)));
    }

    @ParameterizedTest
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
        String expectedDoiString = randomDoi().toString();
        var jsonString = "\"" + expectedDoiString + "\"";
        var doi = JsonUtils.dtoObjectMapper.readValue(jsonString, Doi.class);
        assertThat(doi.toString(), is(equalTo(expectedDoiString)));

        var serializedDoi = JsonUtils.dtoObjectMapper.writeValueAsString(doi);
        assertThat(serializedDoi, is(equalTo(jsonString)));
    }

    private String removeRoot(String path) {
        return path.startsWith("/")
                   ? path.substring(SECOND_CHARACTER)
                   : path;
    }
}