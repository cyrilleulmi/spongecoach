package com.spongecoach.api;

import com.spongecoach.api.dto.PlayerDetailDto;
import com.spongecoach.api.dto.PlayerSkillRatingDto;
import com.spongecoach.api.dto.PlayerSummaryDto;
import com.spongecoach.api.dto.PlayerUpdateRequest;
import com.spongecoach.api.dto.RatingRequest;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.PlayerDevelopmentGoal;
import com.spongecoach.domain.PlayerSkill;
import com.spongecoach.domain.PlayerSkillRating;
import com.spongecoach.domain.PlayerSkillRatingId;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The team's player pool. Players are created by seed only (no create endpoint in v1). */
@Path("/api/players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlayerResource {

    @GET
    @Transactional
    public List<PlayerSummaryDto> list() {
        return Player.listAllOrderedByName().stream().map(PlayerSummaryDto::from).toList();
    }

    @GET
    @Path("/{playerId}")
    @Transactional
    public PlayerDetailDto get(@PathParam("playerId") UUID playerId) {
        Player player = findPlayerOrThrow(playerId);
        return PlayerDetailDto.from(player, PlayerSkillRating.listForPlayer(playerId));
    }

    @PUT
    @Path("/{playerId}")
    @Transactional
    public PlayerDetailDto update(@PathParam("playerId") UUID playerId, PlayerUpdateRequest request) {
        Player player = findPlayerOrThrow(playerId);
        if (request.developmentGoalIds() != null) {
            player.developmentGoals = new ArrayList<>(
                    request.developmentGoalIds().stream().map(this::findGoalOrThrow).toList());
        }
        return PlayerDetailDto.from(player, PlayerSkillRating.listForPlayer(playerId));
    }

    @GET
    @Path("/{playerId}/skills")
    public List<PlayerSkillRatingDto> listSkills(@PathParam("playerId") UUID playerId) {
        findPlayerOrThrow(playerId);
        return PlayerSkillRating.listForPlayer(playerId).stream().map(PlayerSkillRatingDto::from).toList();
    }

    @PUT
    @Path("/{playerId}/skills/{skillId}")
    @Transactional
    public PlayerSkillRatingDto setRating(
            @PathParam("playerId") UUID playerId, @PathParam("skillId") UUID skillId, RatingRequest request) {
        if (request.rating() == null || request.rating() < 0 || request.rating() > 100) {
            throw new BadRequestException("rating must be between 0 and 100");
        }
        Player player = findPlayerOrThrow(playerId);
        PlayerSkill skill = findSkillOrThrow(skillId);

        PlayerSkillRating rating = PlayerSkillRating.find(playerId, skillId);
        if (rating == null) {
            rating = new PlayerSkillRating();
            rating.id = new PlayerSkillRatingId(playerId, skillId);
            rating.player = player;
            rating.playerSkill = skill;
        }
        rating.rating = request.rating();
        rating.persist();
        return PlayerSkillRatingDto.from(rating);
    }

    @DELETE
    @Path("/{playerId}/skills/{skillId}")
    @Transactional
    public Response removeSkill(@PathParam("playerId") UUID playerId, @PathParam("skillId") UUID skillId) {
        findPlayerOrThrow(playerId);
        PlayerSkillRating rating = PlayerSkillRating.find(playerId, skillId);
        if (rating == null) {
            throw new NotFoundException("Player " + playerId + " is not rated on skill " + skillId);
        }
        rating.delete();
        return Response.noContent().build();
    }

    private Player findPlayerOrThrow(UUID playerId) {
        Player player = Player.findById(playerId);
        if (player == null) {
            throw new NotFoundException("Player " + playerId + " not found");
        }
        return player;
    }

    private PlayerSkill findSkillOrThrow(UUID skillId) {
        PlayerSkill skill = PlayerSkill.findById(skillId);
        if (skill == null || skill.deletedAt != null) {
            throw new NotFoundException("Player skill " + skillId + " not found");
        }
        return skill;
    }

    private PlayerDevelopmentGoal findGoalOrThrow(UUID goalId) {
        PlayerDevelopmentGoal goal = PlayerDevelopmentGoal.findById(goalId);
        if (goal == null || goal.deletedAt != null) {
            throw new NotFoundException("Player development goal " + goalId + " not found");
        }
        return goal;
    }
}
