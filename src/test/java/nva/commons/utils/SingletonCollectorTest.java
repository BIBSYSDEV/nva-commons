package nva.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SingletonCollectorTest {

    @DisplayName("SingletonCollector::collect collects a single element")
    @Test
    void collectReturnsSingleElementWhenSingleElementIsPresentInInputList() {
        String expected = "singleton";
        List<String> input = List.of(expected);
        assertEquals(expected, input.stream().collect(SingletonCollector.collect()));
    }

    @DisplayName("SingletonCollector::collect throws IllegalStateException when input is empty")
    @Test
    void collectThrowsIllegalStateExceptionWhenInputContainsZeroElements() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> Stream.empty().collect(SingletonCollector.collect())
        );
        String expected = String.format(SingletonCollector.SINGLETON_EXPECTED_ERROR_TEMPLATE, 0);
        assertEquals(expected, exception.getMessage());
    }

    @DisplayName("SingletonCollector::collect throws IllegalStateException when input contains more than one element")
    @Test
    void collectThrowsIllegalStateExceptionWhenInputContainsMoreThanOneElements() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> List.of("A", "B").stream().collect(SingletonCollector.collect())
        );
        String expected = String.format(SingletonCollector.SINGLETON_EXPECTED_ERROR_TEMPLATE, 2);
        assertEquals(expected, exception.getMessage());
    }

    @DisplayName("SingletonCollector::collectOrElse returns single element from singleton list")
    @Test
    void collectOrElseReturnsSingleElementWhenInputListContainsSingleElement() {
        String expected = "singleton";
        List<String> input = List.of(expected);
        assertEquals(expected, input.stream().collect(SingletonCollector.collectOrElse("Something")));
    }

    @DisplayName("SingletonCollector::collectOrElse returns alternative when input list is empty")
    @Test
    void collectOrElseReturnsAlternativeWhenInputListContainsZeroElements() {
        List<String> input = Collections.emptyList();
        String expected = "Something";
        assertEquals(expected, Stream.empty().collect(SingletonCollector.collectOrElse(expected)));
    }

    @DisplayName("SingletonCollector::collectOrElse throws IllegalStateException when list contains > 1 element")
    @Test
    void collectOrElseThrowsIllegalStateExceptionWhenInputListContainsMoreThanOneElement() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> List.of("A", "B").stream().collect(SingletonCollector.collectOrElse("alternative"))
        );
        String expected = String.format(SingletonCollector.SINGLETON_OR_NULL_EXPECTED_ERROR_TEMPLATE, 2);
        assertEquals(expected, exception.getMessage());
    }
}
