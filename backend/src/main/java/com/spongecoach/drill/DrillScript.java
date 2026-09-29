package com.spongecoach.drill;

import java.util.List;

/**
 * A Drill's animation: its Stages, as one document (ADR-0018). This is what the interpreter
 * produces, what the editor saves, and what {@code drill_script_version.script} stores. Its JSON
 * schema is {@code drill/interpretation.schema.json}; {@link DrillScriptValidator} checks what a
 * schema can't. Positions are {@link Rink} metres.
 *
 * <p>"No value" is an empty string rather than null for the optional references
 * ({@code targetPartId}, {@code after}, {@code nextPartId}), so the model's schema needs no nullable
 * types. Readers treat null the same as empty.
 */
public record DrillScript(List<Stage> stages, List<String> assumptions) {

    /** One continuous animated run. */
    public record Stage(
            String id,
            String name,
            /** Positions (1-based) of the sketches this Stage was read from. */
            List<Integer> sketches,
            Area area,
            List<Actor> actors,
            List<Part> parts,
            List<Prop> props,
            List<Step> steps,
            Repetition repetition) {
    }

    public enum Area {
        FULL,
        HALF
    }

    /** A figure in the animation. Not a Player; keeps its id across Stages. */
    public record Actor(String id, ActorKind kind, Side side, String label, Point start, boolean hasBall) {
    }

    public enum ActorKind {
        PLAYER,
        GOALIE,
        COACH
    }

    public enum Side {
        A,
        B,
        NEUTRAL
    }

    /**
     * What one Actor does in a run. Steps refer to Parts, so a seamless loop can hand a Part to a
     * different Actor: after the run, the Actor playing this Part plays {@code nextPartId}.
     */
    public record Part(String id, String name, String actorId, String nextPartId) {
    }

    public record Prop(String id, PropKind kind, Point position) {
    }

    public enum PropKind {
        CONE,
        POLE,
        SMALL_GOAL,
        OTHER
    }

    /**
     * One action. It starts when the Step {@code after} reaches {@code afterEdge} (its start or end,
     * or {@code afterFraction} of the way through it for {@link Edge#DURING}), plus {@code delay}
     * seconds; with no {@code after} it starts at the beginning of the run. {@code DURING} is how a
     * pass or shot happens while someone is still running.
     *
     * <ul>
     *   <li>{@code RUN}, {@code DRIBBLE}: the Part's Actor moves along {@code path} at {@code speed}.
     *       Who has the ball is independent of the type: a run carries the ball if the Actor has it.
     *   <li>{@code PASS}, {@code SHOT}: the ball travels along {@code path}; a pass hands it to
     *       {@code targetPartId}.
     *   <li>{@code ROAM}: loose back-and-forth movement along {@code path} for {@code duration}.
     *   <li>{@code WAIT}: stands still for {@code duration}.
     * </ul>
     */
    public record Step(
            String id,
            String partId,
            StepType type,
            List<Point> path,
            String targetPartId,
            String after,
            Edge afterEdge,
            /** For {@link Edge#DURING}: how far through {@code after} this Step starts, between 0 and 1 exclusive. */
            double afterFraction,
            double delay,
            double speed,
            double duration,
            /** Position (1-based) of the sketch this Step was drawn on; 0 when it was drawn by hand. */
            int sketch,
            /** The number drawn next to it, if any. */
            String label) {
    }

    public enum StepType {
        RUN,
        DRIBBLE,
        PASS,
        SHOT,
        ROAM,
        WAIT
    }

    public enum Edge {
        START,
        END,
        /** Part-way through the anchor Step, at {@link Step#afterFraction}. */
        DURING
    }

    /**
     * How the run loops: {@code REPLAY} resets and plays again; {@code SEAMLESS} moves every Actor to
     * the start of its next Part and carries on. {@code mirrored} reflects every other run across
     * the rink's long axis (x → −x).
     */
    public record Repetition(RepetitionMode mode, boolean mirrored) {
    }

    public enum RepetitionMode {
        REPLAY,
        SEAMLESS
    }

    public record Point(double x, double y) {
    }
}
