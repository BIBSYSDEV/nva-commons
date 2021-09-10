package nva.commons.doi;

public class InvalidDoiException extends RuntimeException {

    public InvalidDoiException(String doi) {
        super(doi);
    }
}
