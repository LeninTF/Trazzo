package trazzo.back.reports.domain.exception;

public class DuplicateClosureException extends RuntimeException {
    public DuplicateClosureException(int month, int year) {
        super("Monthly closure already exists for " + month + "/" + year);
    }
}
