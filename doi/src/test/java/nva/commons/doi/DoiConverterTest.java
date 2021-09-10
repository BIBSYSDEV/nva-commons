package nva.commons.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import nva.commons.doi.DoiSuppliers.DoiInput;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.invocation.InvocationOnMock;

public class DoiConverterTest {

    public static final boolean DO_NOT_VALIDATE_ONLINE = true;
    private static final String DOI = "10.1000/182";
    private static final String EXPECTED = "https://doi.org/" + DOI;
    DoiConverter doiConverterImpl = new DoiConverter((uri) -> DO_NOT_VALIDATE_ONLINE);

    @Tag("RemoteTest")
    @ParameterizedTest(name = "validateOnline returns true for doi:{0}")
    @MethodSource("resolvableDois")
    public void toUriReturnsUrisWhenInputCanBeEasilyConvertedToValidDoi(String doi) {
        assertThat(doiConverterImpl.toUri(doi), is(not(nullValue())));
    }

    @ParameterizedTest(name = "toUri returns a URI if input is a valid DOI string")
    @MethodSource("validDois")
    public void toUriReturnsAUriIfInputIsAValidDoiUri(DoiInput inputDoi) {
        URI actual = doiConverterImpl.toUri(inputDoi.getDoi());
        assertThat(actual, is(equalTo(inputDoi.getExpectedUri())));
    }

    @ParameterizedTest(name = "doi returns URI when input is valid URI but does not have alphanumeric char at the end")
    @ValueSource(strings = {"doi:10.1016/j.seares.2007.02.001.", "doi:10.1016/j.seares.2007.02.001.."})
    public void toUriReturnsUriWhenInputIsValidUriButHasNonAlphanumericCharacterAtTheEnd(String doi) {
        URI actual = doiConverterImpl.toUri(doi);
        assertThat(actual, is(equalTo(URI.create("https://doi.org/10.1016/j.seares.2007.02.001"))));
    }

    @Test
    public void toUriThrowsExceptionWhenInputCannotBeResolvedOnline() {
        String doi = "10.1016/j.jngse.2015.08.051";
        UnitHttpClient unitClient = mock(UnitHttpClient.class);
        when(unitClient.sendAsync(any(HttpRequest.Builder.class), any(HttpResponse.BodyHandler.class)))
            .thenAnswer(this::failedResponse);
        doiConverterImpl = new DoiConverter(new DoiValidator(unitClient));
        assertThrows(UnresolvableDoiException.class, () -> doiConverterImpl.toUri(doi));
    }

    @DisplayName("toUri returns a URI when input is a doi identifier")
    @Test
    public void toUriReturnsAUriIfInputIsADoiIdentifier() {
        URI actual = doiConverterImpl.toUri(DOI);
        assertThat(actual.toString(), is(equalTo(EXPECTED)));
    }

    @DisplayName("toUri returns an HTTPS URI when input is an HTTP URI")
    @Test
    public void toUriReturnsAUriIfInputIsAnHttpDoiUri() {
        String input = "http://doi.org/" + DOI;
        URI actual = doiConverterImpl.toUri(input);
        assertThat(actual.toString(), is(equalTo(EXPECTED)));
    }

    @Test
    @DisplayName("toUri throws Exception when input is not a valid URI")
    public void toUriThrowsAnExceptionWhenInputIsNotValidUri() {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        String input = "http://somethingelse.org/" + DOI;
        InvalidDoiException exception = assertThrows(InvalidDoiException.class, () -> doiConverterImpl.toUri(input));
        assertThat(exception.getMessage(), containsString(input));
        assertThat(appender.getMessages(), containsString(input));
    }

    @Test
    @DisplayName("toURI returns a URI when input is a DOI string with DOI prefix")
    public void toUriReturnsAUriIfUriWhenInputIsDoiStringWithDoiPrefix() {
        String input = "doi:" + DOI;
        URI actual = doiConverterImpl.toUri(input);
        assertThat(actual.toString(), is(equalTo(EXPECTED)));
    }

    @Test
    @DisplayName("toURI throws Exception when input is an invalid DOI string")
    public void toUriThrowsExceptionWhenInputIsAnInvalidDoiString() {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        String input = "213456";
        InvalidDoiException exception = assertThrows(InvalidDoiException.class, () -> doiConverterImpl.toUri(input));
        assertThat(exception.getMessage(), containsString(input));
        assertThat(appender.getMessages(), containsString(input));
    }

    private static Stream<DoiInput> validDois() throws URISyntaxException {
        return DoiSuppliers.validDois();
    }

    private static Stream<String> resolvableDois() {
        return DoiSuppliers.resolvableDois();
    }

    private CompletableFuture<HttpResponse<String>> failedResponse(InvocationOnMock invocation) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
        when(response.body()).thenReturn("Not Found");
        return CompletableFuture.completedFuture(response);
    }
}