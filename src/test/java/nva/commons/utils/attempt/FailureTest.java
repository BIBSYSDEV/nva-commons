package nva.commons.utils.attempt;

import static nva.commons.utils.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.exceptions.TestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class FailureTest {

    public static final String EXPECTED_EXCEPTION_MESSAGE = "Expected exception message";
    public static final String NESTED_EXCEPTION_MESSAGE = "Nested exception message";
    public static final String NOT_EXPECTED_MESSAGE = "NotExpectedMessage";
    private static final Integer DEFAULT_VALUE = 100;

    private final Integer sample = 1;

    @Test
    @DisplayName("orElseThrow throws illegalStateException for null argument")
    public void orElseThrowsIllegalStateExceptionForNullArgument() {

        Executable action =
            () -> Try.of(sample)
                     .map(i -> illegalAction(i, NOT_EXPECTED_MESSAGE))
                     .orElseThrow(null);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), is(equalTo(Failure.NULL_ACTION_MESSAGE)));
    }

    @Test
    @DisplayName("orElseThrow throws the specified exception")
    public void orElseThrowsTheSpecifiedException() {
        Executable action =
            () -> Try.of(sample)
                     .map((Integer i) -> illegalAction(i, NESTED_EXCEPTION_MESSAGE))
                     .orElseThrow(f -> new TestException(f.getException(), EXPECTED_EXCEPTION_MESSAGE));

        TestException exception = assertThrows(TestException.class, action);
        assertThat(exception.getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
    }

    @Test
    @DisplayName("orElse returns the specified value")
    public void orElseReturnsTheSpecifiedValue() {

        Integer actual = Try.of(sample)
                            .map((Integer i) -> illegalAction(i, NESTED_EXCEPTION_MESSAGE))
                            .orElse(f -> DEFAULT_VALUE);

        assertThat(actual, is(equalTo(DEFAULT_VALUE)));
    }

    @Test
    @DisplayName("orElse throws exception when the final method throws an exception")
    public void orElseThrowsExceptionWhenTheFinalMethodThrowsAnException() {

        Executable action =
            () -> Try.of(sample)
                     .map((Integer i) -> illegalAction(i, NESTED_EXCEPTION_MESSAGE))
                     .orElse(f -> anotherIllegalAction(EXPECTED_EXCEPTION_MESSAGE));

        TestException exception = assertThrows(TestException.class, action);
        assertThat(exception.getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
    }

    @Test
    @DisplayName("orElse throws IllegalStateException when the input arg is null")
    public void orElseThrowsIllegalStateExceptionWhenTheInputArgumentIsNull() {

        Executable action =
            () -> Try.of(sample)
                     .map((Integer i) -> illegalAction(i, NESTED_EXCEPTION_MESSAGE))
                     .orElse(null);

        assertThrows(IllegalStateException.class, action);
    }

    @Test
    @DisplayName("flatMap returns a failure with the first Exception")
    public void flatMapReturnsAFailureWithTheFirstException() {
        Try<Integer> actual = Try.of(sample)
                                 .map(i -> illegalAction(i, EXPECTED_EXCEPTION_MESSAGE))
                                 .flatMap(this::anotherTry);

        assertTrue(actual.isFailure());
        assertThat(actual.getException().getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
    }

    @Test
    @DisplayName("get throws IllegalStateException")
    public void getThrowsIllegalStateException() {
        Executable action = () -> Try.of(sample).map(i -> illegalAction(i, NOT_EXPECTED_MESSAGE)).get();
        assertThrows(IllegalStateException.class, action);
    }

    @Test
    @DisplayName("stream returns an emptyStream")
    public void streamReturnsAnEmptyStream() {
        List<Integer> list = Try.of(sample).map(i -> illegalAction(i, NOT_EXPECTED_MESSAGE))
                                .stream().collect(Collectors.toList());
        assertThat(list, is(empty()));
    }

    @Test
    @DisplayName("isSuccess returns false")
    public void isSuccessReturnsFalse() {
        boolean actual = Try.of(sample).map(i -> illegalAction(i, NOT_EXPECTED_MESSAGE)).isSuccess();
        assertFalse(actual);
    }

    @Test
    @DisplayName("isFailure returns true")
    public void isFailureReturnsTrue() {
        boolean actual = Try.of(sample).map(i -> illegalAction(i, NOT_EXPECTED_MESSAGE)).isFailure();
        assertTrue(actual);
    }

    private Try<Integer> anotherTry(Integer i) {
        return attempt(() -> i + 1);
    }

    private int illegalAction(Integer i, String exceptionMessage) throws IOException {
        throw new IOException(exceptionMessage);
    }

    private int anotherIllegalAction(String message) throws TestException {
        throw new TestException(message);
    }
}
