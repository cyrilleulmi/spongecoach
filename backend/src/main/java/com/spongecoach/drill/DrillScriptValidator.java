package com.spongecoach.drill;

import com.spongecoach.drill.DrillScript.Actor;
import com.spongecoach.drill.DrillScript.Part;
import com.spongecoach.drill.DrillScript.Point;
import com.spongecoach.drill.DrillScript.Prop;
import com.spongecoach.drill.DrillScript.RepetitionMode;
import com.spongecoach.drill.DrillScript.Stage;
import com.spongecoach.drill.DrillScript.Step;
import com.spongecoach.drill.DrillScript.StepType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks what the JSON schema cannot (ADR-0018): references resolve, positions lie on the rink,
 * speeds and durations make sense, the step order has no cycle, a seamless loop hands every Part on,
 * and an Actor keeps its kind and side across Stages. Returns every problem at once, so the
 * interpreter can fix them all in its one retry (ADR-0019).
 */
public final class DrillScriptValidator {

    /** Faster than any real shot; anything above is a typo. */
    static final double MAX_SPEED = 40;

    private DrillScriptValidator() {
    }

    /** @param sketchCount how many sketches the Drill has, for checking sketch references */
    public static List<String> validate(DrillScript script, int sketchCount) {
        List<String> errors = new ArrayList<>();
        if (script == null || script.stages() == null || script.stages().isEmpty()) {
            errors.add("script: needs at least one stage");
            return errors;
        }
        Set<String> stageIds = new HashSet<>();
        Map<String, Actor> actorsById = new HashMap<>();
        for (Stage stage : script.stages()) {
            String where = "stage " + stage.id();
            if (blank(stage.id()) || !stageIds.add(stage.id())) {
                errors.add(where + ": id must be present and unique");
            }
            if (stage.area() == null || stage.repetition() == null || stage.repetition().mode() == null) {
                errors.add(where + ": area and repetition mode are required");
            }
            for (Integer sketch : nonNull(stage.sketches())) {
                checkSketch(errors, where, sketch == null ? 0 : sketch, sketchCount);
            }
            validateStage(errors, where, stage, sketchCount);
            for (Actor actor : nonNull(stage.actors())) {
                Actor earlier = actorsById.putIfAbsent(actor.id(), actor);
                if (earlier != null && (earlier.kind() != actor.kind() || earlier.side() != actor.side())) {
                    errors.add(where + ": actor " + actor.id() + " changes its kind or side from an earlier stage");
                }
            }
        }
        return errors;
    }

    private static void validateStage(List<String> errors, String where, Stage stage, int sketchCount) {
        Set<String> actorIds = new HashSet<>();
        for (Actor actor : nonNull(stage.actors())) {
            if (blank(actor.id()) || !actorIds.add(actor.id())) {
                errors.add(where + ": actor id " + actor.id() + " must be present and unique");
            }
            if (actor.kind() == null || actor.side() == null) {
                errors.add(where + ": actor " + actor.id() + " needs a kind and a side");
            }
            checkPoint(errors, where + ", actor " + actor.id() + " start", actor.start());
        }

        Map<String, Part> partsById = new HashMap<>();
        Set<String> castActors = new HashSet<>();
        for (Part part : nonNull(stage.parts())) {
            if (blank(part.id()) || partsById.putIfAbsent(part.id(), part) != null) {
                errors.add(where + ": part id " + part.id() + " must be present and unique");
            }
            if (!actorIds.contains(part.actorId())) {
                errors.add(where + ": part " + part.id() + " names unknown actor " + part.actorId());
            } else if (!castActors.add(part.actorId())) {
                errors.add(where + ": actor " + part.actorId() + " plays more than one part");
            }
        }
        for (Part part : nonNull(stage.parts())) {
            if (!blank(part.nextPartId()) && !partsById.containsKey(part.nextPartId())) {
                errors.add(where + ": part " + part.id() + " hands on to unknown part " + part.nextPartId());
            }
        }
        if (stage.repetition() != null && stage.repetition().mode() == RepetitionMode.SEAMLESS) {
            checkHandOver(errors, where, stage.parts(), partsById.keySet());
        }

        Set<String> propIds = new HashSet<>();
        for (Prop prop : nonNull(stage.props())) {
            if (blank(prop.id()) || !propIds.add(prop.id())) {
                errors.add(where + ": prop id " + prop.id() + " must be present and unique");
            }
            checkPoint(errors, where + ", prop " + prop.id(), prop.position());
        }

        Map<String, Step> stepsById = new HashMap<>();
        for (Step step : nonNull(stage.steps())) {
            if (blank(step.id()) || stepsById.putIfAbsent(step.id(), step) != null) {
                errors.add(where + ": step id " + step.id() + " must be present and unique");
            }
        }
        for (Step step : nonNull(stage.steps())) {
            validateStep(errors, where + ", step " + step.id(), step, partsById.keySet(), stepsById, sketchCount);
        }
        checkNoCycle(errors, where, stepsById);
    }

    private static void validateStep(
            List<String> errors, String where, Step step, Set<String> partIds, Map<String, Step> stepsById,
            int sketchCount) {
        if (!partIds.contains(step.partId())) {
            errors.add(where + ": unknown part " + step.partId());
        }
        if (step.type() == null) {
            errors.add(where + ": type is required");
            return;
        }
        List<Point> path = nonNull(step.path());
        switch (step.type()) {
            case RUN, DRIBBLE, PASS, SHOT -> {
                if (path.isEmpty()) {
                    errors.add(where + ": " + step.type() + " needs a path");
                }
                if (!(step.speed() > 0 && step.speed() <= MAX_SPEED)) {
                    errors.add(where + ": speed must be above 0 and at most " + MAX_SPEED + " m/s");
                }
            }
            case ROAM -> {
                if (path.isEmpty()) {
                    errors.add(where + ": ROAM needs a path to move along");
                }
                if (!(step.duration() > 0)) {
                    errors.add(where + ": ROAM needs a duration above 0");
                }
            }
            case WAIT -> {
                if (!(step.duration() > 0)) {
                    errors.add(where + ": WAIT needs a duration above 0");
                }
            }
        }
        if (!blank(step.targetPartId())) {
            if (step.type() != StepType.PASS) {
                errors.add(where + ": only a PASS has a target part");
            } else if (!partIds.contains(step.targetPartId())) {
                errors.add(where + ": passes to unknown part " + step.targetPartId());
            }
        }
        if (!blank(step.after())) {
            if (step.after().equals(step.id())) {
                errors.add(where + ": cannot wait for itself");
            } else if (!stepsById.containsKey(step.after())) {
                errors.add(where + ": waits for unknown step " + step.after());
            }
        }
        if (!(step.delay() >= 0)) {
            errors.add(where + ": delay must not be negative");
        }
        checkSketch(errors, where, step.sketch(), sketchCount);
        for (Point point : path) {
            checkPoint(errors, where + " path", point);
        }
    }

    /** Every Part must be handed on, and no two Parts to the same one: a permutation. */
    private static void checkHandOver(List<String> errors, String where, List<Part> parts, Set<String> partIds) {
        Set<String> targets = new HashSet<>();
        for (Part part : nonNull(parts)) {
            if (blank(part.nextPartId())) {
                errors.add(where + ": seamless repetition needs a next part for part " + part.id());
            } else if (!targets.add(part.nextPartId())) {
                errors.add(where + ": two parts hand on to part " + part.nextPartId());
            }
        }
        if (targets.size() == partIds.size() && !targets.equals(partIds)) {
            errors.add(where + ": seamless repetition must hand every part on to exactly one part");
        }
    }

    private static void checkNoCycle(List<String> errors, String where, Map<String, Step> stepsById) {
        for (Step start : stepsById.values()) {
            Set<String> seen = new HashSet<>();
            Step current = start;
            while (current != null && !blank(current.after())) {
                if (!seen.add(current.id())) {
                    errors.add(where + ": steps wait for each other in a cycle through " + start.id());
                    return;
                }
                current = stepsById.get(current.after());
            }
        }
    }

    private static void checkSketch(List<String> errors, String where, int sketch, int sketchCount) {
        if (sketch < 1 || sketch > sketchCount) {
            errors.add(where + ": sketch " + sketch + " does not exist (the drill has " + sketchCount + ")");
        }
    }

    private static void checkPoint(List<String> errors, String where, Point point) {
        if (point == null) {
            errors.add(where + ": position is required");
        } else if (!Rink.contains(point.x(), point.y())) {
            errors.add(where + ": (" + point.x() + ", " + point.y() + ") is off the rink");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
