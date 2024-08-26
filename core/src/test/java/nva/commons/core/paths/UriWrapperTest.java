package nva.commons.core.paths;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class UriWrapperTest {

    public static final String HOST = "http://www.example.org";
    public static final int MAX_PORT_NUMBER = 65535;
    private static final String ROOT = "/";

    public static Stream<Named<URI>> queryParameterProvider() {
        return Stream.of(
            Named.of("URI with unencoded query part", URI.create("https://example.org?q=høhë")),
            Named.of("URI with encoded query part", URI.create("https://example.org?q=h%C3%B8h%C3%AB"))
        );
    }

    public static Stream<Named<URI>> encodedParameterProvider() {
        return Stream.of(
            Named.of("Key containing encoded assignment character", URI.create("https://example.org?k%3Ds=equals")),
            Named.of("Value containing encoded assignment character", URI.create("https://example.org?kes=%3Ds"))
        );
    }

    public static Stream<Named<String>> valuelessParameterProvider() {
        return Stream.of(
            Named.of("URI with key only", "https://example.org?param"),
            Named.of("URI with key and assignment symbol", "https://example.org?param="),
            Named.of("URI with key and null-as-string assigned", "https://example.org?param=null"),
            Named.of("URI with key and space assigned", "https://example.org?param=%20")
        );
    }

    @Test
    void getPathRemovesPathDelimiterFromTheEndOfTheUri() {
        String inputPath = "/some/folder/file.json/";
        UriWrapper uriWrapper = UriWrapper.fromUri("http://www.example.org" + inputPath);
        String actualPath = uriWrapper.getPath().toString();
        String expectedPath = "/some/folder/file.json";
        assertThat(actualPath, is(equalTo(expectedPath)));
    }

    @Test
    void getParentReturnsParentPathIfParentExists() {
        UriWrapper uriWrapper = UriWrapper.fromUri(HOST + "/level1/level2/file.json");
        UriWrapper parent = uriWrapper.getParent().orElseThrow();
        assertThat(parent.getPath().toString(), is(equalTo("/level1/level2")));
        UriWrapper grandParent = parent.getParent().orElseThrow();
        assertThat(grandParent.getPath().toString(), is(equalTo("/level1")));
    }

    @Test
    void getParentReturnsEmptyWhenPathIsRoot() {
        UriWrapper uriWrapper = UriWrapper.fromUri((HOST + "/"));
        Optional<UriWrapper> parent = uriWrapper.getParent();
        assertThat(parent.isEmpty(), is(true));
    }

    @Test
    void getHostReturnsHostUri() {
        UriWrapper uriWrapper = UriWrapper.fromUri(HOST + "/some/path/is.here");
        URI expectedUri = URI.create(HOST);
        assertThat(uriWrapper.getHost().getUri(), is(equalTo(expectedUri)));
    }

    @Test
    void addChildAddsChildToPath() {
        String originalPath = "/some/path";
        UriWrapper parent = UriWrapper.fromUri(HOST + originalPath);
        UriWrapper child = parent.addChild("level1", "level2", "level3");
        URI expectedChildUri = URI.create(HOST + originalPath + "/level1/level2/level3");
        assertThat(child.getUri(), is(equalTo(expectedChildUri)));

        UriWrapper anotherChild = parent.addChild("level4").addChild("level5");
        URI expectedAnotherChildUri = URI.create(HOST + originalPath + "/level4/level5");
        assertThat(anotherChild.getUri(), is(equalTo(expectedAnotherChildUri)));
    }

    @Test
    void addChildReturnsPathWithChildWhenChildDoesNotStartWithDelimiter() {
        UriWrapper parentPath = UriWrapper.fromUri(HOST);
        String inputChildPath = "some/path";
        URI expectedResult = URI.create(HOST + ROOT + inputChildPath);
        UriWrapper actualResult = parentPath.addChild(inputChildPath);
        assertThat(actualResult.getUri(), is(equalTo(expectedResult)));
    }

    @Test
    void toS3BucketPathReturnsPathWithoutRoot() {
        String expectedPath = "parent1/parent2/filename.txt";
        URI s3Uri = URI.create("s3://somebucket" + ROOT + expectedPath);
        UriWrapper wrapper = UriWrapper.fromUri(s3Uri);
        UnixPath s3Path = wrapper.toS3bucketPath();
        assertThat(s3Path.toString(), is(equalTo(expectedPath)));
    }

    @Test
    void getFilenameReturnsFilenameOfUri() {
        String expectedFilename = "filename.txt";
        String filePath = String.join(UnixPath.PATH_DELIMITER, "parent1", "parent2", expectedFilename);
        URI s3Uri = URI.create("s3://somebucket" + ROOT + filePath);
        UriWrapper wrapper = UriWrapper.fromUri(s3Uri);
        assertThat(wrapper.getLastPathElement(), is(equalTo(expectedFilename)));
    }

    @Test
    void shouldReturnUriWithSchemeAndHostWhenCalledWithSchemeAndHost() {
        var uri = new UriWrapper("https", "example.org");
        assertThat(uri.getUri(), is(equalTo(URI.create("https://example.org"))));
    }

    @Test
    void shouldReturnUriWithQueryParametersWhenSingleQueryParameterIsPresent() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1");
        URI uri = URI.create("https://www.example.org/");
        URI actualUri = UriWrapper.fromUri(uri)
                            .addChild("path1")
                            .addQueryParameter("key1", "value1")
                            .addChild("path2")
                            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnUriWithEscapedAmpersandInQueryParameterValue() {
        URI expectedUri = URI.create(
            "https://www.example.org/my-path?key1=someWonderfulSimpleValue&key2=some%20%26%20value&key3=valueWithout"
            + "%26space");
        URI uri = URI.create("https://www.example.org/");
        URI actualUri = UriWrapper.fromUri(uri)
                            .addChild("my-path")
                            .addQueryParameter("key1", "someWonderfulSimpleValue")
                            .addQueryParameter("key2", "some & value")
                            .addQueryParameter("key3", "valueWithout&space")
                            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldPreservePortWhenAddingPathAndQueryParametersInUri() {
        var expectedUri = URI.create("https://www.example.org:1234/path1/path2?key1=value1");
        var host = URI.create("https://www.example.org:1234");
        var actualUri = UriWrapper.fromUri(host)
                            .addChild("path1")
                            .addQueryParameter("key1", "value1")
                            .addChild("path2")
                            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnUriWithQueryParametersWhenManyQueryParametersArePresent() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1&key2=value2");
        URI uri = URI.create("https://www.example.org/");
        URI actualUri = UriWrapper.fromUri(uri)
                            .addChild("path1")
                            .addQueryParameter("key1", "value1")
                            .addQueryParameter("key2", "value2")
                            .addChild("path2")
                            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnUriWithQueryParametersWhenQueryParametersAreMap() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1&key2=value2&key3=value3");
        URI uri = URI.create("https://www.example.org/");
        final Map<String, String> parameters = getOrderedParametersMap();
        URI actualUri = UriWrapper.fromUri(uri)
                            .addChild("path1")
                            .addQueryParameters(parameters)
                            .addChild("path2")
                            .addQueryParameter("key3", "value3")
                            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnStringRepresentationOfUri() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1&key2=value2&key3=value3");
        UriWrapper uri = new UriWrapper("https", "www.example.org")
                             .addChild("path1")
                             .addChild("path2")
                             .addQueryParameter("key1", "value1")
                             .addQueryParameter("key2", "value2")
                             .addQueryParameter("key3", "value3");

        assertThat(uri.toString(), is(equalTo(expectedUri.toString())));
    }

    @ParameterizedTest(name = "should throw exception when either host is empty")
    @NullAndEmptySource
    void shouldThrowExceptionWhenHostIsEmpty(String emptyInput) {
        assertThrows(IllegalArgumentException.class, () -> new UriWrapper("https", emptyInput));
    }

    @Test
    void shouldCreateAnHttpsUriByDefaultWhenInputIsAHostDomain() {
        var constructedUri = UriWrapper.fromHost("example.org").getUri();
        assertThat(constructedUri.getScheme(), is(equalTo(UriWrapper.HTTPS)));
    }

    @Test
    void shouldATolerateInputAsUriWhenCreatingUriFromHost() {
        var hostAsUri = "http://example.com/hello/world";
        var actualHostUri = UriWrapper.fromHost(hostAsUri).getUri();
        var expectedHostUri = URI.create("https://example.com");
        assertThat(actualHostUri, is(expectedHostUri));
    }

    @Test
    void shouldReturnUriWithCustomPort() {
        var expectedPort = randomInteger(MAX_PORT_NUMBER);
        var actualUri = UriWrapper.fromHost("example.org", expectedPort).getUri();
        assertThat(actualUri, is(equalTo(URI.create("https://example.org:" + expectedPort))));
    }

    @Test
    void shouldUpdateQueryParametersDynamically() {
        var uri = randomUri();
        var uriWrapper = UriWrapper.fromUri(uri);
        var firstQueryParameter = randomString();
        var secondQueryParameter = randomString();

        uriWrapper.addQueryParameter("param1", firstQueryParameter);
        uriWrapper.addQueryParameter("param2", secondQueryParameter);

        assertThat(uriWrapper.toString(), containsString(firstQueryParameter));
        assertThat(uriWrapper.toString(), containsString(secondQueryParameter));
    }

    @Test
    void shouldAddTheSameQueryParameterTwiceDynamically() {
        var uri = randomUri();
        var uriWrapper = UriWrapper.fromUri(uri);
        var firstQueryParameter = randomString();
        var secondQueryParameter = randomString();

        uriWrapper.addQueryParameter("param", firstQueryParameter);
        uriWrapper.addQueryParameter("param", secondQueryParameter);

        assertThat(uriWrapper.toString(), containsString(firstQueryParameter));
        assertThat(uriWrapper.toString(), containsString(secondQueryParameter));
    }

    @ParameterizedTest
    @DisplayName("Should encode query params passed with original URI when adding new query params")
    @MethodSource("queryParameterProvider")
    void shouldEncodeOriginalUriQueryParametersWhenAddingQueryParameters(URI uri) {
        var expected = "https://example.org?q=h%C3%B8h%C3%AB&b=m%C3%BCll%C3%A5r";
        var uriWrapper = UriWrapper.fromUri(uri).addQueryParameter("b", "müllår");
        assertThat(uriWrapper.toString(), is(equalTo(expected)));
    }

    @ParameterizedTest
    @DisplayName("Should split query params passed with original URI when adding new query params")
    @MethodSource("encodedParameterProvider")
    void shouldSplitOriginalUriParametersWhenQueryParametersAreAdded(URI uri) {
        var expected = uri.toString() + "&some=thing";
        var uriWrapper = UriWrapper.fromUri(uri).addQueryParameter("some", "thing");
        assertThat(uriWrapper.toString(), is(equalTo(expected)));
    }

    @ParameterizedTest
    @DisplayName("Should accept zero value query params passed with original URI when adding new query params")
    @MethodSource("valuelessParameterProvider")
    void shouldAcceptValuelessQueryParameterWhenQueryParametersAreAdded(String uri) {
        var expected = uri + "&a=b";
        var uriWrapper = UriWrapper.fromUri(URI.create(uri)).addQueryParameter("a", "b");
        assertThat(uriWrapper.toString(), is(equalTo(expected)));
    }

    private Map<String, String> getOrderedParametersMap() {
        final Map<String, String> parameters = new TreeMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        return parameters;
    }
}
