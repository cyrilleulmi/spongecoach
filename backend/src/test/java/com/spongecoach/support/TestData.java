package com.spongecoach.support;

import com.spongecoach.domain.AppUser;
import com.spongecoach.domain.AttendanceStatus;
import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Drill;
import com.spongecoach.domain.DrillMessage;
import com.spongecoach.domain.DrillMessageAuthor;
import com.spongecoach.domain.DrillScriptSource;
import com.spongecoach.domain.DrillScriptVersion;
import com.spongecoach.domain.DrillSketch;
import com.spongecoach.domain.DrillStatus;
import com.spongecoach.domain.DrillTag;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.EventAttendance;
import com.spongecoach.domain.EventAttendanceId;
import com.spongecoach.domain.EventLinePlayer;
import com.spongecoach.domain.EventLinePlayerId;
import com.spongecoach.domain.EventType;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.LineFocusEvent;
import com.spongecoach.domain.LineFocusEventId;
import com.spongecoach.domain.LineSkill;
import com.spongecoach.domain.LineSkillId;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.PlayerAvatar;
import com.spongecoach.domain.PlayerDevelopmentGoal;
import com.spongecoach.domain.PlayerSkill;
import com.spongecoach.domain.PlayerSkillRating;
import com.spongecoach.domain.PlayerSkillRatingId;
import com.spongecoach.domain.Role;
import com.spongecoach.domain.SketchRelation;
import com.spongecoach.domain.Skill;
import com.spongecoach.domain.Team;
import com.spongecoach.drill.DrillJson;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Inserts and tears down fixture rows directly against the domain model, bypassing the REST
 * layer, since v1 has no create/delete endpoints for catalogs, lines, or the team itself.
 */
@ApplicationScoped
public class TestData {

    @Inject
    EntityManager entityManager;

    @Transactional
    public boolean teamExists() {
        return Team.count() > 0;
    }

    @Transactional
    public Line createLine(String name) {
        Team team = Team.theTeam();
        Line line = new Line();
        line.id = UUID.randomUUID();
        line.team = team;
        line.name = name;
        line.color = "#3b6ea5";
        line.persist();
        return line;
    }

    @Transactional
    public Player createPlayer(String name) {
        Player player = new Player();
        player.id = UUID.randomUUID();
        player.team = Team.theTeam();
        player.name = name;
        player.persist();
        return player;
    }

    /** A User in the Team (a SysAdmin in none), acting as the given Player if not null. */
    @Transactional
    public AppUser createUser(String name, Role role, UUID playerId) {
        AppUser user = new AppUser();
        user.id = UUID.randomUUID();
        user.name = name;
        user.role = role;
        user.teamId = role == Role.SYS_ADMIN ? null : Team.theTeam().id;
        user.playerId = playerId;
        user.persist();
        return user;
    }

    @Transactional
    public void deleteUser(UUID userId) {
        AppUser.deleteById(userId);
    }

    @Transactional
    public void linkPlayer(UUID lineId, UUID playerId) {
        entityManager.createNativeQuery("insert into line_player (line_id, player_id) values (?1, ?2)")
                .setParameter(1, lineId)
                .setParameter(2, playerId)
                .executeUpdate();
    }

    /** Convenience: create a team player and roster them on the given line. */
    @Transactional
    public Player addPlayer(UUID lineId, String name) {
        Player player = createPlayer(name);
        linkPlayer(lineId, player.id);
        return player;
    }

    @Transactional
    public void deletePlayer(UUID playerId) {
        entityManager.createNativeQuery("delete from line_player where player_id = ?1")
                .setParameter(1, playerId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event_line_player where player_id = ?1")
                .setParameter(1, playerId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event_attendance where player_id = ?1")
                .setParameter(1, playerId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from player_skill_rating where player_id = ?1")
                .setParameter(1, playerId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from player_player_development_goal where player_id = ?1")
                .setParameter(1, playerId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from player_avatar where player_id = ?1")
                .setParameter(1, playerId)
                .executeUpdate();
        Player.deleteById(playerId);
    }

    @Transactional
    public Long setAvatar(UUID playerId, byte[] image) {
        PlayerAvatar avatar = new PlayerAvatar();
        avatar.playerId = playerId;
        avatar.image = image;
        avatar.persist();
        Player player = Player.findById(playerId);
        player.avatarUpdatedAt = Instant.now();
        return player.avatarVersion();
    }

    @Transactional
    public PlayerSkill createPlayerSkill(String name, String color) {
        PlayerSkill skill = new PlayerSkill();
        skill.id = UUID.randomUUID();
        skill.name = name;
        skill.color = color;
        skill.persist();
        return skill;
    }

    @Transactional
    public PlayerDevelopmentGoal createPlayerGoal(String name, String color) {
        PlayerDevelopmentGoal goal = new PlayerDevelopmentGoal();
        goal.id = UUID.randomUUID();
        goal.name = name;
        goal.color = color;
        goal.persist();
        return goal;
    }

    @Transactional
    public void setPlayerRating(UUID playerId, UUID playerSkillId, int rating) {
        PlayerSkillRating playerRating = new PlayerSkillRating();
        playerRating.id = new PlayerSkillRatingId(playerId, playerSkillId);
        playerRating.player = Player.findById(playerId);
        playerRating.playerSkill = PlayerSkill.findById(playerSkillId);
        playerRating.rating = rating;
        playerRating.persist();
    }

    @Transactional
    public void linkPlayerGoal(UUID playerId, UUID playerGoalId) {
        entityManager.createNativeQuery(
                        "insert into player_player_development_goal (player_id, player_development_goal_id) values (?1, ?2)")
                .setParameter(1, playerId)
                .setParameter(2, playerGoalId)
                .executeUpdate();
    }

    @Transactional
    public void deletePlayerSkill(UUID playerSkillId) {
        entityManager.createNativeQuery("delete from player_skill_rating where player_skill_id = ?1")
                .setParameter(1, playerSkillId)
                .executeUpdate();
        PlayerSkill.deleteById(playerSkillId);
    }

    @Transactional
    public void deletePlayerGoal(UUID playerGoalId) {
        entityManager.createNativeQuery(
                        "delete from player_player_development_goal where player_development_goal_id = ?1")
                .setParameter(1, playerGoalId)
                .executeUpdate();
        PlayerDevelopmentGoal.deleteById(playerGoalId);
    }

    @Transactional
    public Skill createSkill(String name, String color) {
        Skill skill = new Skill();
        skill.id = UUID.randomUUID();
        skill.name = name;
        skill.color = color;
        skill.persist();
        return skill;
    }

    @Transactional
    public DevelopmentGoal createGoal(String name, String color) {
        DevelopmentGoal goal = new DevelopmentGoal();
        goal.id = UUID.randomUUID();
        goal.name = name;
        goal.color = color;
        goal.persist();
        return goal;
    }

    @Transactional
    public void setRating(UUID lineId, UUID skillId, int rating) {
        LineSkill lineSkill = new LineSkill();
        lineSkill.id = new LineSkillId(lineId, skillId);
        lineSkill.line = Line.findById(lineId);
        lineSkill.skill = Skill.findById(skillId);
        lineSkill.rating = rating;
        lineSkill.persist();
    }

    @Transactional
    public void deleteLine(UUID lineId) {
        Line line = Line.findById(lineId);
        if (line == null) {
            return;
        }
        entityManager.createNativeQuery("delete from line_focus_event where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event_line where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event_line_player where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        LineSkill.delete("line.id", lineId);
        entityManager.createNativeQuery("delete from line_player where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from line_development_goal where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        line.delete();
    }

    @Transactional
    public void softDeleteSkill(UUID skillId) {
        Skill skill = Skill.findById(skillId);
        skill.deletedAt = Instant.now();
    }

    @Transactional
    public void softDeleteGoal(UUID goalId) {
        DevelopmentGoal goal = DevelopmentGoal.findById(goalId);
        goal.deletedAt = Instant.now();
    }

    /** Row-level check that bypasses the {@code deletedAt is null} filter, for asserting a soft-deleted Line's row (and its history) survives. */
    public boolean lineRowExists(UUID lineId) {
        return Line.findById(lineId) != null;
    }

    /** Bypasses the {@code deletedAt is null} filter, for asserting a soft-deleted Line's roster survives. */
    @Transactional
    public int lineRosterSize(UUID lineId) {
        Line line = Line.findById(lineId);
        return line == null ? -1 : line.players.size();
    }

    @Transactional
    public void deleteSkill(UUID skillId) {
        entityManager.createNativeQuery("delete from line_skill where skill_id = ?1")
                .setParameter(1, skillId)
                .executeUpdate();
        Skill.deleteById(skillId);
    }

    @Transactional
    public void deleteGoal(UUID goalId) {
        entityManager.createNativeQuery("delete from line_development_goal where development_goal_id = ?1")
                .setParameter(1, goalId)
                .executeUpdate();
        DevelopmentGoal.deleteById(goalId);
    }

    // --- Iteration timeline (issue #12) ----------------------------------------

    /** The seeded Event type with the given name ("Training" / "Match"). */
    public EventType eventType(String name) {
        return EventType.find("name", name).firstResult();
    }

    @Transactional
    public Iteration createIteration(String name, int position) {
        Iteration iteration = new Iteration();
        iteration.id = UUID.randomUUID();
        iteration.team = Team.theTeam();
        iteration.name = name;
        iteration.position = position;
        iteration.persist();
        return iteration;
    }

    @Transactional
    public Event createEvent(UUID iterationId, UUID eventTypeId, String name, LocalDateTime scheduledOn) {
        Event event = new Event();
        event.id = UUID.randomUUID();
        event.iteration = Iteration.findById(iterationId);
        event.eventType = EventType.findById(eventTypeId);
        event.name = name;
        event.scheduledOn = scheduledOn;
        event.lines = new java.util.ArrayList<>(Line.listAllOrderedByName());
        event.persist();
        for (Line line : event.lines) {
            snapshotAttendanceForLine(event, line);
        }
        return event;
    }

    /** Snapshots the given Line onto an already-created Event's attendance, mirroring what
     * production does at creation time — for tests that create the Line after the Event. */
    @Transactional
    public void attendEvent(UUID eventId, UUID lineId) {
        Event event = Event.findById(eventId);
        Line line = Line.findById(lineId);
        if (event.lines.stream().noneMatch(l -> l.id.equals(lineId))) {
            event.lines.add(line);
        }
        snapshotAttendanceForLine(event, line);
    }

    private void snapshotAttendanceForLine(Event event, Line line) {
        for (Player player : line.players) {
            if (EventLinePlayer.find(event.id, line.id, player.id) == null) {
                EventLinePlayer link = new EventLinePlayer();
                link.id = new EventLinePlayerId(event.id, line.id, player.id);
                link.event = event;
                link.line = line;
                link.player = player;
                link.persist();
            }
            if (EventAttendance.find(event.id, player.id) == null) {
                EventAttendance attendance = new EventAttendance();
                attendance.id = new EventAttendanceId(event.id, player.id);
                attendance.event = event;
                attendance.player = player;
                attendance.persist();
            }
        }
    }

    /** Sets a Player's attendance answer directly, bypassing the REST layer — for tests that need
     * a non-default (attending/declined) starting state. */
    @Transactional
    public void setAttendance(UUID eventId, UUID playerId, AttendanceStatus status, String declineMessage) {
        EventAttendance attendance = EventAttendance.find(eventId, playerId);
        attendance.status = status;
        attendance.declineMessage = declineMessage;
    }

    /** Convenience: a Training on the given Iteration at the given datetime. */
    @Transactional
    public Event addTraining(UUID iterationId, LocalDateTime scheduledOn) {
        return createEvent(iterationId, eventType("Training").id, null, scheduledOn);
    }

    @Transactional
    public void attachFocus(UUID eventId, UUID lineId, String focusText) {
        LineFocusEvent attachment = new LineFocusEvent();
        attachment.id = new LineFocusEventId(eventId, lineId);
        attachment.event = Event.findById(eventId);
        attachment.line = Line.findById(lineId);
        attachment.focus = focusText;
        attachment.persist();
    }

    @Transactional
    public void deleteEvent(UUID eventId) {
        entityManager.createNativeQuery("delete from line_focus_event where event_id = ?1")
                .setParameter(1, eventId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event_line where event_id = ?1")
                .setParameter(1, eventId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event_line_player where event_id = ?1")
                .setParameter(1, eventId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event_attendance where event_id = ?1")
                .setParameter(1, eventId)
                .executeUpdate();
        Event.deleteById(eventId);
    }

    @Transactional
    public void deleteIteration(UUID iterationId) {
        entityManager.createNativeQuery(
                        "delete from line_focus_event where event_id in (select id from event where iteration_id = ?1)")
                .setParameter(1, iterationId)
                .executeUpdate();
        entityManager.createNativeQuery(
                        "delete from event_line where event_id in (select id from event where iteration_id = ?1)")
                .setParameter(1, iterationId)
                .executeUpdate();
        entityManager.createNativeQuery(
                        "delete from event_line_player where event_id in (select id from event where iteration_id = ?1)")
                .setParameter(1, iterationId)
                .executeUpdate();
        entityManager.createNativeQuery(
                        "delete from event_attendance where event_id in (select id from event where iteration_id = ?1)")
                .setParameter(1, iterationId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from event where iteration_id = ?1")
                .setParameter(1, iterationId)
                .executeUpdate();
        Iteration.deleteById(iterationId);
    }

    // --- Drills (ADR-0018) --------------------------------------------------------

    /** The question a Drill "waiting for answers" asks. */
    public static final String OPEN_QUESTION = "Sind die Kreise Verteidigerinnen oder Hütchen?";

    /**
     * A Drill with one sketch, in the given state, with no interpreter job behind it:
     * <ul>
     *   <li>{@code READY}: version 1, and the interpreter's reply that made it.
     *   <li>{@code NEEDS_INPUT}: an earlier version 1, and the interpreter's open question.
     *   <li>{@code FAILED}: no version, and an error.
     *   <li>{@code PENDING}: no version, as if a job were still running.
     * </ul>
     */
    @Transactional
    public Drill createDrill(String name, DrillStatus status, Instant updatedAt) {
        Drill drill = new Drill();
        drill.id = UUID.randomUUID();
        drill.team = Team.theTeam();
        drill.name = name;
        drill.sketchRelation = SketchRelation.MIXED;
        drill.status = status;
        drill.createdAt = updatedAt;
        drill.updatedAt = updatedAt;
        drill.persist();

        DrillSketch sketch = new DrillSketch();
        sketch.id = UUID.randomUUID();
        sketch.drill = drill;
        sketch.position = 1;
        sketch.image = Jpegs.sketch();
        sketch.persist();
        drill.sketches.add(sketch);

        if (status == DrillStatus.FAILED) {
            drill.error = "Claude ist nicht erreichbar.";
        }
        if (status == DrillStatus.READY || status == DrillStatus.NEEDS_INPUT) {
            DrillScriptVersion version = new DrillScriptVersion();
            version.id = UUID.randomUUID();
            version.drill = drill;
            version.version = 1;
            version.source = DrillScriptSource.AI;
            version.script = DrillJson.write(TestScripts.playable("Stufe 1"));
            version.createdAt = updatedAt;
            version.persist();
            drill.currentVersion = 1;

            DrillMessage reply = new DrillMessage();
            reply.id = UUID.randomUUID();
            reply.drill = drill;
            reply.position = 1;
            reply.author = DrillMessageAuthor.INTERPRETER;
            reply.createdAt = updatedAt;
            if (status == DrillStatus.READY) {
                reply.content = "Skript erstellt.";
                reply.scriptVersion = 1;
            } else {
                reply.content = "Eine Rückfrage.";
                reply.questions = "[{\"id\":\"q1\",\"text\":\"" + OPEN_QUESTION
                        + "\",\"options\":[\"Verteidigerinnen\",\"Hütchen\"],\"sketch\":1,\"symbolIds\":[]}]";
            }
            reply.persist();
        }
        return drill;
    }

    /** A seeded Drill tag, by name. */
    public UUID drillTagId(String name) {
        DrillTag tag = DrillTag.find("name", name).firstResult();
        if (tag == null) {
            throw new IllegalArgumentException("no seeded drill tag named " + name);
        }
        return tag.id;
    }

    @Transactional
    public DrillStatus drillStatus(UUID drillId) {
        Drill drill = Drill.findById(drillId);
        return drill == null ? null : drill.status;
    }

    /** Removes a Drill and everything under it, soft-deleted or not. */
    @Transactional
    public void deleteDrill(UUID drillId) {
        for (String table : List.of("drill_message", "drill_script_version", "drill_sketch", "drill_drill_tag")) {
            entityManager.createNativeQuery("delete from " + table + " where drill_id = ?1")
                    .setParameter(1, drillId)
                    .executeUpdate();
        }
        entityManager.createNativeQuery("delete from drill where id = ?1")
                .setParameter(1, drillId)
                .executeUpdate();
    }
}
