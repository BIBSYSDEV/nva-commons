package nva.commons.core.parallel;

import static java.lang.Math.min;
import static nva.commons.core.attempt.Try.attempt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.attempt.Try;

@SuppressWarnings("PMD.DoNotUseThreads")
public class ParallelMapper<I, O> {

    public static final int DEFAULT_BATCH_SIZE = 100;
    private final List<Callable<O>> actions;
    private final int batchSize;
    private final List<Future<O>> futures;
    private final Function<I, O> function;

    public ParallelMapper(Collection<I> inputs, Function<I, O> function) {
        this(inputs.stream().parallel(), function, DEFAULT_BATCH_SIZE);
    }

    public ParallelMapper(Collection<I> inputs, Function<I, O> function, int batchSize) {
        this(inputs.stream().parallel(), function, batchSize);
    }

    public ParallelMapper(Stream<I> inputs, Function<I, O> function) {
        this(inputs, function, DEFAULT_BATCH_SIZE);
    }

    public ParallelMapper(Stream<I> inputs, Function<I, O> function, int batchSize) {
        this.function = function;
        actions = inputs.map(this::toCallable).collect(Collectors.toList());
        this.batchSize = batchSize;
        futures = new ArrayList<>();
    }

    public ParallelMapper<I, O> run() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int index = 0; index < actions.size(); index += batchSize) {
            executeBatch(executor, index);
        }
        return this;
    }

    private void executeBatch(ExecutorService executor, int index) throws InterruptedException {
        List<Callable<O>> actionsForExecution = actions.subList(index, endIndex(index));
        List<Future<O>> executed = executor.invokeAll(actionsForExecution);
        futures.addAll(executed);
    }

    private int endIndex(int index) {
        return min(actions.size(), index + batchSize);
    }

    private Stream<Try<O>> getCompleted() {
        return futures.stream()
                   .parallel()
                   .filter(Future::isDone)
                   .map(attempt(Future::get));
    }

    public List<O> getSuccesses() {
        return getCompleted()
                   .filter(Try::isSuccess)
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    public List<ParallelExecutionException> getExceptions() {
        return getCompleted()
                   .filter(Try::isFailure)
                   .map(Try::getException)
                   .map(this::getExceptionWithInput)
                   .map(exception -> (ParallelExecutionException) exception)
                   .collect(Collectors.toList());
    }

    private Throwable getExceptionWithInput(Exception exception) {
        return exception.getCause();
    }

    private Callable<O> toCallable(I input) {
        return () -> attempt(() -> function.apply(input))
                         .orElseThrow(fail -> new ParallelExecutionException(input, fail.getException()));
    }
}
