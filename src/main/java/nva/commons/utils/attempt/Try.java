package nva.commons.utils.attempt;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class Try<T> {

    public abstract Stream<T> stream();

    public abstract boolean isSuccess();

    public abstract T get();

    public abstract Exception getException();

    public final boolean isFailure() {
        return !isSuccess();
    }

    public abstract <S,E extends Exception> Try<S> map(FunctionWithException<T, S, E> action);

    public abstract <S,E extends Exception> Try<S> flatMap(FunctionWithException<T,Try<S>,E> action);

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
