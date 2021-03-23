package nva.commons.core.parallel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ParallelMapperTest {

    private static final String MESSAGE_TEMPLATE = "ExpectedMessage:";
    private static final int NUMBER_OF_INPUTS_WITH_TOTAL_FOOTPRINT_LARGER_THAN_AVAILABLE_MEMORY = 10;
    private static final int SIGNIFICANT_PART_OF_AVAILABLE_MEMORY = 3;

    @Test
    public void parallelMapperReturnsFunctionResultsOnSetOfInputs() throws InterruptedException {
        List<Integer> inputs = sampleInputs(1_000);

        ParallelMapper<Integer, String> mapper = new ParallelMapper<>(inputs, this::integerToString).run();
        verifyParallelMapperTransformsInputObjects(mapper, inputs);
    }

    @Test
    public void parallelMapperAcceptsStreamsAsInput() throws InterruptedException {
        List<Integer> inputs = sampleInputs(100);
        ParallelMapper<Integer, String> mapper = new ParallelMapper<>(inputs.stream(), this::integerToString).run();
        verifyParallelMapperTransformsInputObjects(mapper, inputs);
    }

    @Test
    public void parallelMapperReturnsExceptionsForFailedExecutions() throws InterruptedException {
        List<Integer> inputs = sampleInputs(100);
        ParallelMapper<Integer, String> mapper = new ParallelMapper<>(inputs, this::failingIntegerToString).run();

        List<ParallelExecutionException> actualExceptions = mapper.getExceptions();

        List<String> actualExceptionMessages = actualExceptions.stream()
                                                   .map(Throwable::getMessage)
                                                   .collect(Collectors.toList());
        List<String> expectedExceptionMessages = inputs.stream()
                                                     .map(this::constructExpectedMessage)
                                                     .collect(Collectors.toList());
        assertThat(actualExceptionMessages, is(equalTo(expectedExceptionMessages)));
    }

    @Test
    public void parallelMapperReturnsExceptionsContainingTheFailingInputs() throws InterruptedException {
        List<Integer> inputs = sampleInputs(100);
        ParallelMapper<Integer, String> mapper = new ParallelMapper<>(inputs, this::failingIntegerToString).run();

        List<Integer> regeneratedInputs = mapper.getExceptions()
                                              .stream()
                                              .map(ParallelExecutionException::getInput)
                                              .map(input -> (Integer) input)
                                              .collect(Collectors.toList());

        assertThat(regeneratedInputs, is(equalTo(inputs)));
    }

    @Test
    public void parallelMapperThrowsNoExceptionForVeryLargeInputs() throws InterruptedException {
        List<Integer> input = sampleInputs(NUMBER_OF_INPUTS_WITH_TOTAL_FOOTPRINT_LARGER_THAN_AVAILABLE_MEMORY);
        ParallelMapper<Integer, String> mapper =
            new ParallelMapper<>(input, this::processingWithTemporarilyLargeFootPrint, 1);
        List<String> successes = mapper.run().getSuccesses();
        assertThat(successes.size(), is(equalTo(input.size())));
    }

    private List<Integer> sampleInputs(int i) {
        return IntStream.range(0, i).boxed().collect(Collectors.toList());
    }

    private String processingWithTemporarilyLargeFootPrint(Integer input) {
        sleep();
        return input + tenMbString().substring(0, 10);
    }

    private String tenMbString() {
        int freeMemory = (int) Runtime.getRuntime().freeMemory();
        int stringSize = freeMemory / SIGNIFICANT_PART_OF_AVAILABLE_MEMORY;
        char[] hugeString = garbageString(stringSize);
        return String.valueOf(hugeString);
    }

    private char[] garbageString(int stringSize) {
        char[] hugeString = new char[stringSize];
        for (int i = 0; i < stringSize; i++) {
            hugeString[i] = 'a';
        }
        return hugeString;
    }

    private void sleep() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void verifyParallelMapperTransformsInputObjects(ParallelMapper<Integer, String> mapper,
                                                            List<Integer> inputs) {

        List<String> outputs = mapper.getSuccesses();
        List<Integer> regeneratedInputs = outputs.stream().map(Integer::parseInt).collect(Collectors.toList());
        assertThat(regeneratedInputs, is(equalTo(inputs)));
    }

    private String integerToString(Integer input) {
        return input.toString();
    }

    private String failingIntegerToString(Integer input) {
        throw new RuntimeException(exceptionMessage(input));
    }

    private String constructExpectedMessage(Integer input) {
        RuntimeException cause = new RuntimeException(exceptionMessage(input));
        return new ParallelExecutionException(input, cause).getMessage();
    }

    private String exceptionMessage(Integer input) {
        return MESSAGE_TEMPLATE + input;
    }
}