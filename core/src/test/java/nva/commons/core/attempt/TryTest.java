package nva.commons.core.attempt;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class TryTest {

  public static final int PROVOKE_ERROR = 0;
  private static final String SOME_STRING = "SomeString";
  private static final String EXCEPTION_MESSAGE = "ExceptionMessage";
  private static final ArithmeticException SAMPLE_UNCHECKED_EXCEPTION =
      new ArithmeticException(EXCEPTION_MESSAGE);
  private static final Map<Integer, String> NUMBERS;

  static {
    NUMBERS = new HashMap<>();
    NUMBERS.put(1, "one");
    NUMBERS.put(2, "two");
    NUMBERS.put(3, "three");
    NUMBERS.put(4, "four");
    NUMBERS.put(5, "five");
    NUMBERS.put(6, "six");
    NUMBERS.put(7, "seven");
    NUMBERS.put(8, "eight");
    NUMBERS.put(9, "nine");
    NUMBERS.put(0, "zero");
  }

  @Test
  void attemptASuccessfulActionsReturnsSuccess() {
    Try<Integer> attempt = attempt(() -> divide(6, 3));
    assertThat(attempt.isSuccess(), is(equalTo(true)));
  }

  @Test
  void attemptAFailingActionThrowsException() {
    Try<Integer> attempt = attempt(() -> divide(6, 0));
    assertThat(attempt.isSuccess(), is(equalTo(false)));
    assertThat(attempt.isFailure(), is(equalTo(true)));
    assertInstanceOf(ArithmeticException.class, attempt.getException());
  }

  @Test
  void mapReturnsAMappableResult() {
    Try<String> attempt = attempt(() -> divide(6, 3)).map(NUMBERS::get).map(String::toUpperCase);
    assertThat(attempt.get(), is(equalTo("TWO")));
  }

  @Test
  void mapReportsTheFirstException() {
    Try<String> attempt = attempt(() -> divide(6, 0)).map(NUMBERS::get).map(String::toUpperCase);

    assertThat(attempt.isFailure(), is(true));
    assertThat(attempt.getException().getClass(), is(equalTo(ArithmeticException.class)));
  }

  @Test
  void flatMapFlattensNestedTry() {
    Function<String, Try<String>> fun1 = attempt(string -> string.toUpperCase(Locale.ROOT));
    String input = "input";
    Try<String> nestedTry = attempt(() -> input).flatMap(fun1::apply);
    assertThat(nestedTry.get(), is(equalTo(input.toUpperCase(Locale.ROOT))));
  }

  @Test
  void attemptReturnsFailureWhenCheckedExceptionIsThrown() {
    Optional<Exception> exception =
        Stream.of(SOME_STRING)
            .map(attempt(this::throwCheckedException))
            .filter(Try::isFailure)
            .map(Try::getException)
            .findFirst();
    assertThat(exception.isPresent(), is(true));
    assertThat(exception.get().getClass(), is(equalTo(IOException.class)));
  }

  @Test
  void attemptReturnsFailureWhenNonCheckedExceptionIsThrown() {
    Try<String> effort = attempt(() -> throwUnCheckedException("someInput"));
    assertThat(effort.isFailure(), is(true));
    assertThat(
        effort.getException().getClass(), is(equalTo(SAMPLE_UNCHECKED_EXCEPTION.getClass())));
  }

  @Test
  void attemptReturnsFailureAtTheEndOfStreamProcessingWhenUncheckedExceptionIsThrown() {
    Optional<Exception> exception =
        Stream.of(SOME_STRING)
            .map(attempt(this::throwUnCheckedException))
            .filter(Try::isFailure)
            .map(Try::getException)
            .findFirst();
    assertThat(exception.isPresent(), is(true));
    assertThat(exception.get().getClass(), is(equalTo(SAMPLE_UNCHECKED_EXCEPTION.getClass())));
  }

  @Test
  void orElseThrowsSpecifiedExceptionWhenUncheckedExceptionIsThrown() {
    Executable action =
        () ->
            attempt(() -> throwUnCheckedException("Some message"))
                .orElseThrow(fail -> new TestException());
    assertThrows(TestException.class, action);
  }

  @Test
  void
      shouldThrowExceptionDiscardingTheCauseWhenThereIsFailureAndSuppliedWithExpectionInsteadOfFunction() {
    var unexpectedMessage = randomString();
    var expectedMessage = randomString();
    Executable action =
        () ->
            attempt(() -> throwCheckedException(unexpectedMessage))
                .orElseThrow(new RuntimeException(expectedMessage));
    var actualException = assertThrows(RuntimeException.class, action);
    assertThat(actualException.getMessage(), is(equalTo(expectedMessage)));
    assertThat(actualException.getCause(), is(nullValue()));
  }

  @Test
  void shouldReturnValueWhenSuccessfulAndOrElseThrowsWithExceptionIsCalled() {
    var expectedMessage = randomString();
    var actualMessage =
        attempt(() -> expectedMessage).orElseThrow(new RuntimeException(randomString()));

    assertThat(actualMessage, is(equalTo(expectedMessage)));
  }

  @Test
  void failurePropagatesErrorThroughTheWholeChain() {
    int someInt = 2;
    Try<String> results =
        Stream.of(someInt)
            .map(NUMBERS::get)
            .map(String::toUpperCase)
            .map(attempt(this::throwCheckedException))
            .map(att -> att.map(s -> s.replaceAll("o", "cc")))
            .map(att -> att.map(String::trim))
            .filter(Try::isFailure)
            .findFirst()
            .orElse(null);
    Exception exception = results.getException();
    assertThat(exception.getClass(), is(equalTo(IOException.class)));
    assertThat(exception.getMessage(), is(equalTo(EXCEPTION_MESSAGE)));
  }

  @Test
  @DisplayName("flatMap returns a failure with the first Exception")
  void flatMapPropagatesTheFirstException() {
    Integer someInt = 2;
    Try<String> actual =
        Try.of(someInt).map(Object::toString).flatMap(this::throwCheckedExceptionForFlatMap);

    assertTrue(actual.isFailure());
    assertThat(actual.getException().getMessage(), is(IsEqual.equalTo(EXCEPTION_MESSAGE)));
  }

  @Test
  void shouldReturnFistSuccessWhenOrIsCalled() {
    var firstSucceeds =
        attempt(() -> divide(1, 1)).or(() -> divide(2, 1)).or(() -> divide(3, 1)).orElseThrow();
    assertThat(firstSucceeds, is(equalTo(1)));

    var middleSucceeds =
        attempt(() -> divide(1, PROVOKE_ERROR))
            .or(() -> divide(2, 1))
            .or(() -> divide(3, 1))
            .orElseThrow();
    assertThat(middleSucceeds, is(equalTo(2)));

    var finalSucceeds =
        attempt(() -> divide(1, PROVOKE_ERROR))
            .or(() -> divide(2, PROVOKE_ERROR))
            .or(() -> divide(3, 1))
            .orElseThrow();
    assertThat(finalSucceeds, is(equalTo(3)));
  }

  private int divide(int x, int y) {
    return x / y;
  }

  private String throwCheckedException(String ignoredInput) throws IOException {
    throw new IOException(EXCEPTION_MESSAGE);
  }

  private Try<String> throwCheckedExceptionForFlatMap(String ignoredInput) throws IOException {
    throw new IOException(EXCEPTION_MESSAGE);
  }

  private String throwUnCheckedException(String ignoredInput) {
    throw SAMPLE_UNCHECKED_EXCEPTION;
  }
}
