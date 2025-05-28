package no.unit.nva.identifiers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SortableIdentifierTest {

    public static final int MIN_NUMBER_OF_SHUFFLES = 5;
    public static final int ADDITIONAL_SHUFFLES = 10;
    public static final String DELIMITER = "-";
    public static final SortableIdentifier SAMPLE_IDENTIFIER = SortableIdentifier.next();
    public static final String SAMPLE_CLASS_ID_FIELD = String.format("\"id\" : \"%s\"", SAMPLE_IDENTIFIER);
    public static final String SAMPLE_EXAMPLE_CLASS_JSON = "{" + SAMPLE_CLASS_ID_FIELD + "}";
    public static final String EXAMPLE_HOST = "www.example.org";
    public static final String EXAMPLE_SCHEME = "https";
    public static final String EMPTY_FRAGMENT = null;

    @Test
    public void sortableIdentifierStringContainsSixParts() {
        SortableIdentifier identifier = SortableIdentifier.next();
        String identifierString = identifier.toString();
        String[] identifierParts = identifierString.split(DELIMITER);
        assertThat(identifierParts.length, is(equalTo(6)));
    }

    @Test
    public void sortableIdentifierAcceptsUuidString() {
        UUID oldId = UUID.randomUUID();
        SortableIdentifier identifier = new SortableIdentifier(oldId.toString());
        assertThat(identifier.toString(), is(equalTo(oldId.toString())));
    }

    @Test
    public void sortableIdentifierSerializesAsString() throws JsonProcessingException {
        ExampleClass exampleClass = new ExampleClass();
        exampleClass.setId(SAMPLE_IDENTIFIER);
        String json = JsonUtils.dtoObjectMapper.writeValueAsString(exampleClass);
        assertThat(json, containsString(SAMPLE_CLASS_ID_FIELD));
    }

    @Test
    public void sortableIdentifierDeserializesFromString() throws JsonProcessingException {
        ExampleClass actual = JsonUtils.dtoObjectMapper.readValue(SAMPLE_EXAMPLE_CLASS_JSON, ExampleClass.class);
        SortableIdentifier actualIdentifier = actual.getId();
        assertThat(actualIdentifier, is(equalTo(SAMPLE_IDENTIFIER)));
    }

    @Test
    public void sortableIdentifierIsSortable() throws InterruptedException {
        final Map<SortableIdentifier, Integer> expectedIdentifierOrder = sortableIdentifiersWithExpectedOrder();
        List<SortableIdentifier> idStrings = new ArrayList<>(expectedIdentifierOrder.keySet());
        shuffle(idStrings);
        Collections.sort(idStrings);

        for (int actualIndex = 0; actualIndex < idStrings.size(); actualIndex++) {
            SortableIdentifier idString = idStrings.get(actualIndex);
            int expectedIndex = expectedIdentifierOrder.get(idString);
            assertThat(actualIndex, is(equalTo(expectedIndex)));
        }
    }

    @Test
    public void serializationErrorReturnsMessageWithSortableIdentifierValue() throws IOException {
        SortableIdentifier identifier = SortableIdentifier.next();
        String expectedMessage = "expectedMessage";

        SortableIdentifierSerializer serializer = new SortableIdentifierSerializer();
        IllegalStateException exceptionCause = new IllegalStateException(expectedMessage);

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        doThrow(exceptionCause).when(jsonGenerator).writeString(anyString());

        Executable action = () -> serializer.serialize(identifier, jsonGenerator, null);
        RuntimeException actualException = assertThrows(RuntimeException.class, action);
        Throwable cause = actualException.getCause();
        assertThat(actualException.getMessage(), containsString(identifier.toString()));
        assertThat(actualException.getMessage(),
                   containsString(SortableIdentifierSerializer.SERIALIZATION_EXCEPTION_ERROR));
        assertThat(cause, is(equalTo(exceptionCause)));
    }

    @Test
    public void fromUriReturnsSortableIdentifierWhenUriContainsSortableIdentifierAsLastPartOfItsPath()
        throws URISyntaxException {
        SortableIdentifier identifier = SortableIdentifier.next();
        String path = "/this/is/a/path/after/the/host/" + identifier.toString();
        var sampleUri = new URI(EXAMPLE_SCHEME, EXAMPLE_HOST, path, EMPTY_FRAGMENT);
        SortableIdentifier actualIdentifier = SortableIdentifier.fromUri(sampleUri);
        assertThat(actualIdentifier, is(equalTo(identifier)));
    }

    @Test
    public void fromUriThrowsIllegalArgumentExceptionContainingTheInvalidUriWhenPathDoesNotContainSortableIdentifier()
        throws URISyntaxException {
        String path = "/this/is/a/path/after/the/host";
        var sampleUri = new URI(EXAMPLE_SCHEME, EXAMPLE_HOST, path, EMPTY_FRAGMENT);
        Executable action = () -> SortableIdentifier.fromUri(sampleUri);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(sampleUri.toString()));
    }

    @Test
    public void fromUriThrowsIllegalArgumentExceptionContainingTheInvalidUriWhenThereIsNoPath()
        throws URISyntaxException {
        String path = null;
        var sampleUri = new URI(EXAMPLE_SCHEME, EXAMPLE_HOST, path, EMPTY_FRAGMENT);
        Executable action = () -> SortableIdentifier.fromUri(sampleUri);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(sampleUri.toString()));
    }

    @ParameterizedTest
    @MethodSource("caseSensitiveSortableIdentifierProvider")
    public void shouldIgnoreCaseWhenComparingSortableIdentifiers(SortableIdentifier left, SortableIdentifier right) {
        assertEquals(left, right);
    }

    private static Stream<Arguments> caseSensitiveSortableIdentifierProvider() {
        return Stream.of(
            Arguments.of(
                new SortableIdentifier("f0dd703d-5d08-486f-af8a-f578fc7f6b59"),
                new SortableIdentifier("F0DD703D-5D08-486F-AF8A-F578FC7F6B59")),
            Arguments.of(
                new SortableIdentifier("2F451CDA-08A6-4469-9C26-72051632BB90"),
                new SortableIdentifier("2f451cda-08a6-4469-9c26-72051632bb90")),
            Arguments.of(
                new SortableIdentifier("c36517c5-1357-4c2d-af6d-33a634e82bc2"),
                new SortableIdentifier("c36517c5-1357-4c2d-af6d-33a634e82bc2")),
            Arguments.of(
                new SortableIdentifier("0C443CDB-20C4-45A6-901B-26232773A1B7"),
                new SortableIdentifier("0C443CDB-20C4-45A6-901B-26232773A1B7"))
        );
    }

    private void shuffle(List<SortableIdentifier> idStrings) {
        int numberOfShuffles = MIN_NUMBER_OF_SHUFFLES + new Random().nextInt(ADDITIONAL_SHUFFLES);
        for (int i = 0; i < numberOfShuffles; i++) {
            Collections.shuffle(idStrings);
        }
    }

    private Map<SortableIdentifier, Integer> sortableIdentifiersWithExpectedOrder() throws InterruptedException {
        final Map<SortableIdentifier, Integer> expectedIdentifierIndices = new ConcurrentHashMap<>();
        int sampleSize = 1000;
        for (int index = 0; index < sampleSize; index++) {
            Thread.sleep(2);
            expectedIdentifierIndices.put(SortableIdentifier.next(), index);
        }
        return expectedIdentifierIndices;
    }

    private static class ExampleClass {

        private SortableIdentifier id;

        public SortableIdentifier getId() {
            return id;
        }

        public void setId(SortableIdentifier id) {
            this.id = id;
        }
    }
}
