package coderev.exception;

/** User-supplied data failed validation (blank title, bad date, ...). */
public class ValidationException extends CodeRevException {
    public ValidationException(String message) {
        super(message);
    }
}
