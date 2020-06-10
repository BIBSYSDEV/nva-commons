package nva.commons.utils.attempt;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utility class for handling checked exceptions in map functions. Check tests for examples
 *
 * @param <T> the contained object.
 */
public abstract class Try<T> {

    public static final String NULL_ACTION_MESSAGE = "Action cannot be null";

    @SuppressWarnings("PMD.ShortMethodName")
    public static <T> Try<T> of(T input) {
        return new Success<>(input);
    }

    public abstract Stream<T> stream();

    public abstract boolean isSuccess();

    public abstract T get();

    public abstract Exception getException();

    public final boolean isFailure() {
        return !isSuccess();
    }

    public abstract <S, E extends Exception> Try<S> map(FunctionWithException<T, S, E> action);

    public abstract <S, E extends Exception> Try<S> flatMap(FunctionWithException<T, Try<S>, E> action);

    public abstract <E extends Exception> Try<Void> forEach(ConsumerWithException<T, E> consumer);

    public abstract <E extends Exception> T orElseThrow(Function<Failure<T>, E> action) throws E;

    public abstract <E extends Exception> T orElse(FunctionWithException<Failure<T>, T, E> action) throws E;

    /**
     * A wrapper for actions that throw checked Exceptions. See {@see https://www.oreilly.com/content/handling
     * -checked-exceptions-in-java-streams/} Try to perform the action. Any exception will be enclosed in a Failure.
     *
     * @param action a {@link Callable} action that throws or does not throw a checked Exception
     * @param <S>    the resulting object
     * @return a new {@link Try} instance
     */
    public static <S> Try<S> attempt(Callable<S> action) {
        try {
            return new Success<>(action.call());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }

    /**
     * A wrapper for functions that throw checked Exceptions. See {@see https://www.oreilly.com/content/handling
     * -checked-exceptions-in-java-streams/} Try to perform the action. Any exception will be enclosed in a Failure.
     *
     * @param fe  a {@link FunctionWithException} function that throws or does not throw a checked Exception
     * @param <T> the type of the argument of the function.
     * @param <R> the type of the result of the function
     * @param <E> the type of the thrown Exception
     * @return a new {@link Try} instance
     */
    public static <T, R, E extends Exception> Function<T, Try<R>> attempt(FunctionWithException<T, R, E> fe) {
        return arg -> {
            try {
                return new Success<>(fe.apply(arg));
            } catch (Exception e) {
                return new Failure<>(e);
            }
        };
    }
}
