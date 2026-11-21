package coderev.util;

import coderev.exception.ValidationException;

import java.time.LocalDate;

/** Small shared validation helpers used by services and the UI. */
public final class Validators {
    private Validators() {
    }

    public static String requireNonBlank(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " must not be empty.");
        }
        return value.trim();
    }

    public static int requireRange(int value, int min, int max, String fieldName) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException(fieldName + " must be between " + min + " and " + max + ", got " + value + ".");
        }
        return value;
    }

    public static LocalDate requireNotFuture(LocalDate date, String fieldName) throws ValidationException {
        if (date == null) {
            throw new ValidationException(fieldName + " must not be null.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException(fieldName + " cannot be in the future: " + date + ".");
        }
        return date;
    }
}
