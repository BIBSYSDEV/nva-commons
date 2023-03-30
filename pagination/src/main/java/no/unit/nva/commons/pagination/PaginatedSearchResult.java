package no.unit.nva.commons.pagination;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import nva.commons.core.paths.UriWrapper;

@JsonInclude(ALWAYS)
@JsonPropertyOrder({
    PaginatedSearchResult.CONTEXT_FIELD_NAME,
    PaginatedSearchResult.ID_FIELD_NAME,
    PaginatedSearchResult.TOTAL_HITS_FIELD_NAME,
    PaginatedSearchResult.NEXT_RESULTS_FIELD_NAME,
    PaginatedSearchResult.PREVIOUS_RESULTS_FIELD_NAME,
    PaginatedSearchResult.HITS_FIELD_NAME})
public final class PaginatedSearchResult<T> {

    /* default */ static final String CONTEXT_FIELD_NAME = "@context";
    /* default */ static final String ID_FIELD_NAME = "id";
    /* default */ static final String TOTAL_HITS_FIELD_NAME = "totalHits";
    /* default */ static final String NEXT_RESULTS_FIELD_NAME = "nextResults";
    /* default */ static final String PREVIOUS_RESULTS_FIELD_NAME = "previousResults";
    /* default */ static final String HITS_FIELD_NAME = "hits";

    private static final String PAGINATED_SEARCH_RESULT_CONTEXT
        = "https://bibsysdev.github.io/src/search/paginated-search-result.json";

    public static final String OFFSET_QUERY_PARAM_NAME = "offset";
    public static final String SIZE_QUERY_PARAM_NAME = "size";

    @JsonProperty(CONTEXT_FIELD_NAME)
    private final URI context = URI.create(PAGINATED_SEARCH_RESULT_CONTEXT);
    @JsonProperty(ID_FIELD_NAME)
    private final URI id;
    @JsonProperty(TOTAL_HITS_FIELD_NAME)
    private final int totalHits;
    @JsonProperty(NEXT_RESULTS_FIELD_NAME)
    private final URI nextResults;
    @JsonProperty(PREVIOUS_RESULTS_FIELD_NAME)
    private final URI previousResults;
    @JsonProperty(HITS_FIELD_NAME)
    private final List<T> hits;

    @JsonCreator
    private PaginatedSearchResult(@JsonProperty(ID_FIELD_NAME) URI id,
                                 @JsonProperty(TOTAL_HITS_FIELD_NAME) int totalHits,
                                 @JsonProperty(NEXT_RESULTS_FIELD_NAME) URI nextResults,
                                 @JsonProperty(PREVIOUS_RESULTS_FIELD_NAME) URI previousResults,
                                 @JsonProperty(HITS_FIELD_NAME) List<T> hits) {
        this.id = id;
        this.totalHits = totalHits;
        this.nextResults = nextResults;
        this.previousResults = previousResults;
        this.hits = hits;
    }

    public static <T> PaginatedSearchResult<T> create(URI baseUri,
                                                      int queryOffset,
                                                      int querySize,
                                                      int totalHits,
                                                      List<T> hits) {
        return create(baseUri, queryOffset, querySize, totalHits, hits, Collections.emptyMap());
    }

    public static <T> PaginatedSearchResult<T> create(URI baseUri,
                                                      int queryOffset,
                                                      int querySize,
                                                      int totalHits,
                                                      List<T> hits,
                                                      Map<String, String> queryParameters) {

        var selfUri = generateSelfUri(baseUri, queryOffset, querySize, queryParameters);
        var nextResults = calculateNextResults(queryOffset, querySize, totalHits, hits.size(), baseUri,
                                               queryParameters);
        var previousResults = calculatePreviousResults(queryOffset, totalHits, querySize, baseUri, queryParameters);

        return new PaginatedSearchResult<>(selfUri,
                                           totalHits,
                                           nextResults,
                                           previousResults,
                                           hits);
    }

    public URI getContext() {
        return context;
    }

    public URI getId() {
        return id;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public URI getNextResults() {
        return nextResults;
    }

    public URI getPreviousResults() {
        return previousResults;
    }

    public List<T> getHits() {
        return hits;
    }

    private static URI calculateNextResults(int queryOffset,
                                            int querySize,
                                            int totalHits,
                                            int numberOfHits,
                                            URI baseUri,
                                            Map<String, String> queryParams) {
        return isLastPage(queryOffset, totalHits, numberOfHits)
                   ? null
                   : generateSelfUri(baseUri, queryOffset + querySize, querySize, queryParams);
    }

    private static boolean isLastPage(int queryOffset, int totalHits, int numberOfHits) {
        return (queryOffset + numberOfHits) >= totalHits;
    }

    private static URI calculatePreviousResults(int queryOffset,
                                                int totalHits,
                                                int querySize,
                                                URI baseUri,
                                                Map<String, String> queryParams) {
        return isFirstPage(queryOffset)
                   ? null
                   : generatePreviousResult(queryOffset, totalHits, querySize, baseUri, queryParams);
    }

    private static URI generatePreviousResult(int queryOffset, int totalHits, int querySize, URI baseUri,
                                              Map<String, String> queryParams) {
        var previousOffset = queryOffset < totalHits
                                 ? Math.max(0, queryOffset - querySize)
                                 : calculateActualLastPage(totalHits, querySize);
        var previousSize = Math.min(querySize, queryOffset);
        return generateSelfUri(baseUri, previousOffset, previousSize, queryParams);
    }

    private static int calculateActualLastPage(int totalHits, int querySize) {
        return (totalHits / querySize - 1) * querySize;
    }

    private static boolean isFirstPage(int queryOffset) {
        return queryOffset == 0;
    }

    private static URI generateSelfUri(URI baseUri, int queryOffset, int querySize, Map<String, String> queryParams) {
        return UriWrapper.fromUri(baseUri)
            .addQueryParameters(queryParams)
            .addQueryParameter(OFFSET_QUERY_PARAM_NAME, Integer.toString(queryOffset))
            .addQueryParameter(SIZE_QUERY_PARAM_NAME, Integer.toString(querySize))
            .getUri();
    }
}
