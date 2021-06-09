package no.unit.nva.s3;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.text.IsEmptyString.emptyString;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class UnixPathTest {

    public static final String EMPTY_STRING = "";

    @Test
    public void ofReturnsPathWithAllPathElementsInOrderWhenInputArrayIsNotEmpty() {
        UnixPath unixPath = UnixPath.of("first", "second", "third");
        assertThat(unixPath.toString(), is(equalTo("first/second/third")));
    }

    @Test
    public void ofReturnsPathWithRootWhenRootIsFirstElement() {
        UnixPath unixPath = UnixPath.of("/", "first", "second", "third");
        assertThat(unixPath.toString(), is(equalTo("/first/second/third")));
    }

    @Test
    public void ofReturnsEmptyPathWhenInputIsNull() {
        UnixPath unixPath = UnixPath.of(null);
        System.out.println(unixPath);
        assertThat(unixPath.toString(), is(emptyString()));
    }

    @Test
    public void ofReturnsEmptyPathWhenInputIsEmptyArray() {
        UnixPath unixPath = UnixPath.of();
        assertThat(unixPath.toString(), is(emptyString()));
    }

    @Test
    public void fromStringReturnsPathElementsWhenInputIsPathStringWithElementsDividedWithUnixPathDelimiter() {
        String originalPathString = "first/second/third";
        UnixPath unixPath = UnixPath.fromString(originalPathString);
        assertThat(unixPath.toString(), is(equalTo(originalPathString)));
    }

    @Test
    public void fromStringReturnsPathWithRootWhenInputContainsRoot() {
        String originalPathString = "/first/second/third";
        UnixPath unixPath = UnixPath.fromString(originalPathString);
        assertThat(unixPath.toString(), is(equalTo(originalPathString)));
    }

    @Test
    public void addChildReturnsNewUnixPathWithChildPathAppendedToParentPath() {
        String parentPath = "first/second";
        String childPath = "third";
        String grandChildPath = "fourth/fifth";
        UnixPath actualPath = UnixPath.fromString(parentPath).addChild(childPath).addChild(grandChildPath);
        String expectedPath = "first/second/third/fourth/fifth";
        assertThat(actualPath.toString(), is(equalTo(expectedPath)));
    }

    @Test
    public void getParentReturnsParentUnixPathWhenUnixPathIsNotRootOrEmpty() {
        String path = "first/second/file.txt";
        String parent = "first/second";
        UnixPath originalPath = UnixPath.of(path);
        Optional<UnixPath> parentPath = originalPath.getParent();
        assertThat(parentPath.orElseThrow().toString(), is(equalTo(parent)));
    }

    @ParameterizedTest(name = "getParent returns empty optional when Unix Path is empty : \"{0}\"")
    @NullAndEmptySource
    public void getParentReturnsEmptyOptionalWhenUnixPathIsEmpty(String path) {
        UnixPath unixPath = UnixPath.of(path);
        assertThat(unixPath.getParent(), isEmpty());
    }

    @Test
    public void equalsReturnsTrueWhenTwoUnixPathsAreGeneratedFromEquivalentDefinitions() {
        UnixPath left = UnixPath.of("first", "second", "third");
        UnixPath right = UnixPath.fromString("first/second/third");
        assertThat(left, is(equalTo(right)));
        assertThat(left, is(not(sameInstance(right))));
    }

    @Test
    public void equalsReturnsTrueWhenTwoUnixPathsAreGeneratedFromEquivalentStrings() {
        UnixPath left = UnixPath.fromString("first/second/third");
        UnixPath right = UnixPath.fromString("first//second//third");
        assertThat(left, is(equalTo(right)));
        assertThat(left, is(not(sameInstance(right))));
        assertThat(right.toString(), is(equalTo("first/second/third")));
    }

    @Test
    public void equalsReturnsFalseWhenTwoUnixPathsAreNotEqual() {
        UnixPath left = UnixPath.fromString("first/second/third");
        UnixPath right = UnixPath.fromString("first/second");
        assertThat(left, is(not(equalTo(right))));
    }

    @Test
    public void ofReturnsPathIgnoringEmptyStrings() {
        UnixPath left = UnixPath.of("first", EMPTY_STRING, EMPTY_STRING, "second", EMPTY_STRING, "third");
        UnixPath right = UnixPath.of("first", "second", "third");
        assertThat(left, is(equalTo(right)));
        assertThat(left, is(not(sameInstance(right))));
    }

    @Test
    public void ofReturnsPathSplittingStringElementsOnPathSeparator() {
        UnixPath path = UnixPath.of("first/second/", "third/fourth", "fifth");
        assertThat(path.toString(), is(equalTo("first/second/third/fourth/fifth")));
    }

    @Test
    public void isRootReturnsTrueWhenUnixPathIsRoot() {
        UnixPath path = UnixPath.of("/");
        assertThat(path.isRoot(), is(true));
        assertThat(path, is(equalTo(UnixPath.ROOT_PATH)));
    }

    @Test
    public void getParentReturnsRootIfPathHasRootAndParentPathIsRoot() {
        UnixPath path = UnixPath.of("/folder");
        UnixPath parent = path.getParent().orElseThrow();
        assertThat(parent.isRoot(), is(true));
        assertThat(parent, is(equalTo(UnixPath.ROOT_PATH)));
    }

    @ParameterizedTest(name = "getFilename returns ${1} when input is ${0}")
    @CsvSource({
        "/some/existing/folder/, folder",
        "/some/existing/folder/existingFile.ending, existingFile.ending"
    })
    public void getFilenameReturnsTheLastElementOfaUnixPath(String inputPath, String expectedFilename) {
        UnixPath unixPath = UnixPath.of(inputPath);
        assertThat(unixPath.getFilename(), is(equalTo(expectedFilename)));
    }
}