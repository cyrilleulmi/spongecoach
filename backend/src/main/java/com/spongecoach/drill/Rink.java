package com.spongecoach.drill;

/**
 * The Swiss small court every Drill is animated on (ADR-0018), in metres. The origin is the centre
 * spot; x runs across the rink (left to right), y along it, with the goals at ±{@link #GOAL_LINE_Y}.
 * A half-rink Stage shows only y ≥ 0, so its goal is the one at +y.
 *
 * <p>Taken from a summary of the swiss unihockey rules, not from the rulebook itself: the official
 * PDFs could not be read when this was written. The frontend mirrors these numbers in
 * {@code rink.ts}; change both together.
 */
public final class Rink {

    public static final double WIDTH = 14.0;
    public static final double LENGTH = 24.0;

    /** Distance from the centre spot to each goal line: the goal stands 2.35 m off the end board. */
    public static final double GOAL_LINE_Y = LENGTH / 2 - 2.35;

    /** How far outside the boards a drawn position may land before it counts as off the rink. */
    public static final double TOLERANCE = 0.5;

    private Rink() {
    }

    public static boolean contains(double x, double y) {
        return Math.abs(x) <= WIDTH / 2 + TOLERANCE && Math.abs(y) <= LENGTH / 2 + TOLERANCE;
    }
}
