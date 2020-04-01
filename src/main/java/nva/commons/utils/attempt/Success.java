package nva.commons.utils.attempt;

import java.util.Objects;
import java.util.stream.Stream;

public class Success<T> extends Try<T> {

    public final T value;

    public Success(T value) {
        super();
        this.value = value;
    }

    @Override
    public Stream<T> stream() {
        return Stream.of(value);
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public Exception getException() {
        throw new IllegalStateException("Result is a success, no exception");
    }

    @Override
    public <S, E extends Exception> Try<S> map(FunctionWithException<T, S, E> mapper) {
        Objects.requireNonNull(mapper);
        return attempt(() -> mapper.apply(value));
    }

    @Override
    public <S, E extends Exception> Try<S> flatMap(FunctionWithException<T, Try<S>, E> action) {
        try {
            return action.apply(value);
        } catch (Exception e) {
            return new Failure<S>(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Success<?> success = (Success<?>) o;
        return Objects.equals(value, success.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
