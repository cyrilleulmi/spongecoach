package com.spongecoach.support;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spongecoach.drill.DrillJson;
import com.spongecoach.drill.DrillScript;
import com.spongecoach.drill.DrillScript.Actor;
import com.spongecoach.drill.DrillScript.ActorKind;
import com.spongecoach.drill.DrillScript.Area;
import com.spongecoach.drill.DrillScript.Edge;
import com.spongecoach.drill.DrillScript.Part;
import com.spongecoach.drill.DrillScript.Point;
import com.spongecoach.drill.DrillScript.Repetition;
import com.spongecoach.drill.DrillScript.RepetitionMode;
import com.spongecoach.drill.DrillScript.Side;
import com.spongecoach.drill.DrillScript.Stage;
import com.spongecoach.drill.DrillScript.Step;
import com.spongecoach.drill.DrillScript.StepType;

import java.util.List;

/**
 * Drill scripts for tests: a small playable one (a pass, then a shot), and the ways of breaking it
 * that the validator must catch.
 */
public final class TestScripts {

    private TestScripts() {
    }

    /** One Stage on the full rink: A1 passes to A2, who shoots at the top goal. */
    public static DrillScript playable(String stageName) {
        return new DrillScript(List.of(new Stage(
                "s1",
                stageName,
                List.of(1),
                Area.FULL,
                List.of(
                        new Actor("a1", ActorKind.PLAYER, Side.A, "A1", new Point(-3, -2), true),
                        new Actor("a2", ActorKind.PLAYER, Side.A, "A2", new Point(3, 2), false)),
                List.of(new Part("p1", "Passgeberin", "a1", ""), new Part("p2", "Schützin", "a2", "")),
                List.of(),
                List.of(
                        new Step("st1", "p1", StepType.PASS, List.of(new Point(3, 2)), "p2", "", Edge.END, 0, 0, 12, 0, 1, "1"),
                        new Step("st2", "p2", StepType.SHOT, List.of(new Point(0, 9.65)), "", "st1", Edge.END, 0, 0.2, 25, 0, 1, "2")),
                new Repetition(RepetitionMode.REPLAY, false))),
                List.of());
    }

    /** The same run drawn by hand: on no sketch, so every reference to one is 0 (ADR-0020). */
    public static DrillScript drawn(String stageName) {
        Stage stage = playable(stageName).stages().get(0);
        List<Step> steps = stage.steps().stream()
                .map(step -> new Step(step.id(), step.partId(), step.type(), step.path(), step.targetPartId(),
                        step.after(), step.afterEdge(), step.afterFraction(), step.delay(), step.speed(),
                        step.duration(), 0, step.label()))
                .toList();
        return new DrillScript(List.of(new Stage(stage.id(), stage.name(), List.of(), stage.area(), stage.actors(),
                stage.parts(), stage.props(), steps, stage.repetition())), List.of());
    }

    /** The playable script as JSON, broken in the way a hand-edit scenario names. */
    public static ObjectNode broken(String problem) {
        ObjectNode script = DrillJson.mapper().valueToTree(playable("Stufe 1"));
        ObjectNode stage = (ObjectNode) script.withArray("stages").get(0);
        ArrayNode steps = stage.withArray("steps");
        switch (problem) {
            case "a step for a part that does not exist" -> ((ObjectNode) steps.get(0)).put("partId", "p9");
            case "a position off the rink" ->
                    ((ObjectNode) stage.withArray("actors").get(0).get("start")).put("x", 30);
            case "a field the format does not know" -> script.put("tempo", "schnell");
            case "steps waiting for each other in a cycle" -> ((ObjectNode) steps.get(0)).put("after", "st2");
            case "a seamless loop that drops a part" -> {
                ((ObjectNode) stage.get("repetition")).put("mode", "SEAMLESS");
                ((ObjectNode) stage.withArray("parts").get(0)).put("nextPartId", "p2");
            }
            case "a during step with a fraction of 1" -> during(steps, 1);
            case "a during step with a fraction of 0" -> during(steps, 0);
            case "a during step with no step to happen during" -> {
                during(steps, 0.5);
                ((ObjectNode) steps.get(1)).put("after", "");
            }
            default -> throw new IllegalArgumentException("no broken script for: " + problem);
        }
        return script;
    }

    /** The playable script, changed the way a valid variant scenario names, named "Handarbeit". */
    public static ObjectNode variant(String what) {
        ObjectNode script = DrillJson.mapper().valueToTree(playable("Handarbeit"));
        ArrayNode steps = script.withArray("stages").get(0).withArray("steps");
        switch (what) {
            case "a step drawn by hand" -> ((ObjectNode) steps.get(0)).put("sketch", 0);
            case "a step during another step" -> during(steps, 0.5);
            default -> throw new IllegalArgumentException("no script variant for: " + what);
        }
        return script;
    }

    /** The shot starts part-way through the pass, as when someone shoots while a runner is still running. */
    private static void during(ArrayNode steps, double fraction) {
        ObjectNode shot = (ObjectNode) steps.get(1);
        shot.put("afterEdge", "DURING");
        shot.put("afterFraction", fraction);
    }
}
