package coderev.exception;

/** Wraps database and file failures so upper layers never see SQLException/IOException. */
public class PersistenceException extends CodeRevException {
    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceException(String message) {
        super(message);
    }
}
