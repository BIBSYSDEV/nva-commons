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
    PagedSearchResult.CONTEXT_FIELD_NAME,
    PagedSearchResult.ID_FIELD_NAME,
    PagedSearchResult.TOTAL_HITS_FIELD_NAME,
    PagedSearchResult.NEXT_RESULTS_FIELD_NAME,
    PagedSearchResult.PREVIOUS_RESULTS_FIELD_NAME,
    PagedSearchResult.HITS_FIELD_NAME})
public class PagedSearchResult<T> {

    protected static final String CONTEXT_FIELD_NAME = "@context";
    protected static final String ID_FIELD_NAME = "id";
    protected static final String TOTAL_HITS_FIELD_NAME = "totalHits";
    protected static final String NEXT_RESULTS_FIELD_NAME = "nextResults";
    protected static final String PREVIOUS_RESULTS_FIELD_NAME = "previousResults";
    protected static final String HITS_FIELD_NAME = "hits";

    public static final String OFFSET_QUERY_PARAM_NAME = "offset";
    public static final String SIZE_QUERY_PARAM_NAME = "size";

    @JsonProperty(CONTEXT_FIELD_NAME)
    private final URI context;
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
    public PagedSearchResult(@JsonProperty(CONTEXT_FIELD_NAME) URI context,
                             @JsonProperty(ID_FIELD_NAME) URI id,
                             @JsonProperty(TOTAL_HITS_FIELD_NAME) int totalHits,
                             @JsonProperty(NEXT_RESULTS_FIELD_NAME) URI nextResults,
                             @JsonProperty(PREVIOUS_RESULTS_FIELD_NAME) URI previousResults,
                             @JsonProperty(HITS_FIELD_NAME) List<T> hits) {
        this.context = context;
        this.id = id;
        this.totalHits = totalHits;
        this.nextResults = nextResults;
        this.previousResults = previousResults;
        this.hits = hits;
    }

    public static <T> PagedSearchResult<T> create(URI context,
                             URI baseUri,
                             int queryOffset,
                             int querySize,
                             int totalHits,
                             List<T> hits) {
        return create(context, baseUri, queryOffset, querySize, totalHits, hits, Collections.emptyMap());
    }

    public static <T> PagedSearchResult<T> create(URI context,
                             URI baseUri,
                             int queryOffset,
                             int querySize,
                             int totalHits,
                             List<T> hits,
                             Map<String, String> queryParameters) {

        URI selfUri = generateSelfUri(baseUri, queryOffset, querySize, queryParameters);
        URI nextResults = calculateNextResults(queryOffset, querySize, totalHits, hits.size(), baseUri,
                                               queryParameters);
        URI previousResults = calculatePreviousResults(queryOffset, querySize, baseUri, queryParameters);

        return new PagedSearchResult<>(context, selfUri,
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
                                            int noHits,
                                            URI baseUri,
                                            Map<String, String> queryParams) {
        if ((queryOffset + noHits) < totalHits) {
            return generateSelfUri(baseUri, queryOffset + querySize, querySize, queryParams);
        } else {
            return null;
        }
    }

    private static URI calculatePreviousResults(int queryOffset,
                                                int querySize,
                                                URI baseUri,
                                                Map<String, String> queryParams) {
        if (queryOffset > 0) {
            var previousOffset = Math.max(0, queryOffset - querySize);
            int previousSize = Math.min(querySize, queryOffset);
            return generateSelfUri(baseUri, previousOffset, previousSize, queryParams);
        } else {
            return null;
        }
    }

    private static URI generateSelfUri(URI baseUri, int queryOffset, int querySize, Map<String, String> queryParams) {
        return UriWrapper.fromUri(baseUri)
            .addQueryParameters(queryParams)
            .addQueryParameter(OFFSET_QUERY_PARAM_NAME, Integer.toString(queryOffset))
            .addQueryParameter(SIZE_QUERY_PARAM_NAME, Integer.toString(querySize))
            .getUri();
    }
}
