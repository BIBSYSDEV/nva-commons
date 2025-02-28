package nva.commons.apigateway;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.core.StringUtils;

public record ViewingScope(Set<String> includes, Set<String> excludes) {

    private static final String EMPTY_CLAIM = "null";
    private static final String COMMA = ",";

    public static ViewingScope from(String includesAsString, String excludesAsString) {
        var includes = extractAsSet(includesAsString);
        var excludes = extractAsSet(excludesAsString);
        return new ViewingScope(includes, excludes);
    }

    private static Set<String> extractAsSet(String includesExcludes) {
        return StringUtils.isEmpty(includesExcludes) || EMPTY_CLAIM.equalsIgnoreCase(includesExcludes) ?
                   Collections.emptySet() :
                                              splitToSet(includesExcludes);
    }

    private static Set<String> splitToSet(String string) {
        return Arrays.stream(string.split(COMMA)).map(String::trim).collect(Collectors.toSet());
    }
}
