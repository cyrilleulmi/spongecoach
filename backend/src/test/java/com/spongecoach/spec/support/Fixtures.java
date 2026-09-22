package com.spongecoach.spec.support;

import com.spongecoach.domain.Event;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.Player;
import com.spongecoach.support.TestData;
import io.quarkiverse.cucumber.ScenarioScope;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates the rows a {@code Given} describes: writes them through {@link TestData}, registers them
 * in the {@link ScenarioWorld} under the name the scenario spoke, and queues their removal.
 *
 * <p>Steps call this rather than {@code TestData} directly, so no step has to remember to uniquify
 * a name or to clean up after itself.
 */
@ScenarioScope
public class Fixtures {

    @Inject
    TestData testData;

    @Inject
    ScenarioWorld world;

    public UUID line(String spokenName) {
        if (world.knows(Kind.LINE, spokenName)) {
            return world.id(Kind.LINE, spokenName);
        }
        Line line = testData.createLine(world.uniquify(spokenName));
        world.register(Kind.LINE, spokenName, line.id, line.name);
        world.onCleanup(() -> testData.deleteLine(line.id));
        return line.id;
    }

    /** A Team Player on no Line. */
    public UUID player(String spokenName) {
        if (world.knows(Kind.PLAYER, spokenName)) {
            return world.id(Kind.PLAYER, spokenName);
        }
        Player player = testData.createPlayer(world.uniquify(spokenName));
        world.register(Kind.PLAYER, spokenName, player.id, player.name);
        world.onCleanup(() -> testData.deletePlayer(player.id));
        return player.id;
    }

    /** A Team Player, rostered on the given Line. */
    public UUID playerOn(String lineSpokenName, String spokenName) {
        UUID lineId = line(lineSpokenName);
        UUID playerId = player(spokenName);
        testData.linkPlayer(lineId, playerId);
        return playerId;
    }

    public UUID skill(String spokenName, String color) {
        if (world.knows(Kind.SKILL, spokenName)) {
            return world.id(Kind.SKILL, spokenName);
        }
        var skill = testData.createSkill(world.uniquify(spokenName), color);
        world.register(Kind.SKILL, spokenName, skill.id, skill.name);
        world.onCleanup(() -> testData.deleteSkill(skill.id));
        return skill.id;
    }

    public UUID goal(String spokenName, String color) {
        if (world.knows(Kind.GOAL, spokenName)) {
            return world.id(Kind.GOAL, spokenName);
        }
        var goal = testData.createGoal(world.uniquify(spokenName), color);
        world.register(Kind.GOAL, spokenName, goal.id, goal.name);
        world.onCleanup(() -> testData.deleteGoal(goal.id));
        return goal.id;
    }

    public UUID focus(String spokenName, List<UUID> goalIds) {
        if (world.knows(Kind.FOCUS, spokenName)) {
            return world.id(Kind.FOCUS, spokenName);
        }
        var focus = testData.createFocus(world.uniquify(spokenName), goalIds);
        world.register(Kind.FOCUS, spokenName, focus.id, focus.name);
        world.onCleanup(() -> testData.deleteFocus(focus.id));
        return focus.id;
    }

    /** A Focus derived from one Development goal created alongside it. */
    public UUID focus(String spokenName) {
        UUID goalId = goal(spokenName + " goal", "#c0392b");
        return focus(spokenName, List.of(goalId));
    }

    public UUID iteration(String spokenName) {
        if (world.knows(Kind.ITERATION, spokenName)) {
            return world.id(Kind.ITERATION, spokenName);
        }
        // Positions are plain coach-assigned numbers, not gapless or unique (CONTEXT.md), so a
        // random high one keeps a scenario's Iterations clear of the seeded ones.
        Iteration iteration = testData.createIteration(
                world.uniquify(spokenName), ThreadLocalRandom.current().nextInt(1_000, 1_000_000));
        world.register(Kind.ITERATION, spokenName, iteration.id, iteration.name);
        world.onCleanup(() -> testData.deleteIteration(iteration.id));
        return iteration.id;
    }

    /**
     * An Event of the given type on the given Iteration. Trainings keep the null name they have in
     * production — only Matches are named — so scenarios refer to Events by their spoken name and
     * assertions go by id.
     */
    public UUID event(String iterationSpokenName, String eventTypeName, String spokenName, LocalDateTime scheduledOn) {
        UUID iterationId = iteration(iterationSpokenName);
        String actualName = "Match".equals(eventTypeName) ? world.uniquify(spokenName) : null;
        Event event = testData.createEvent(
                iterationId, testData.eventType(eventTypeName).id, actualName, scheduledOn);
        world.register(Kind.EVENT, spokenName, event.id, actualName);
        // Its Iteration's cleanup takes the Event with it, so nothing extra is queued here.
        return event.id;
    }

    public TestData testData() {
        return testData;
    }
}
