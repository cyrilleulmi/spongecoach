package com.spongecoach.drill;

import java.util.List;

/**
 * What the interpreter answers (ADR-0019): either Clarifying questions, or a script ready to play.
 * Its JSON schema is {@code drill/interpretation.schema.json}.
 *
 * @param message a short note to the coach, in German, shown in the conversation
 * @param readings what the interpreter read on each sketch; empty when it did not re-read them
 * @param script with {@code NEEDS_INPUT}, may be empty; with {@code READY}, the new version
 * @param changeSummary what changed against the previous version; empty for a first version
 */
public record Interpretation(
        Status status,
        String message,
        List<Reading> readings,
        List<Question> questions,
        DrillScript script,
        String changeSummary) {

    public enum Status {
        NEEDS_INPUT,
        READY
    }

    /** The symbols on one sketch, positioned on the rink, so a coach can see what was misread. */
    public record Reading(int sketch, DrillScript.Area boardSide, String orientation, List<Symbol> symbols) {
    }

    public record Symbol(
            String id,
            SymbolKind kind,
            String color,
            List<DrillScript.Point> points,
            String label,
            double confidence,
            /** What the interpreter takes it to mean, in German. */
            String meaning) {
    }

    public enum SymbolKind {
        CROSS,
        GOALIE_MARK,
        CIRCLE,
        SQUARE,
        DOT,
        RUN_LINE,
        DRIBBLE_LINE,
        PASS_LINE,
        SHOT_LINE,
        MOVEMENT_ARROW,
        NUMBER,
        TEXT,
        OTHER
    }

    /**
     * @param sketch the sketch it is about, 1-based; 0 for the whole Drill
     * @param symbolIds the symbols it is about, for highlighting
     */
    public record Question(String id, String text, List<String> options, int sketch, List<String> symbolIds) {
    }
}
