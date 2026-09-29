package com.spongecoach.drill;

import com.spongecoach.domain.SketchRelation;

import java.util.List;

/**
 * Turns a Drill's sketches and its conversation so far into questions or a script (ADR-0019). The
 * real one calls Claude; the test profile swaps in a stub, so the spec suite never hits the network.
 */
public interface DrillInterpreter {

    /**
     * @throws DrillUnavailableException when the interpreter can't be reached, refuses, or answers
     *     with something that isn't an {@link Interpretation}
     */
    Interpretation interpret(Input input);

    /**
     * Everything the interpreter sees. The first coach turn (the sketches, notes, tags and relation)
     * is built from the fields; {@code turns} is the conversation after it, oldest first.
     */
    record Input(String drillName, List<Sketch> sketches, List<String> tags, SketchRelation relation, List<Turn> turns) {
    }

    /** @param jpeg the downscaled photo, as stored */
    record Sketch(int position, byte[] jpeg, String note) {
    }

    /**
     * One turn after the upload. A coach turn is plain text (answers, a chat message, or the
     * validator's complaint); an interpreter turn is its earlier answer as JSON.
     */
    record Turn(boolean fromCoach, String text) {
    }
}
