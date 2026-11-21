package coderev.exception;

/** A problem with the same title on the same platform already exists. */
public class DuplicateProblemException extends CodeRevException {
    public DuplicateProblemException(String title, String platform) {
        super("Problem '" + title + "' on " + platform + " already exists.");
    }
}
