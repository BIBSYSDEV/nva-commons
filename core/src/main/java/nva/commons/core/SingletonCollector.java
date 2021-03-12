package nva.commons.core;

import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import nva.commons.core.attempt.Try;

public final class SingletonCollector {

    public static final int SINGLETON = 1;
    public static final int ONLY_ELEMENT = 0;
    public static final String SINGLETON_EXPECTED_ERROR_TEMPLATE = "Expected a single value, but %d were found";
    public static final String SINGLETON_OR_NULL_EXPECTED_ERROR_TEMPLATE
        = "Expected zero or a single value, but %d were found";

    private SingletonCollector() {
    }

    /**
     * A utility to collect and return singletons from lists.
     *
     * @param <T> the type of input elements to the reduction operation.
     * @return a singleton of type T.
     */
    public static <T> Collector<T, ?, T> collect() {
        return Collectors.collectingAndThen(Collectors.toList(), SingletonCollector::get);
    }

    /**
     * Returns the single expected value in the {@link java.util.stream.Stream} or the  alternative if nothing was
     * found. It throws an {@link IllegalStateException} when more than one results have been collected from the {@link
     * java.util.stream.Stream}
     *
     * @param alternative the alterative value if the {@link java.util.stream.Stream} is empty.
     * @param <T>         the class of the collected object.
     * @return the single value of the {@link java.util.stream.Stream} or the alternative if the {@link
     *     java.util.stream.Stream} is empty.
     */
    public static <T> Collector<T, ?, T> collectOrElse(T alternative) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> orElse(list, alternative));
    }

    /**
     * A utility to return a singleton from a list that is expected to contain one and only one item, throwing supplied
     * exception if the list is empty or does not contain one element.
     *
     * @param <T> The type of input elements to the reduction operation.
     * @param <E> The type of the exception to be thrown.
     * @return A type of the singleton.
     */
    public static <T, E extends Exception> Collector<T, ?, Try<T>> tryCollect() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> attempt(() -> get(list)));
    }

    private static <T, E extends Exception> T get(List<T> list) {
        if (list.size() != SINGLETON) {
            throw defaultException(list);
        }
        return list.get(ONLY_ELEMENT);
    }

    private static <T> IllegalStateException defaultException(List<T> list) {
        return new IllegalStateException(String.format(SINGLETON_EXPECTED_ERROR_TEMPLATE, list.size()));
    }

    private static <T> T orElse(List<T> list, T alternative) {
        if (list.size() < SINGLETON) {
            return alternative;
        } else if (list.size() > SINGLETON) {
            throw new IllegalStateException(String.format(SINGLETON_OR_NULL_EXPECTED_ERROR_TEMPLATE, list.size()));
        }
        return list.get(ONLY_ELEMENT);
    }
}
