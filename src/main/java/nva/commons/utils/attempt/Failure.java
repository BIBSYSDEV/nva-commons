package nva.commons.utils.attempt;

import java.util.function.Function;
import java.util.stream.Stream;

public class Failure<T> extends Try<T> {

    private final Exception exception;

    public Failure(Exception exception) {
        super();
        this.exception = exception;
    }

    @Override
    public Stream<T> stream() {
        return Stream.empty();
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public T get() {
        throw new IllegalStateException("Result is a failure. Try getting the exception");
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public <S, E extends Exception> Try<S> map(FunctionWithException<T, S, E> action) {
        return new Failure<>(exception);
    }

    @Override
    public <S, E extends Exception> Try<S> flatMap(FunctionWithException<T, Try<S>, E> action) {
        return null;
    }

    @Override
    @SuppressWarnings("PMD.AvoidThrowingNullPointerException")
    public <E extends Exception> T orElseThrow(Function<Try<T>, E> action) throws E {
        if (action != null) {
            throw action.apply(this);
        } else {
            throw new IllegalStateException("Action cannot be null");
        }
    }
}
