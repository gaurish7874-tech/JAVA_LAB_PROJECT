package coderev.exception;

/** No problem exists for the requested id. */
public class ProblemNotFoundException extends CodeRevException {
    public ProblemNotFoundException(long id) {
        super("No problem found with id " + id + ".");
    }
}
