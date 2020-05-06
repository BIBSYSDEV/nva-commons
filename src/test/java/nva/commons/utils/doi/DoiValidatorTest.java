package nva.commons.utils.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class DoiValidatorTest {

    private static final String DOI = "10.1000/182";
    private static final String DOI_PREFIX = "doi:";
    private static final String HTTPS_DOI = "https://doi.org/";
    private static final String HTTP_DOI = "http://doi.org/";
    private static final String HTTPS_DX_DOI = "https://dx.doi.org/";
    private static final String INVALID_URL = "https://something.com";
    private static final String INVALID_PREFIX = "odi:";

    @Test
    public void validateReturnsTrueForDoiStringWithoutDoiPrefix() {
        assertThat(DoiValidator.validate(DOI), is((equalTo(true))));
    }

    @Test
    public void validateReturnsTrueForDoiStringWitDoiPrefix() {
        String input = DOI_PREFIX + DOI;
        assertThat(DoiValidator.validate(input), is((equalTo(true))));
    }

    @Test
    public void validateReturnsTrueForHttpsDoiWitDoiUri() {
        String input = HTTPS_DOI + DOI;
        assertThat(DoiValidator.validate(input), is((equalTo(true))));
    }

    @Test
    public void validateReturnsTrueForHttpDoiWitDoiUri() {
        String input = HTTP_DOI + DOI;
        assertThat(DoiValidator.validate(input), is((equalTo(true))));
    }

    @Test
    public void validateReturnsTrueForHttpsDxDoiWitDoiUri() {
        String input = HTTPS_DX_DOI + DOI;
        assertThat(DoiValidator.validate(input), is((equalTo(true))));
    }

    @Test
    public void validateReturnsFalseForInvalidDoiUri() {
        String input = INVALID_URL + DOI;
        assertThat(DoiValidator.validate(input), is((equalTo(false))));
    }

    @Test
    public void validateReturnsFalseForInvalidDoiPrefix() {
        String input = INVALID_PREFIX + DOI;
        assertThat(DoiValidator.validate(input), is((equalTo(false))));
    }

    @Test
    public void validateReturnsTrueForValidURL() throws MalformedURLException {
        URL input = URI.create(HTTP_DOI + DOI).toURL();
        assertThat(DoiValidator.validate(input), is((equalTo(true))));
    }
}