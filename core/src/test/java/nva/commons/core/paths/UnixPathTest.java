package nva.commons.core.paths;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.paths.UnixPath.PATH_DELIMITER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.text.IsEmptyString.emptyString;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class UnixPathTest {

    public static final String EMPTY_STRING = "";
    public static final String NULL_STRING = null;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ofReturnsPathWithAllPathElementsInOrderWhenInputArrayIsNotEmpty() {
        UnixPath unixPath = UnixPath.of("first", "second", "third");
        assertThat(unixPath.toString(), is(equalTo("first/second/third")));
    }

    @Test
    void ofReturnsPathWithRootWhenRootIsFirstElement() {
        UnixPath unixPath = UnixPath.of(PATH_DELIMITER, "first", "second", "third");
        assertThat(unixPath.toString(), is(equalTo("/first/second/third")));
    }

    @Test
    void ofReturnsEmptyPathWhenInputIsNull() {
        UnixPath unixPath = UnixPath.of(NULL_STRING);
        System.out.println(unixPath);
        assertThat(unixPath.toString(), is(emptyString()));
    }

    @Test
    void ofReturnsEmptyPathWhenInputIsEmptyArray() {
        UnixPath unixPath = UnixPath.of();
        assertThat(unixPath.toString(), is(emptyString()));
    }

    @Test
    void fromStringReturnsPathElementsWhenInputIsPathStringWithElementsDividedWithUnixPathDelimiter() {
        String originalPathString = "first/second/third";
        UnixPath unixPath = UnixPath.fromString(originalPathString);
        assertThat(unixPath.toString(), is(equalTo(originalPathString)));
    }

    @Test
    void fromStringReturnsPathWithRootWhenInputContainsRoot() {
        String originalPathString = "/first/second/third";
        UnixPath unixPath = UnixPath.fromString(originalPathString);
        assertThat(unixPath.toString(), is(equalTo(originalPathString)));
    }

    @Test
    void addChildReturnsNewUnixPathWithChildPathAppendedToParentPath() {
        String parentPath = "first/second";
        String childPath = "third";
        String grandChildPath = "fourth/fifth";
        UnixPath actualPath = UnixPath.fromString(parentPath).addChild(childPath).addChild(grandChildPath);
        String expectedPath = "first/second/third/fourth/fifth";
        assertThat(actualPath.toString(), is(equalTo(expectedPath)));
    }

    @Test
    void getParentReturnsParentUnixPathWhenUnixPathIsNotRootOrEmpty() {
        String path = "first/second/file.txt";
        String parent = "first/second";
        UnixPath originalPath = UnixPath.of(path);
        Optional<UnixPath> parentPath = originalPath.getParent();
        assertThat(parentPath.orElseThrow().toString(), is(equalTo(parent)));
    }

    @ParameterizedTest(name = "getParent returns empty optional when Unix Path is empty : \"{0}\"")
    @NullAndEmptySource
    void getParentReturnsEmptyOptionalWhenUnixPathIsEmpty(String path) {
        UnixPath unixPath = UnixPath.of(path);
        assertThat(unixPath.getParent(), isEmpty());
    }

    @Test
    void equalsReturnsTrueWhenTwoUnixPathsAreGeneratedFromEquivalentDefinitions() {
        UnixPath left = UnixPath.of("first", "second", "third");
        UnixPath right = UnixPath.fromString("first/second/third");
        assertThat(left, is(equalTo(right)));
        assertThat(left, is(not(sameInstance(right))));
    }

    @Test
    void equalsReturnsTrueWhenTwoUnixPathsAreGeneratedFromEquivalentStrings() {
        UnixPath left = UnixPath.fromString("first/second/third");
        UnixPath right = UnixPath.fromString("first//second//third");
        assertThat(left, is(equalTo(right)));
        assertThat(left, is(not(sameInstance(right))));
        assertThat(right.toString(), is(equalTo("first/second/third")));
    }

    @Test
    void equalsReturnsFalseWhenTwoUnixPathsAreNotEqual() {
        UnixPath left = UnixPath.fromString("first/second/third");
        UnixPath right = UnixPath.fromString("first/second");
        assertThat(left, is(not(equalTo(right))));
    }

    @Test
    void ofReturnsPathIgnoringEmptyStrings() {
        UnixPath left = UnixPath.of("first", EMPTY_STRING, EMPTY_STRING, "second", EMPTY_STRING, "third");
        UnixPath right = UnixPath.of("first", "second", "third");
        assertThat(left, is(equalTo(right)));
        assertThat(left, is(not(sameInstance(right))));
    }

    @Test
    void ofReturnsPathSplittingStringElementsOnPathSeparator() {
        UnixPath path = UnixPath.of("first/second/", "third/fourth", "fifth");
        assertThat(path.toString(), is(equalTo("first/second/third/fourth/fifth")));
    }

    @Test
    void isRootReturnsTrueWhenUnixPathIsRoot() {
        UnixPath path = UnixPath.of("/");
        assertThat(path.isRoot(), is(true));
        assertThat(path, is(equalTo(UnixPath.ROOT_PATH)));
    }

    @Test
    void getParentReturnsRootIfPathHasRootAndParentPathIsRoot() {
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
    void getFilenameReturnsTheLastElementOfaUnixPath(String inputPath, String expectedFilename) {
        UnixPath unixPath = UnixPath.of(inputPath);
        assertThat(unixPath.getLastPathElement(), is(equalTo(expectedFilename)));
    }

    @Test
    void objectMapperSerializesUnixPathAsString() throws JsonProcessingException {
        String unixPath = "/some/folder";

        ClassWithUnixPath classWithUnixPath = new ClassWithUnixPath();
        classWithUnixPath.setField(UnixPath.of(unixPath));
        String jsonString = objectMapper.writeValueAsString(classWithUnixPath);
        JsonNode actualJson = objectMapper.readTree(jsonString);

        ObjectNode expectedJson = objectMapper.createObjectNode();
        expectedJson.put(ClassWithUnixPath.fieldName(), unixPath);

        assertThat(actualJson, is(equalTo(expectedJson)));
    }

    @Test
    void objectMapperReturnsValidUnixPathWhenMappingStringToUnixPath()
        throws JsonProcessingException {
        String expectedPath = "/some/folder";
        ObjectNode json = objectMapper.createObjectNode();
        json.put(ClassWithUnixPath.fieldName(), expectedPath);
        String jsonString = objectMapper.writeValueAsString(json);
        ClassWithUnixPath objectContainingUnixPath =
            objectMapper.readValue(jsonString, ClassWithUnixPath.class);

        assertThat(objectContainingUnixPath.getField().toString(), is(equalTo(expectedPath)));
    }

    @Test
    void addRootAddsRootToNonAbsoluteUnixPath() {
        String pathString = "some/path";
        String expectedPathString = UnixPath.ROOT + pathString;
        String actualPathString = UnixPath.fromString(pathString).addRoot().toString();
        assertThat(actualPathString, is(equalTo(expectedPathString)));
    }

    @Test
    void addRootReturnsSamePathWhenPathIsAbsoluteUnixPath() {
        String expectedPathString = UnixPath.ROOT + "some/path";
        String actualPathString = UnixPath.fromString(expectedPathString).addRoot().toString();
        assertThat(actualPathString, is(equalTo(expectedPathString)));
    }

    @Test
    void removeRootRemovesRootToNonAbsoluteUnixPath() {
        String expectedPathString = "some/path";
        String actualPathString = UnixPath.fromString(UnixPath.ROOT + expectedPathString).removeRoot().toString();
        assertThat(actualPathString, is(equalTo(expectedPathString)));
    }

    @Test
    void removeRootReturnsSamePathWhenUnixPathIsNotAbsolute() {
        String expectedPathString = "some/path";
        String actualPathString = UnixPath.fromString(expectedPathString).removeRoot().toString();
        assertThat(actualPathString, is(equalTo(expectedPathString)));
    }
    
    @Test
    void addChildReturnsPathRemovingRootFromChildThatHasRoot() {
        String parentFolder = "/some/folder";
        String childFolder = "/child";
        String expectedPath = "/some/folder/child";
        String actualPath = UnixPath.of(parentFolder).addChild(childFolder).toString();
        assertThat(actualPath, is(equalTo(expectedPath)));
    }
    
    @Test
    void shouldReturnEmptyTrueWhenInputIsEmptyPath() {
        var empty = UnixPath.EMPTY_PATH;
        assertThat(empty.isEmptyPath(), is(true));
        var emptyToo = UnixPath.of("");
        assertThat(emptyToo.isEmptyPath(), is(true));
    }
    
    @Test
    void shouldReturnEmptyFalseWhenPathIsNotEmptyPath() {
        var nonEmpty = UnixPath.ROOT_PATH;
        assertThat(nonEmpty.isEmptyPath(), is(false));
        var nonEmpty2 = UnixPath.of(randomString());
        assertThat(nonEmpty2.isEmptyPath(), is(false));
        var nonEmpty3 = UnixPath.of(UnixPath.ROOT, randomString(), randomString());
        assertThat(nonEmpty3.isEmptyPath(), is(false));
    }
    
    private static class ClassWithUnixPath {
        
        private UnixPath field;
        
        @JsonIgnore
        public static String fieldName() {
            return "field";
        }

        public UnixPath getField() {
            return field;
        }

        public void setField(UnixPath field) {
            this.field = field;
        }
    }
}