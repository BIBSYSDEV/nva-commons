package no.unit.nva.commons.pagination;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import nva.commons.apigateway.exceptions.UnprocessableContentException;
import nva.commons.core.paths.UriWrapper;

@JsonInclude(ALWAYS)
public final class PaginatedSearchResult<T> {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String OFFSET_QUERY_PARAM_NAME = "offset";
    private static final String SIZE_QUERY_PARAM_NAME = "size";
    private static final String AGGREGATIONS_FIELD_NAME = "aggregations";
    private static final String CONTEXT_FIELD_NAME = "@context";
    private static final String ID_FIELD_NAME = "id";
    private static final String TOTAL_HITS_FIELD_NAME = "totalHits";
    private static final String NEXT_RESULTS_FIELD_NAME = "nextResults";
    private static final String PREVIOUS_RESULTS_FIELD_NAME = "previousResults";
    private static final String HITS_FIELD_NAME = "hits";
    private static final String PAGINATED_SEARCH_RESULT_CONTEXT
        = "https://bibsysdev.github.io/src/search/paginated-search-result.json";
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

    @JsonProperty(AGGREGATIONS_FIELD_NAME)
    private final JsonNode aggregations;

    @JsonCreator
    private PaginatedSearchResult(@JsonProperty(ID_FIELD_NAME) URI id,
                                  @JsonProperty(TOTAL_HITS_FIELD_NAME) int totalHits,
                                  @JsonProperty(NEXT_RESULTS_FIELD_NAME) URI nextResults,
                                  @JsonProperty(PREVIOUS_RESULTS_FIELD_NAME) URI previousResults,
                                  @JsonProperty(HITS_FIELD_NAME) List<T> hits,
                                  @JsonProperty(AGGREGATIONS_FIELD_NAME) JsonNode aggregations) {
        this.id = id;
        this.totalHits = totalHits;
        this.nextResults = nextResults;
        this.previousResults = previousResults;
        this.hits = hits;
        this.aggregations = aggregations;
    }

    public static <T> PaginatedSearchResult<T> create(URI baseUri,
                                                      int queryOffset,
                                                      int querySize,
                                                      int totalHits,
                                                      List<T> hits) throws UnprocessableContentException {
        return create(baseUri, queryOffset, querySize, totalHits, hits, Collections.emptyMap());
    }

    public static <T> PaginatedSearchResult<T> create(URI baseUri,
                                                      int queryOffset,
                                                      int querySize,
                                                      int totalHits,
                                                      List<T> hits,
                                                      Map<String, String> queryParameters)
        throws UnprocessableContentException {

        return create(baseUri, queryOffset, querySize, totalHits, hits, queryParameters,
                      OBJECT_MAPPER.createObjectNode());
    }

    public static <T> PaginatedSearchResult<T> create(URI baseUri,
                                                      int queryOffset,
                                                      int querySize,
                                                      int totalHits,
                                                      List<T> hits,
                                                      Map<String, String> queryParameters,
                                                      JsonNode aggregations)
        throws UnprocessableContentException {

        validateOffsetAndSize(queryOffset, querySize);

        var selfUri = generateSelfUri(baseUri, queryOffset, querySize, queryParameters);
        var nextResults = calculateNextResults(queryOffset, querySize, totalHits, hits.size(), baseUri,
                                               queryParameters);
        var previousResults = calculatePreviousResults(queryOffset, totalHits, querySize, baseUri, queryParameters);

        return new PaginatedSearchResult<>(selfUri,
                                           totalHits,
                                           nextResults,
                                           previousResults,
                                           hits,
                                           aggregations);
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

    public JsonNode getAggregations() {
        return aggregations;
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

    private static void validateOffsetAndSize(int offset, int size) throws UnprocessableContentException {
        if (isLessThanZero(offset)) {
            throw new UnprocessableContentException("Unable to process negative offset");
        }
        if (isLessThanOrEqualToZero(size)) {
            throw new UnprocessableContentException("Unable to process size equal to or less than zero");
        }
    }

    private static boolean isLessThanOrEqualToZero(int number) {
        return isLessThanZero(number) || number == 0;
    }

    private static boolean isLessThanZero(int number) {
        return number < 0;
    }
}
