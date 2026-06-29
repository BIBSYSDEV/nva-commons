package nva.commons.apigateway.mediatype;

import java.util.List;
import java.util.Map;

/**
 * Immutable snapshot of the configuration fields from {@link MediaTypeParser}. Passed to {@link
 * ParseContext} at construction time so that ParseContext does not reach into the parser's fields
 * directly (keeping ATFD low for static analysis).
 *
 * <p>Package-private: not part of the public API.
 */
record ParserConfig(
    int maxInputLength,
    int maxListElements,
    int maxParametersPerType,
    boolean lenientWhitespaceAroundEquals,
    boolean acceptEmptyParameters,
    boolean acceptSingleUnquotedProfileString,
    boolean rejectDuplicateParameters,
    boolean rejectUnknownParameters,
    boolean allowObsText,
    boolean enforceRestrictedNames,
    Map<String, ParameterHandler> handlers,
    List<MediaType> allowedTypes) {}
