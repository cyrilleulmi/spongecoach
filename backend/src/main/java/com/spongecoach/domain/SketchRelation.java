package com.spongecoach.domain;

/** How a {@link Drill}'s sketches relate to each other, as the coach says on upload (ADR-0018). */
public enum SketchRelation {
    /** Each photo is a harder variant, so each starts its own Stage. */
    PROGRESSION,
    /** Each photo is the next phase of the same run, so they share one Stage. */
    CONTINUOUS,
    /** Some of each; the interpreter asks where it's unclear. */
    MIXED
}
