package com.spongecoach.support;

import com.spongecoach.domain.AttendanceStatus;
import com.spongecoach.domain.DevelopmentGoal;
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
import com.spongecoach.domain.PlayerDevelopmentGoal;
import com.spongecoach.domain.PlayerSkill;
import com.spongecoach.domain.PlayerSkillRating;
import com.spongecoach.domain.PlayerSkillRatingId;
import com.spongecoach.domain.Skill;
import com.spongecoach.domain.Team;
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
        Player.deleteById(playerId);
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
}
