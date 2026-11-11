package com.erd.core.exception;

/**
 * Raised when an imported script cannot become a diagram. The message is shown to the user, and
 * the diagram is left unchanged.
 */
public class DdlImportException extends RuntimeException {

    public DdlImportException(String message) {
        super(message);
    }

}
