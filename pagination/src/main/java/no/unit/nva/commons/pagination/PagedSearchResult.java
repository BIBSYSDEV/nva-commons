package no.unit.nva.commons.pagination;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonInclude(ALWAYS)
@JsonPropertyOrder({
    PagedSearchResult.CONTEXT_FIELD_NAME,
    PagedSearchResult.ID_FIELD_NAME,
    PagedSearchResult.SIZE_FIELD_NAME,
    PagedSearchResult.NEXT_RESULTS_FIELD_NAME,
    PagedSearchResult.PREVIOUS_RESULTS_FIELD_NAME,
    PagedSearchResult.HITS_FIELD_NAME})
public class PagedSearchResult<T> {

    protected static final String CONTEXT_FIELD_NAME = "@context";
    protected static final String ID_FIELD_NAME = "id";
    protected static final String SIZE_FIELD_NAME = "size";
    protected static final String NEXT_RESULTS_FIELD_NAME = "nextResults";
    protected static final String PREVIOUS_RESULTS_FIELD_NAME = "previousResults";
    protected static final String HITS_FIELD_NAME = "hits";

    public static final String OFFSET_QUERY_PARAM_NAME = "offset";
    public static final String SIZE_QUERY_PARAM_NAME = "size";

    @JsonProperty(CONTEXT_FIELD_NAME)
    private final URI context;
    @JsonProperty(ID_FIELD_NAME)
    private final URI id;
    @JsonProperty(SIZE_FIELD_NAME)
    private final int totalSize;
    @JsonProperty(NEXT_RESULTS_FIELD_NAME)
    private final URI nextResults;
    @JsonProperty(PREVIOUS_RESULTS_FIELD_NAME)
    private final URI previousResults;
    @JsonProperty(HITS_FIELD_NAME)
    private final List<T> hits;

    public PagedSearchResult(URI context,
                             URI baseUri,
                             int queryOffset,
                             int querySize,
                             int totalSize,
                             List<T> hits) {
        this.context = context;
        this.id = UriWrapper.fromUri(baseUri)
                      .addQueryParameter(OFFSET_QUERY_PARAM_NAME, Integer.toString(queryOffset))
                      .addQueryParameter(SIZE_QUERY_PARAM_NAME, Integer.toString(querySize))
                      .getUri();
        this.nextResults = calculateNextResults(queryOffset, querySize, totalSize, hits.size(), baseUri);
        this.previousResults = calculatePreviousResults(queryOffset, querySize, baseUri);
        this.totalSize = totalSize;
        this.hits = hits;
    }

    @JsonCreator
    @JacocoGenerated
    public PagedSearchResult(@JsonProperty(CONTEXT_FIELD_NAME) URI context,
                             @JsonProperty(ID_FIELD_NAME) URI id,
                             @JsonProperty(SIZE_FIELD_NAME) int totalSize,
                             @JsonProperty(NEXT_RESULTS_FIELD_NAME) URI nextResults,
                             @JsonProperty(PREVIOUS_RESULTS_FIELD_NAME) URI previousResults,
                             @JsonProperty(HITS_FIELD_NAME) List<T> hits) {
        this.context = context;
        this.id = id;
        this.totalSize = totalSize;
        this.nextResults = nextResults;
        this.previousResults = previousResults;
        this.hits = hits;
    }

    public URI getContext() {
        return context;
    }

    public URI getId() {
        return id;
    }

    public int getTotalSize() {
        return totalSize;
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

    private static URI calculateNextResults(int queryOffset, int querySize, int totalSize, int noHits, URI baseUri) {
        if ((queryOffset + noHits) < totalSize) {
            return generateSelfUri(baseUri, queryOffset + querySize, querySize);
        } else {
            return null;
        }
    }

    private static URI calculatePreviousResults(int queryOffset,
                                                int querySize,
                                                URI baseUri) {
        if (queryOffset > 0) {
            var previousOffset = Math.max(0, queryOffset - querySize);
            int previousSize = Math.min(querySize, queryOffset);
            return generateSelfUri(baseUri, previousOffset, previousSize);
        } else {
            return null;
        }
    }

    private static URI generateSelfUri(URI baseUri, int queryOffset, int querySize) {
        return UriWrapper.fromUri(baseUri)
                   .addQueryParameter(OFFSET_QUERY_PARAM_NAME, Integer.toString(queryOffset))
                   .addQueryParameter(SIZE_QUERY_PARAM_NAME, Integer.toString(querySize))
                   .getUri();
    }
}
