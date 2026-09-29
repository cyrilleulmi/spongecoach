package com.spongecoach.domain;

/** Where a {@link Drill}'s interpretation stands (ADR-0019). */
public enum DrillStatus {
    /** A job is running; the frontend polls until it ends. */
    PENDING,
    /** The interpreter asked Clarifying questions and waits for the coach's answers. */
    NEEDS_INPUT,
    /** The current script version can be played. */
    READY,
    /** The last job failed; {@link Drill#error} says why. */
    FAILED
}
