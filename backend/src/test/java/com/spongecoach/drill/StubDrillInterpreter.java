package com.spongecoach.drill;

import com.spongecoach.support.TestScripts;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Replaces the Claude-backed interpreter in the test profile, so the spec suite never calls the
 * API (ADR-0019). By default it answers every Drill with a playable script; a scenario's Given can
 * make it ask first, answer with unplayable scripts, or refuse, for the Drill it names. Plans are
 * keyed by the Drill's actual (uniquified) name, so scenarios never see each other's.
 */
@Alternative
@Priority(1)
@IfBuildProfile("test")
@ApplicationScoped
public class StubDrillInterpreter implements DrillInterpreter {

    private final Map<String, String> questionFirst = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> unplayableLeft = new ConcurrentHashMap<>();
    private final Map<String, Boolean> refused = new ConcurrentHashMap<>();

    /** The first interpretation of this Drill asks {@code question} instead of answering. */
    public void askFirst(String drillName, String question) {
        questionFirst.put(drillName, question);
    }

    /** The next {@code times} scripts for this Drill are unplayable. */
    public void unplayable(String drillName, int times) {
        unplayableLeft.put(drillName, new AtomicInteger(times));
    }

    public void refuse(String drillName) {
        refused.put(drillName, true);
    }

    @Override
    public Interpretation interpret(Input input) {
        String name = input.drillName();
        if (refused.containsKey(name)) {
            throw new DrillUnavailableException("Claude hat die Anfrage abgelehnt.");
        }
        if (input.turns().isEmpty() && questionFirst.containsKey(name)) {
            Interpretation.Question question =
                    new Interpretation.Question("q1", questionFirst.get(name), List.of(), 1, List.of());
            return new Interpretation(Interpretation.Status.NEEDS_INPUT, "Eine Rückfrage.", readings(input),
                    List.of(question), new DrillScript(List.of(), List.of()), "");
        }
        AtomicInteger left = unplayableLeft.get(name);
        if (left != null && left.getAndDecrement() > 0) {
            DrillScript playable = TestScripts.playable("Stufe 1");
            DrillScript.Stage stage = playable.stages().get(0);
            DrillScript.Step step = stage.steps().get(0);
            DrillScript.Step broken = new DrillScript.Step(step.id(), "p9", step.type(), step.path(),
                    step.targetPartId(), step.after(), step.afterEdge(), step.delay(), step.speed(),
                    step.duration(), step.sketch(), step.label());
            DrillScript.Stage brokenStage = new DrillScript.Stage(stage.id(), stage.name(), stage.sketches(),
                    stage.area(), stage.actors(), stage.parts(), stage.props(),
                    List.of(broken, stage.steps().get(1)), stage.repetition());
            return ready(input, new DrillScript(List.of(brokenStage), List.of()), "");
        }
        String lastCoachTurn = input.turns().stream()
                .filter(Turn::fromCoach)
                .reduce((first, second) -> second)
                .map(Turn::text)
                .orElse("");
        return ready(input, TestScripts.playable("Stufe 1"), lastCoachTurn);
    }

    private static Interpretation ready(Input input, DrillScript script, String changeSummary) {
        return new Interpretation(Interpretation.Status.READY, "Skript erstellt.", readings(input), List.of(),
                script, changeSummary);
    }

    /** One cross per sketch, where the stub "read" it. */
    private static List<Interpretation.Reading> readings(Input input) {
        return IntStream.rangeClosed(1, input.sketches().size())
                .mapToObj(sketch -> new Interpretation.Reading(sketch, DrillScript.Area.FULL, "aufrecht", List.of(
                        new Interpretation.Symbol("s" + sketch + "-1", Interpretation.SymbolKind.CROSS, "blau",
                                List.of(new DrillScript.Point(-3, -2)), "", 0.9, "Spielerin"))))
                .toList();
    }
}
