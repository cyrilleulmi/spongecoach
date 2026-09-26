package com.spongecoach.api;

/** The current User's Role does not allow this write (ADR-0017). Mapped to 403 {@code forbidden}. */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
