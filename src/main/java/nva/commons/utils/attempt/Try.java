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

    public abstract Stream<T> stream();

    public abstract boolean isSuccess();

    public abstract T get();

    public abstract Exception getException();

    public final boolean isFailure() {
        return !isSuccess();
    }

    public abstract <S, E extends Exception> Try<S> map(FunctionWithException<T, S, E> action);

    public abstract <S, E extends Exception> Try<S> flatMap(FunctionWithException<T, Try<S>, E> action);

    /**
     *  A wrapper for actions that throw checked Expcetions. See {@see https://www.oreilly.com/content/handling-checked-exceptions-in-java-streams/}
     *  Try to perform the action. Any exception will be enclosed in a Failure.
     * @param action a {@link Callable} action that throws or does not throw a checked Exception
     * @param <S> the resultin object
     * @return a new {@link Try} instance
     */
    public static <S> Try<S> attempt(Callable<S> action) {
        try {
            return new Success<>(action.call());
        } catch (Exception e) {
            return new Failure<S>(e);
        }
    }

    public static <T, R, E extends Exception> Function<T, Try<R>> attempt(FunctionWithException<T, R, E> fe) {
        return arg -> {
            try {
                return new Success<R>(fe.apply(arg));
            } catch (Exception e) {
                return new Failure<R>(e);
            }
        };
    }
}
