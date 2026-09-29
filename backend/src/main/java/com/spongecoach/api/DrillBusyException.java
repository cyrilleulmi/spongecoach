package com.spongecoach.api;

/** A Drill's interpreter job is still running; only one may run at a time (ADR-0019). Maps to 409 {@code drill_busy}. */
public class DrillBusyException extends RuntimeException {

    public DrillBusyException(String message) {
        super(message);
    }
}
