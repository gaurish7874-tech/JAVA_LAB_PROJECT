package coderev.exception;

/** Base type for all checked CodeRev failures. */
public class CodeRevException extends Exception {
    public CodeRevException(String message) {
        super(message);
    }

    public CodeRevException(String message, Throwable cause) {
        super(message, cause);
    }
}
