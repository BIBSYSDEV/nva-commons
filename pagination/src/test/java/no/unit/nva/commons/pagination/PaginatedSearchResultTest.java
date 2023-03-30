package no.unit.nva.commons.pagination;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nva.commons.apigateway.exceptions.UnprocessableContentException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaginatedSearchResultTest {

    public static final String QUERY_PARAM_FIELD_NAME = "key";
    public static final String QUERY_PARAM_FIELD_VALUE = "value";
    private static final URI BASE_URI = URI.create("https://localhost");

    @Test
    void shouldPopulateContextIdTotalHitsAndHitsAlways() throws UnprocessableContentException {
        var result = PaginatedSearchResult.create(BASE_URI, 0, 5, 0, Collections.emptyList());

        assertThat(result, doesNotHaveEmptyValuesIgnoringFields(Set.of("nextResults", "previousResults", "hits")));
        assertThat(result.getHits(), emptyIterable());
    }

    @Test
    void shouldPopulateNextResultsWhenMoreHitsAreAvailable() throws UnprocessableContentException {
        var result = PaginatedSearchResult.create(BASE_URI, 0, 1, 2, List.of(randomString()));

        var expectedNextResults = URI.create("https://localhost?offset=1&size=1");
        assertThat(result.getNextResults(), is(equalTo(expectedNextResults)));
        assertThat(result.getPreviousResults(), nullValue());
    }

    @Test
    void shouldPopulatePreviousResultWhenThereArePreviousResults() throws UnprocessableContentException {
        var result = PaginatedSearchResult.create(BASE_URI, 1, 1, 2, List.of(randomString()));

        assertThat(result.getNextResults(), nullValue());

        var expectedPreviousResults = URI.create("https://localhost?offset=0&size=1");
        assertThat(result.getPreviousResults(), is(equalTo(expectedPreviousResults)));
    }

    @Test
    void shouldPopulateBothNextAndPreviousResultWhenApplicable() throws UnprocessableContentException {
        var querySize = 5;
        var hits = generateRandomHits(querySize);
        var result = PaginatedSearchResult.create(BASE_URI, 10, 5, 50, hits);

        var expectedNextResults = URI.create("https://localhost?offset=15&size=5");
        assertThat("nextResults should be at offset 15 with size 5",
                   result.getNextResults(), is(equalTo(expectedNextResults)));

        var expectedPreviousResults = URI.create("https://localhost?offset=5&size=5");
        assertThat("previousResults should be at offset 5 with size 5",
                   result.getPreviousResults(),
                   is(equalTo(expectedPreviousResults)));
    }

    @Test
    void shouldSupportOffsetThatIsNotFullPageSizes() throws UnprocessableContentException {
        var hits = List.of(randomString(), randomString());
        var result = PaginatedSearchResult.create(BASE_URI, 1, 3, 3, hits);

        assertThat(result.getNextResults(), nullValue());

        var expectedPreviousResults = URI.create("https://localhost?offset=0&size=1");
        assertThat(result.getPreviousResults(), is(equalTo(expectedPreviousResults)));
    }

    @Test
    void shouldPopulateNextResultsWithQueryParamsWhenQueryParamsAndMoreHitsAreAvailable()
        throws UnprocessableContentException {
        var queryParams = Map.of(QUERY_PARAM_FIELD_NAME, QUERY_PARAM_FIELD_VALUE, "key2", "value2");
        var result = PaginatedSearchResult.create(BASE_URI, 0, 1, 2, List.of(randomString()), queryParams);

        var expectedNextResults = getUri(queryParams, "1", "1");

        assertThat(result.getNextResults(), is(equalTo(expectedNextResults)));
        assertThat(result.getPreviousResults(), nullValue());
    }

    @Test
    void shouldPopulateBothNextAndPreviousResultWithQueryParamWhenQueryParamWhenApplicable()
        throws UnprocessableContentException {
        var querySize = 5;
        var hits = generateRandomHits(querySize);
        var queryParams = Map.of(QUERY_PARAM_FIELD_NAME, QUERY_PARAM_FIELD_VALUE);
        var result = PaginatedSearchResult.create(BASE_URI, 10, 5, 50, hits, queryParams);

        var expectedNextResults = getUri(Map.of(QUERY_PARAM_FIELD_NAME, QUERY_PARAM_FIELD_VALUE), "15", "5");

        assertThat(result.getNextResults(), is(equalTo(expectedNextResults)));

        var expectedPreviousResults = getUri(Map.of(QUERY_PARAM_FIELD_NAME, QUERY_PARAM_FIELD_VALUE), "5", "5");

        assertThat(result.getPreviousResults(), is(equalTo(expectedPreviousResults)));
    }

    @Test
    void shouldReturnActualLastPageWhenSpecifiedOffsetIsGreaterThanTotalResultPages()
        throws UnprocessableContentException {
        var querySize = 5;
        var hits = generateRandomHits(querySize);
        var queryParams = Map.of(QUERY_PARAM_FIELD_NAME, QUERY_PARAM_FIELD_VALUE);
        var result = PaginatedSearchResult.create(BASE_URI, 100, 5, 50, hits, queryParams);

        assertThat(result.getNextResults(), is(nullValue()));

        var expectedPreviousResults = getUri(Map.of(QUERY_PARAM_FIELD_NAME, QUERY_PARAM_FIELD_VALUE), "45", "5");

        assertThat(result.getPreviousResults(), is(equalTo(expectedPreviousResults)));
    }

    @ParameterizedTest
    @CsvSource({"10,0", "-1,10", "10,-1"})
    void shouldThrowUnprocessableContentExceptionOnNonsenseRequest(int offset, int size) {
        assertThrows(UnprocessableContentException.class,
                     () -> PaginatedSearchResult.create(BASE_URI, offset, size, 50, Collections.emptyList(),
                                                        Collections.emptyMap()));
    }

    private URI getUri(Map<String, String> queryParams, String offset, String size) {
        return UriWrapper.fromUri("https://localhost")
            .addQueryParameters(queryParams)
            .addQueryParameter("offset", offset)
            .addQueryParameter("size", size)
            .getUri();
    }

    private List<String> generateRandomHits(int size) {
        var hits = new ArrayList<String>(size);
        for (int hitNo = 0; hitNo < size; hitNo++) {
            hits.add(randomString());
        }
        return hits;
    }
}
