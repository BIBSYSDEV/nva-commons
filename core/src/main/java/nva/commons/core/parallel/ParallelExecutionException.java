package nva.commons.core.parallel;

public class ParallelExecutionException extends RuntimeException {

    private final Object input;

    public ParallelExecutionException(Object input, Exception exception) {
        super(exception);
        this.input = input;
    }

    public Object getInput() {
        return input;
    }
}
