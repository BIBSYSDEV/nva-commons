package no.unit.nva.commons.pagination;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.IsNull.nullValue;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PagedSearchResultTest {

    private static final URI CONTEXT = randomUri();
    private static final URI BASE_URI = URI.create("https://localhost");

    @Test
    public void shouldPopulateContextIdTotalHitsAndHitsAlways() {
        var result = new PagedSearchResult<>(CONTEXT, BASE_URI, 0, 5, 0, Collections.emptyList());

        assertThat(result.getContext(), is(equalTo(CONTEXT)));
        assertThat(result.getId(), is(URI.create("https://localhost?offset=0&size=5")));
        assertThat(result.getTotalSize(), is(equalTo(0)));
        assertThat(result.getHits(), emptyIterable());
    }

    @Test
    public void shouldNotPopulateNextAndPreviousResultsOnEmptyResult() {
        var result = new PagedSearchResult<>(CONTEXT, BASE_URI, 0, 5, 0, Collections.emptyList());

        assertThat(result.getNextResults(), nullValue());
        assertThat(result.getPreviousResults(), nullValue());
    }

    @Test
    public void shouldPopulateNextResultsWhenMoreHitsAreAvailable() {
        var result = new PagedSearchResult<>(CONTEXT, BASE_URI, 0, 1, 2, List.of(randomString()));

        var expectedNextResults = URI.create("https://localhost?offset=1&size=1");
        assertThat(result.getNextResults(), is(equalTo(expectedNextResults)));
        assertThat(result.getPreviousResults(), nullValue());
    }

    @Test
    public void shouldPopulatePreviousResultWhenThereArePreviousResults() {
        var result = new PagedSearchResult<>(CONTEXT, BASE_URI, 1, 1, 2, List.of(randomString()));

        assertThat(result.getNextResults(), nullValue());

        var expectedPreviousResults = URI.create("https://localhost?offset=0&size=1");
        assertThat(result.getPreviousResults(), is(equalTo(expectedPreviousResults)));
    }

    @Test
    public void shouldPopulateBothNextAndPreviousResultWhenApplicable() {
        var querySize = 5;
        var hits = generateRandomHits(querySize);
        var result = new PagedSearchResult<>(CONTEXT, BASE_URI, 10, 5, 50, hits);

        var expectedNextResults = URI.create("https://localhost?offset=15&size=5");
        assertThat("nextResults should be at offset 15 with size 5",
                   result.getNextResults(), is(equalTo(expectedNextResults)));

        var expectedPreviousResults = URI.create("https://localhost?offset=5&size=5");
        assertThat("previousResults should be at offset 5 with size 5",
                   result.getPreviousResults(),
                   is(equalTo(expectedPreviousResults)));
    }

    @Test
    public void shouldSupportOffsetThatIsNotFullPageSizes() {
        var hits = List.of(randomString(), randomString());
        var result = new PagedSearchResult<>(CONTEXT, BASE_URI, 1, 3, 3, hits);

        assertThat(result.getNextResults(), nullValue());

        var expectedPreviousResults = URI.create("https://localhost?offset=0&size=1");
        assertThat(result.getPreviousResults(), is(equalTo(expectedPreviousResults)));
    }

    private List<String> generateRandomHits(int size) {
        var hits = new ArrayList<String>(size);
        for (int hitNo = 0; hitNo < size; hitNo++) {
            hits.add(randomString());
        }
        return hits;
    }
}
