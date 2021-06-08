package no.unit.nva.s3;

import static java.util.Objects.nonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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

        List<String> pathElements = Optional.ofNullable(path)
                                        .stream()
                                        .flatMap(Arrays::stream)
                                        .filter(Objects::nonNull)
                                        .map(UnixPath::splitCompositePathElements)
                                        .flatMap(Arrays::stream)
                                        .filter(StringUtils::isNotBlank)
                                        .collect(Collectors.toList());
        pathElements = addRootIfPresentInOriginalPath(pathElements, path);
        return pathIsEmpty(pathElements)
                   ? EMPTY_PATH
                   : new UnixPath(pathElements);
    }

    public static UnixPath fromString(String childPath) {
        return UnixPath.of(childPath);
    }

    public boolean isRoot() {
        return this.equals(ROOT_PATH);
    }

    public Optional<UnixPath> getParent() {
        return path.size() > 1
                   ? Optional.of(new UnixPath(path.subList(0, path.size() - 1)))
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

    public UnixPath addChild(String childPath) {
        UnixPath child = fromString(childPath);
        ArrayList<String> newPathArray = new ArrayList<>();
        newPathArray.addAll(this.path);
        newPathArray.addAll(child.path);
        return new UnixPath(newPathArray);
    }

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

    private String toString(List<String> pathArray) {
        return String.join(PATH_DELIMITER, pathArray);
    }

    private static String[] splitCompositePathElements(String pathElement) {
        return pathElement.split(PATH_DELIMITER);
    }

    private static List<String> addRootIfPresentInOriginalPath(List<String> pathElements, String[] path) {
        boolean pathHasRoot = pathBeginsWithRoot(path);
        return pathHasRoot ? addRoot(pathElements) : pathElements;
    }

    private static List<String> addRoot(List<String> pathElements) {
        List<String> updatedPathElements = new ArrayList<>();
        updatedPathElements.add(ROOT);
        updatedPathElements.addAll(pathElements);
        return updatedPathElements;
    }

    private static boolean pathBeginsWithRoot(String[] path) {
        return nonNull(path) && path.length > 0 && nonNull(path[0]) && path[0].startsWith(ROOT);
    }

    private static boolean pathIsEmpty(List<String> path) {
        return Objects.isNull(path) || path.isEmpty();
    }

    private String avoidPrintingPathDelimiterTwice() {
        return ROOT + toString(path.subList(1, path.size()));
    }
}
