package nva.commons.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import nva.commons.doi.DoiSuppliers.DoiInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class DoiValidatorTest {

    private static final String DOI = "10.1000/182";
    private static final String DOI_PREFIX = "doi:";
    private static final String HTTPS_DOI = "https://doi.org/";
    private static final String HTTP_DOI = "http://doi.org/";
    private static final String HTTPS_DX_DOI = "https://dx.doi.org/";
    private static final String INVALID_URL = "https://something.com";

    private static final String ATTACHMENT = " 02872-6 ";

    @ParameterizedTest(name = "validate returns true when doi is {0}")
    @MethodSource("validDois")
    void validateReturnsTrueWhenDoisHaveExpectedForm(DoiInput doi) {
        assertThat(DoiValidator.validateOffline(doi.getDoi()), is(equalTo(true)));
    }

    @Test
    void validateReturnsTrueForDoiStringStartingWithTenDotPrefix() {
        assertThat(DoiValidator.validateOffline(DOI), is((equalTo(true))));
    }

    @Test
    void validateReturnsTrueForDoiStringWitDoiPrefix() {
        String input = DOI_PREFIX + DOI;
        assertThat(DoiValidator.validateOffline(input), is((equalTo(true))));
    }

    @Test
    void validateReturnsTrueForHttpsDoiWitDoiUri() {
        String input = HTTPS_DOI + DOI;
        assertThat(DoiValidator.validateOffline(input), is((equalTo(true))));
    }

    @Test
    void validateReturnsTrueForHttpDoiWitDoiUri() {
        String input = HTTP_DOI + DOI;
        assertThat(DoiValidator.validateOffline(input), is((equalTo(true))));
    }

    @Test
    void validateReturnsTrueForHttpsDxDoiWitDoiUri() {
        String input = HTTPS_DX_DOI + DOI;
        assertThat(DoiValidator.validateOffline(input), is((equalTo(true))));
    }

    @Test
    void validateReturnsFalseForInvalidDoiUri() {
        String input = INVALID_URL + DOI;
        assertThat(DoiValidator.validateOffline(input), is((equalTo(false))));
    }

    @Test
    void validateReturnsTrueForValidURL() {
        URI input = URI.create(HTTP_DOI + DOI);
        assertThat(DoiValidator.validateOffline(input), is((equalTo(true))));
    }

    @ParameterizedTest
    @ValueSource(strings = {ATTACHMENT + HTTPS_DOI + DOI, HTTPS_DOI + DOI + ATTACHMENT})
    void shouldReturnFalseForDoiWithPrefixStringOrPostfixString(String invalidUri) {
        assertThat(DoiValidator.validateOffline(invalidUri), is((equalTo(false))));
    }

    private static Stream<DoiInput> validDois() throws URISyntaxException {
        return DoiSuppliers.validDois();
    }
}