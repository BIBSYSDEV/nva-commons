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
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;

/**
 * {@link ParallelMapper} is a mapper for processing multiple items where the processing of each input item uses a lot
 * of time but few resources (e.g. map a collections of URLs to the content that they are referencing). For other cases
 * the parallel streams of Java are a better solution.
 *
 * <p>{@code ParallelMapper} takes as input a {@link Collection} of inputs of class I, and a mapping function
 * {@link Function} that maps an object I to an object O. When {@link ParallelMapper#map()} is called, it applies the
 * mapping function to all the Collection items. The successful mappings are available thought the method {@link
 * ParallelMapper#getSuccesses()} and the failures through the method {@link ParallelMapper#getExceptions()}.
 *
 * <p>The class provides also a {@link ParallelMapper#getCompleted()} method for the mappings that completed either
 * successfully or unsuccessfully. For more details about this check {@link Future#isDone()}.
 *
 * <p>Example:
 * <pre>
 *      List&#60;URI&#62; inputs = someUris();
 *      ParallelMapper &#60;URI,String&#62;  mapper = new ParallelMapper &#60;&#62;(inputs,uri-&#62;dereference(URI));
 *      mapper.map();
 *      List&#60;Strings&#62; results = mapper.getSuccesses();
 * </pre>
 *
 * <p>{@link ParallelMapper} accepts also another parameter: batchSize. This indicates the size of each batch that is
 * going to be processed in parallel. The batches are executed sequentially one after the other and the
 * items inside the batch are processed in parallel.
 *
 * <p>For example, if the input has 10.000 elements and the batch size is 1.000, all 1.000 elements of the first batch
 * are going to be processed in parallel, and then the second batch is going to be executed, etc. {@code ParallelMapper}
 * creates one execution thread per item in a batch and therefore it not recommended when the
 * mapping function is trivial.
 *
 * @param <I> the class of the input objects.
 * @param <O> the class of the output objects.
 */
@SuppressWarnings("PMD.DoNotUseThreads")
public class ParallelMapper<I, O> {

    public static final int DEFAULT_BATCH_SIZE = 100;
    private final List<Callable<O>> actions;
    private final int batchSize;
    private final List<Future<O>> futures;
    private final Function<I, O> mappingFunction;

    public ParallelMapper(Collection<I> inputs, Function<I, O> function) {
        this(inputs.stream().parallel(), function, DEFAULT_BATCH_SIZE);
    }

    public ParallelMapper(Collection<I> inputs, Function<I, O> mappingFunction, int batchSize) {
        this(inputs.stream().parallel(), mappingFunction, batchSize);
    }

    public ParallelMapper(Stream<I> inputs, Function<I, O> mappingFunction) {
        this(inputs, mappingFunction, DEFAULT_BATCH_SIZE);
    }

    public ParallelMapper(Stream<I> inputs, Function<I, O> mappingFunction, int batchSize) {
        this.mappingFunction = mappingFunction;
        actions = inputs.map(this::toCallable).collect(Collectors.toList());
        this.batchSize = batchSize;
        futures = new ArrayList<>();
    }

    public ParallelMapper<I, O> map() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        for (int index = 0; index < actions.size(); index += batchSize) {
            executeBatch(executor, index);
        }
        return this;
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
                   .map(this::getExceptionWithInputObject)
                   .map(exception -> (ParallelExecutionException) exception)
                   .collect(Collectors.toList());
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

    private Throwable getExceptionWithInputObject(Exception exception) {
        return exception.getCause();
    }

    private Callable<O> toCallable(I input) {
        return () -> attempt(() -> mappingFunction.apply(input))
                         .orElseThrow(fail -> captureAllExceptionsAndAddInputObject(input, fail));
    }

    private ParallelExecutionException captureAllExceptionsAndAddInputObject(I input, Failure<O> fail) {
        return new ParallelExecutionException(input, fail.getException());
    }
}
