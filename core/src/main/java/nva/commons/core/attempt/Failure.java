package nva.commons.core.attempt;

import java.util.Optional;
import java.util.concurrent.Callable;
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
        return new Failure<S>(exception);
    }

    @Override
    public <S, E extends Exception> Try<S> flatMap(FunctionWithException<T, Try<S>, E> action) {
        return new Failure<S>(exception);
    }

    @Override
    public <E extends Exception> Try<Void> forEach(ConsumerWithException<T, E> consumer) {
        return new Failure<Void>(exception);
    }

    @Override
    public <E extends Exception> T orElseThrow(Function<Failure<T>, E> action) throws E {
        if (action != null) {
            throw action.apply(this);
        } else {
            throw new IllegalStateException(NULL_ACTION_MESSAGE);
        }
    }

    @Override
    public T orElseThrow() {
        if (this.getException() instanceof RuntimeException) {
            throw (RuntimeException) this.getException();
        } else {
            throw new RuntimeException(this.getException());
        }
    }

    @Override
    public <E extends Exception> T orElse(FunctionWithException<Failure<T>, T, E> action) throws E {
        if (action != null) {
            return action.apply(this);
        } else {
            throw new IllegalStateException(NULL_ACTION_MESSAGE);
        }
    }

    @Override
    public <E extends Exception> Optional<T> toOptional(ConsumerWithException<Failure<T>, E> action) throws E {
        action.consume(this);
        return Optional.empty();
    }

    @Override
    public Optional<T> toOptional() {
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("PMD.ShortMethodName")
    public Try<T> or(Callable<T> action) {
        return attempt(action);
    }
}
