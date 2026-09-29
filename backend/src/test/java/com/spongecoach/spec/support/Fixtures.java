package com.spongecoach.spec.support;

import com.spongecoach.domain.AppUser;
import com.spongecoach.domain.Drill;
import com.spongecoach.domain.DrillStatus;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Role;
import com.spongecoach.support.TestData;
import io.quarkiverse.cucumber.ScenarioScope;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.LocalDateTime;
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

    private int drillCount;

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

    /**
     * A painted Avatar on an existing Player, noted as {@code avatar} and {@code avatarVersion};
     * the Player's own cleanup removes it.
     */
    public void avatar(String playerSpokenName) {
        byte[] image = Pngs.painted(64, 64);
        world.note("avatar", image);
        world.note("avatarVersion", testData.setAvatar(world.id(Kind.PLAYER, playerSpokenName), image));
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

    public UUID playerSkill(String spokenName, String color) {
        if (world.knows(Kind.PLAYER_SKILL, spokenName)) {
            return world.id(Kind.PLAYER_SKILL, spokenName);
        }
        var skill = testData.createPlayerSkill(world.uniquify(spokenName), color);
        world.register(Kind.PLAYER_SKILL, spokenName, skill.id, skill.name);
        world.onCleanup(() -> testData.deletePlayerSkill(skill.id));
        return skill.id;
    }

    public UUID playerGoal(String spokenName, String color) {
        if (world.knows(Kind.PLAYER_GOAL, spokenName)) {
            return world.id(Kind.PLAYER_GOAL, spokenName);
        }
        var goal = testData.createPlayerGoal(world.uniquify(spokenName), color);
        world.register(Kind.PLAYER_GOAL, spokenName, goal.id, goal.name);
        world.onCleanup(() -> testData.deletePlayerGoal(goal.id));
        return goal.id;
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

    /**
     * A User of the given Role in the Team, acting as the named Player when one is given. A Player
     * user shares its Player's spoken name, so "Carmela" is both.
     */
    public UUID user(String spokenName, Role role, String playerSpokenName) {
        if (world.knows(Kind.USER, spokenName)) {
            return world.id(Kind.USER, spokenName);
        }
        UUID playerId = playerSpokenName == null ? null : world.id(Kind.PLAYER, playerSpokenName);
        AppUser user = testData.createUser(world.uniquify(spokenName), role, playerId);
        world.register(Kind.USER, spokenName, user.id, user.name);
        world.onCleanup(() -> testData.deleteUser(user.id));
        return user.id;
    }

    /**
     * The actual name a Drill the scenario speaks of goes by, chosen before the Drill exists, so a
     * Given can configure the interpreter for a Drill a later When uploads.
     */
    public String drillName(String spokenName) {
        String key = "drillName:" + spokenName;
        if (!world.noted(key)) {
            world.note(key, world.uniquify(spokenName));
        }
        return world.recall(key);
    }

    /** A Drill in the given state with no interpreter job behind it; see {@link TestData#createDrill}. */
    public UUID drill(String spokenName, DrillStatus status) {
        // Created one after another, each later than the last, so "most recently changed" is stable.
        Drill drill = testData.createDrill(drillName(spokenName), status, Instant.now().plusMillis(drillCount++));
        registerDrill(spokenName, drill.id, drill.name);
        return drill.id;
    }

    /** A Drill the API already deleted; see {@link TestData#softDeleteDrill}. */
    public UUID deletedDrill(String spokenName) {
        UUID id = drill(spokenName, DrillStatus.READY);
        testData.softDeleteDrill(id);
        return id;
    }

    /** A ready Drill whose first photo was removed; see {@link TestData#softDeleteSketch}. */
    public UUID drillWithRemovedSketch(String spokenName) {
        UUID id = drill(spokenName, DrillStatus.READY);
        testData.softDeleteSketch(id, 1);
        return id;
    }

    /** Registers a Drill the API created, and queues its removal. */
    public void registerDrill(String spokenName, UUID drillId, String actualName) {
        world.register(Kind.DRILL, spokenName, drillId, actualName);
        world.onCleanup(() -> testData.deleteDrill(drillId));
    }

    public TestData testData() {
        return testData;
    }
}
