package nva.commons.utils.attempt;

import static nva.commons.utils.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class TryTest {

    private static final String SOME_STRING = "SomeString";
    private static final String EXCEPTION_MESSAGE = "ExceptionMessage";
    private static Map<Integer, String> numbers;

    static {
        numbers = new HashMap<>();
        numbers.put(1, "one");
        numbers.put(2, "two");
        numbers.put(3, "three");
        numbers.put(4, "four");
        numbers.put(5, "five");
        numbers.put(6, "six");
        numbers.put(7, "seven");
        numbers.put(8, "eight");
        numbers.put(9, "nine");
        numbers.put(0, "zero");
    }

    @Test
    public void attemptASuccessfulActionsReturnsSuccess() {
        Try<Integer> attempt = attempt(() -> divide(6, 3));
        assertThat(attempt.isSuccess(), is(equalTo(true)));
    }

    @Test
    public void attemptAFailingActionThrowsException() {
        Try<Integer> attempt = attempt(() -> divide(6, 0));
        assertThat(attempt.isSuccess(), is(equalTo(false)));
        assertThat(attempt.isFailure(), is(equalTo(true)));
        Exception e = attempt.getException();
        assertTrue(e instanceof ArithmeticException);
    }

    @Test
    public void mapReturnsAMappableResult() {
        Try<String> attempt = attempt(() -> divide(6, 3))
            .map(res -> numbers.get(res))
            .map(String::toUpperCase);
        assertThat(attempt.get(), is(equalTo("TWO")));
    }

    @Test
    public void mapReportsTheFirstException() {
        Try<String> attempt = attempt(() -> divide(6, 0))
            .map(res -> numbers.get(res))
            .map(String::toUpperCase);

        assertThat(attempt.isFailure(), is(true));
        assertThat(attempt.getException().getClass(), is(equalTo(ArithmeticException.class)));
    }

    @Test
    public void flatMapFlattensNestedTry() {
        Function<String, Try<String>> fun1 = attempt(s -> s.toUpperCase());
        String input = "input";
        Try<String> nestedTry = attempt(() -> input).flatMap(s -> fun1.apply(s));
        assertThat(nestedTry.get(), is(equalTo(input.toUpperCase())));
    }

    @Test
    public void attemptReturnsFailureWhenCheckedExceptionIsThrown() {
        Optional<Exception> exception = Stream.of(SOME_STRING).map(attempt(this::throwCheckedException))
                                              .filter(Try::isFailure)
                                              .map(Try::getException)
                                              .findFirst();
        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), is(equalTo(IOException.class)));
    }

    @Test
    public void attemptReturnsFailureWhenNonCheckedExceptionIsThrown() {
        Optional<Exception> exception = Stream.of(SOME_STRING).map(attempt(this::throwUnCheckedException))
                                              .filter(Try::isFailure)
                                              .map(Try::getException)
                                              .findFirst();
        assertThat(exception.isPresent(), is(true));
        assertThat(exception.get().getClass(), is(equalTo(RuntimeException.class)));
    }

    @Test
    public void failurePropagatesErrorThroughTheWholeChain() {
        int someInt = 2;
        Try<String> results = Stream.of(someInt)
                                    .map(i -> numbers.get(i))
                                    .map(String::toUpperCase)
                                    .map(attempt(this::throwCheckedException))
                                    .map(att -> att.map(s -> s.replaceAll("o", "cc")))
                                    .map(att -> att.map(String::trim))
                                    .filter(Try::isFailure)
                                    .findFirst().orElse(null);
        Exception exception = results.getException();
        assertThat(exception.getClass(), is(equalTo(IOException.class)));
        assertThat(exception.getMessage(), is(equalTo(EXCEPTION_MESSAGE)));
    }

    private int divide(int x, int y) throws IllegalArgumentException {
        return x / y;
    }

    private String throwCheckedException(String input) throws IOException {
        throw new IOException(EXCEPTION_MESSAGE);
    }

    private String throwUnCheckedException(String input) {
        throw new RuntimeException(EXCEPTION_MESSAGE);
    }
}
