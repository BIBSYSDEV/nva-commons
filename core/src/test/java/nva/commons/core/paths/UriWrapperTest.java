package nva.commons.core.paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class UriWrapperTest {

    public static final String HOST = "http://www.example.org";
    private static final String ROOT = "/";

    @Test
    public void getPathRemovesPathDelimiterFromTheEndOfTheUri() {
        String inputPath = "/some/folder/file.json/";
        UriWrapper uriWrapper = new UriWrapper("http://www.example.org" + inputPath);
        String actualPath = uriWrapper.getPath().toString();
        String expectedPath = "/some/folder/file.json";
        assertThat(actualPath, is(equalTo(expectedPath)));
    }

    @Test
    public void getParentReturnsParentPathIfParentExists() {
        UriWrapper uriWrapper = new UriWrapper(HOST + "/level1/level2/file.json");
        UriWrapper parent = uriWrapper.getParent().orElseThrow();
        assertThat(parent.getPath().toString(), is(equalTo("/level1/level2")));
        UriWrapper grandParent = parent.getParent().orElseThrow();
        assertThat(grandParent.getPath().toString(), is(equalTo("/level1")));
    }

    @Test
    public void getParentReturnsEmptyWhenPathIsRoot() {
        UriWrapper uriWrapper = new UriWrapper(HOST + "/");
        Optional<UriWrapper> parent = uriWrapper.getParent();
        assertThat(parent.isEmpty(), is(true));
    }

    @Test
    public void getHostReturnsHostUri() {
        UriWrapper uriWrapper = new UriWrapper(HOST + "/some/path/is.here");
        URI expectedUri = URI.create(HOST);
        assertThat(uriWrapper.getHost().getUri(), is(equalTo(expectedUri)));
    }

    @Test
    public void addChildAddsChildToPath() {
        String originalPath = "/some/path";
        UriWrapper parent = new UriWrapper(HOST + originalPath);
        UriWrapper child = parent.addChild("level1", "level2", "level3");
        URI expectedChildUri = URI.create(HOST + originalPath + "/level1/level2/level3");
        assertThat(child.getUri(), is(equalTo(expectedChildUri)));

        UriWrapper anotherChild = parent.addChild("level4").addChild("level5");
        URI expectedAnotherChildUri = URI.create(HOST + originalPath + "/level4/level5");
        assertThat(anotherChild.getUri(), is(equalTo(expectedAnotherChildUri)));
    }

    @Test
    public void addChildReturnsPathWithChildWhenChildDoesNotStartWithDelimiter() {
        UriWrapper parentPath = new UriWrapper(HOST);
        String inputChildPath = "some/path";
        URI expectedResult = URI.create(HOST + ROOT + inputChildPath);
        UriWrapper actualResult = parentPath.addChild(inputChildPath);
        assertThat(actualResult.getUri(), is(equalTo(expectedResult)));
    }

    @Test
    public void toS3BucketPathReturnsPathWithoutRoot() {
        String expectedPath = "parent1/parent2/filename.txt";
        URI s3Uri = URI.create("s3://somebucket" + ROOT + expectedPath);
        UriWrapper wrapper = new UriWrapper(s3Uri);
        UnixPath s3Path = wrapper.toS3bucketPath();
        assertThat(s3Path.toString(), is(equalTo(expectedPath)));
    }

    @Test
    public void getFilenameReturnsFilenameOfUri() {
        String expectedFilename = "filename.txt";
        String filePath = String.join(UnixPath.PATH_DELIMITER, "parent1", "parent2", expectedFilename);
        URI s3Uri = URI.create("s3://somebucket" + ROOT + filePath);
        UriWrapper wrapper = new UriWrapper(s3Uri);
        assertThat(wrapper.getFilename(), is(equalTo(expectedFilename)));
    }

    @Test
    public void shouldReturnUriWithSchemeAndHostWhenCalledWithSchemeAndHost() {
        var uri = new UriWrapper("https", "example.org");
        assertThat(uri.getUri(), is(equalTo(URI.create("https://example.org"))));
    }

    @Test
    public void shouldReturnUriWithQueryParametersWhenSingleQueryParameterIsPresent() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1");
        URI uri = URI.create("https://www.example.org/");
        URI actualUri = new UriWrapper(uri)
            .addChild("path1")
            .addQueryParameter("key1", "value1")
            .addChild("path2")
            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    public void shouldReturnUriWithQueryParametersWhenManyQueryParametersArePresent() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1&key2=value2");
        URI uri = URI.create("https://www.example.org/");
        URI actualUri = new UriWrapper(uri)
            .addChild("path1")
            .addQueryParameter("key1", "value1")
            .addQueryParameter("key2", "value2")
            .addChild("path2")
            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    public void shouldReturnUriWithQueryParametersWhenQueryParametersAreMap() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1&key2=value2&key3=value3");
        URI uri = URI.create("https://www.example.org/");
        final Map<String, String> parameters = getOrderedParametersMap();
        URI actualUri = new UriWrapper(uri)
            .addChild("path1")
            .addQueryParameters(parameters)
            .addChild("path2")
            .addQueryParameter("key3", "value3")
            .getUri();
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    public void shouldReturnStringRepresentationOfUri() {
        URI expectedUri = URI.create("https://www.example.org/path1/path2?key1=value1&key2=value2&key3=value3");
        UriWrapper uri = new UriWrapper("https", "www.example.org")
            .addChild("path1")
            .addChild("path2")
            .addQueryParameter("key1", "value1")
            .addQueryParameter("key2", "value2")
            .addQueryParameter("key3", "value3");

        assertThat(uri.toString(), is(equalTo(expectedUri.toString())));
    }

    private Map<String, String> getOrderedParametersMap() {
        final Map<String, String> parameters = new TreeMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        return parameters;
    }

    @ParameterizedTest(name = "should throw exception when either host is empty")
    @NullAndEmptySource
    public void shouldThrowExceptionWhenHostIsEmpty(String emptyInput) {
        assertThrows(IllegalArgumentException.class, () -> new UriWrapper("https", emptyInput));
    }
}
