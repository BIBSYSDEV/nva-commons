package nva.commons.core.paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
}
