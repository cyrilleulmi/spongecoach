package com.spongecoach.support;

import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.LineSkill;
import com.spongecoach.domain.LineSkillId;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Skill;
import com.spongecoach.domain.Team;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
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
    public Line createLine(String name) {
        Team team = Team.theTeam();
        Line line = new Line();
        line.id = UUID.randomUUID();
        line.team = team;
        line.name = name;
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
        Player.deleteById(playerId);
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
    public Focus createFocus(String name, List<UUID> goalIds) {
        Focus focus = new Focus();
        focus.id = UUID.randomUUID();
        focus.name = name;
        focus.developmentGoals = goalIds.stream().map((UUID id) -> (DevelopmentGoal) DevelopmentGoal.findById(id)).toList();
        focus.persist();
        return focus;
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
        LineSkill.delete("line.id", lineId);
        entityManager.createNativeQuery("delete from line_player where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from line_development_goal where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from line_focus where line_id = ?1")
                .setParameter(1, lineId)
                .executeUpdate();
        line.delete();
    }

    @Transactional
    public void softDeleteGoal(UUID goalId) {
        DevelopmentGoal goal = DevelopmentGoal.findById(goalId);
        goal.deletedAt = Instant.now();
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
        entityManager.createNativeQuery("delete from focus_development_goal where development_goal_id = ?1")
                .setParameter(1, goalId)
                .executeUpdate();
        DevelopmentGoal.deleteById(goalId);
    }

    @Transactional
    public void deleteFocus(UUID focusId) {
        entityManager.createNativeQuery("delete from line_focus where focus_id = ?1")
                .setParameter(1, focusId)
                .executeUpdate();
        entityManager.createNativeQuery("delete from focus_development_goal where focus_id = ?1")
                .setParameter(1, focusId)
                .executeUpdate();
        Focus.deleteById(focusId);
    }
}
