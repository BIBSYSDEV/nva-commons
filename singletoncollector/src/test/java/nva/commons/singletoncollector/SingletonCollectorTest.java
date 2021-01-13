package nva.commons.singletoncollector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import nva.commons.attempt.Failure;
import nva.commons.attempt.Success;
import nva.commons.attempt.Try;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class SingletonCollectorTest {

    public static final String ALTERNATIVE = "alternative";
    public static final List<String> TWO_ELEMENT_LIST = List.of("A", "B");
    public static final int TWO = 2;

    @DisplayName("SingletonCollector::collect collects a single element")
    @Test
    void collectReturnsSingleElementWhenSingleElementIsPresentInInputList() {
        String expected = "singleton";
        List<String> input = List.of(expected);
        assertEquals(expected, input.stream().collect(SingletonCollector.collect()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @DisplayName("SingletonCollector::collect throws IllegalStateException when input is empty")
    @Test
    void collectThrowsIllegalStateExceptionWhenInputContainsZeroElements() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> Stream.empty().collect(SingletonCollector.collect())
        );
        String expected = String.format(SingletonCollector.SINGLETON_EXPECTED_ERROR_TEMPLATE, 0);
        assertEquals(expected, exception.getMessage());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @DisplayName("SingletonCollector::collect throws IllegalStateException when input contains more than one element")
    @Test
    void collectThrowsIllegalStateExceptionWhenInputContainsMoreThanOneElements() {
        Executable executable = () -> TWO_ELEMENT_LIST.stream().collect(SingletonCollector.collect());
        IllegalStateException exception = assertThrows(IllegalStateException.class, executable);
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
        String expected = "Something";
        assertEquals(expected, Stream.empty().collect(SingletonCollector.collectOrElse(expected)));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @DisplayName("SingletonCollector::collectOrElse throws IllegalStateException when list contains > 1 element")
    @Test
    void collectOrElseThrowsIllegalStateExceptionWhenInputListContainsMoreThanOneElement() {
        Executable executable = () -> TWO_ELEMENT_LIST.stream().collect(SingletonCollector.collectOrElse(ALTERNATIVE));
        IllegalStateException exception = assertThrows(IllegalStateException.class, executable);
        String expected = String.format(SingletonCollector.SINGLETON_OR_NULL_EXPECTED_ERROR_TEMPLATE, TWO);
        assertEquals(expected, exception.getMessage());
    }

    @DisplayName("SingletonCollector:tryCollect returns Success when input list contains one element")
    @Test
    void tryCollectReturnsSingletonWhenInputListContainsOneElement() {
        String expected = "something";
        List<String> input = Collections.singletonList(expected);
        Try<String> actual = input.stream().collect(SingletonCollector.tryCollect());

        assertThat(actual, is(instanceOf(Success.class)));
        String actualValue = actual.get();
        assertThat(actualValue, is(equalTo(expected)));
    }

    @DisplayName("SingletonCollector:tryCollect contains the value when input list contains one element")
    @Test
    void tryCollectContainsTheValueWhenInputListContainsOneElement() {
        String expected = "something";
        List<String> input = Collections.singletonList(expected);
        Try<String> actual = input.stream().collect(SingletonCollector.tryCollect());

        String actualValue = actual.get();
        assertThat(actualValue, is(equalTo(expected)));
    }

    @DisplayName("SingletonCollector::tryCollect returns Failure when input is empty")
    @Test
    void tryCollectReturnsFailureWhenInputIsEmpty() {
        Try<Object> actual = Stream.empty().collect(SingletonCollector.tryCollect());
        assertThat(actual, is(instanceOf(Failure.class)));
    }

    @DisplayName("SingletonCollector::tryCollect returns Failure when input has more than one items")
    @Test
    void collectOrElseThrowThrowsExceptionWhenInputIsNotSingleton() {
        Try<String> reductionResult = TWO_ELEMENT_LIST.stream().collect(SingletonCollector.tryCollect());
        assertThat(reductionResult, is(instanceOf(Failure.class)));
    }
}
