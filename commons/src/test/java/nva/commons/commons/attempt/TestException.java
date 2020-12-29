package nva.commons.commons.attempt;

public class TestException extends Exception {


    public TestException() {
        super();
    }

    public TestException(String message) {
        super(message);
    }

    public TestException(Exception cause,String message) {
        super(message,cause);
    }
}
