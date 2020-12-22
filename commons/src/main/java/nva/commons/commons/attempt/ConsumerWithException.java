package nva.commons.commons.attempt;

@FunctionalInterface
public interface ConsumerWithException<T, E extends Exception> {

    void consume(T t) throws E;
}

