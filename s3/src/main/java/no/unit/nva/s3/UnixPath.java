package no.unit.nva.s3;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public final class UnixPath {

    public static final UnixPath EMPTY_PATH = new UnixPath(Collections.emptyList());
    private static final String PATH_DELIMITER = "/";
    private static final String ROOT = "/";
    public static final UnixPath ROOT_PATH = UnixPath.of(ROOT);
    private static final String EMPTY_STRING = "";

    private final List<String> path;

    private UnixPath(List<String> path) {
        this.path = path;
    }


    @SuppressWarnings("PMD.ShortMethodName")
    public static UnixPath of(String... path) {
        Stream<String> pathElements = extractAllPathElements(path);
        List<String> pathElementsList = addRootIfPresentInOriginalPath(pathElements, path)
                                            .collect(Collectors.toList());
        return pathIsEmpty(pathElementsList)
                   ? EMPTY_PATH
                   : new UnixPath(pathElementsList);
    }

    @JsonCreator
    public static UnixPath fromString(String childPath) {
        return UnixPath.of(childPath);
    }

    public boolean isRoot() {
        return this.equals(ROOT_PATH);
    }

    public Optional<UnixPath> getParent() {
        return path.size() > 1
                   ? Optional.of(new UnixPath(path.subList(0, lastPathElementIndex())))
                   : Optional.empty();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnixPath)) {
            return false;
        }
        UnixPath unixPath = (UnixPath) o;
        return Objects.equals(path, unixPath.path);
    }

    @JsonValue
    @Override
    public String toString() {
        if (pathIsEmpty(path)) {
            return EMPTY_STRING;
        } else if (ROOT.equals(path.get(0))) {
            return avoidPrintingPathDelimiterTwice();
        } else {
            return toString(path);
        }
    }

    public UnixPath addChild(String childPath) {
        return addChild(UnixPath.of(childPath));
    }

    public UnixPath addChild(UnixPath childPath) {
        ArrayList<String> newPathArray = new ArrayList<>();
        newPathArray.addAll(this.path);
        newPathArray.addAll(childPath.path);
        return new UnixPath(newPathArray);
    }

    public String getFilename() {
        return path.get(lastPathElementIndex());
    }

    private static Stream<String> extractAllPathElements(String[] path) {
        Stream<String> nonNullPathElements = discardNullArrayElements(path);
        return splitInputElementsContainingPathDelimiter(nonNullPathElements);
    }

    private static Stream<String> splitInputElementsContainingPathDelimiter(Stream<String> pathElements) {
        return pathElements
                   .map(UnixPath::splitCompositePathElements)
                   .flatMap(Arrays::stream)
                   .filter(StringUtils::isNotBlank);
    }

    private static Stream<String> discardNullArrayElements(String[] path) {
        return Optional.ofNullable(path)
                   .stream()
                   .flatMap(Arrays::stream)
                   .filter(Objects::nonNull);
    }

    //composite path element is an element of the form /folder1/folder2
    private static String[] splitCompositePathElements(String pathElement) {
        return pathElement.split(PATH_DELIMITER);
    }

    private static Stream<String> addRootIfPresentInOriginalPath(Stream<String> pathElements, String[] path) {
        return pathBeginsWithRoot(path) ? addRoot(pathElements) : pathElements;
    }

    private static Stream<String> addRoot(Stream<String> pathElements) {
        return Stream.concat(Stream.of(ROOT), pathElements);
    }

    private static boolean pathBeginsWithRoot(String[] path) {
        return nonNull(path) && path.length > 0 && nonNull(path[0]) && path[0].startsWith(ROOT);
    }

    private static boolean pathIsEmpty(List<String> path) {
        return Objects.isNull(path) || path.isEmpty();
    }

    private int lastPathElementIndex() {
        return path.size() - 1;
    }

    private String toString(List<String> pathArray) {
        return String.join(PATH_DELIMITER, pathArray);
    }

    private String avoidPrintingPathDelimiterTwice() {
        return ROOT + toString(path.subList(1, path.size()));
    }
}
