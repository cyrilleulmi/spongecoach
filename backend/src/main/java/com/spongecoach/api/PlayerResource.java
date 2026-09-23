package com.spongecoach.api;

import com.spongecoach.api.dto.PlayerDetailDto;
import com.spongecoach.api.dto.PlayerSkillRatingDto;
import com.spongecoach.api.dto.PlayerSummaryDto;
import com.spongecoach.api.dto.PlayerUpdateRequest;
import com.spongecoach.api.dto.RatingRequest;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.PlayerAvatar;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** The team's player pool. Players are created by seed only (no create endpoint in v1). */
@Path("/api/players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlayerResource {

    private static final String PNG = "image/png";
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
    private static final int AVATAR_MAX_BYTES = 200 * 1024;
    private static final int AVATAR_MAX_SIDE = 512;

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

    /**
     * The painted Avatar PNG. Clients ask for it with {@code ?v=<avatarVersion>}; a request naming the
     * current version is cached for good, since a new save produces a new version and thus a new URL.
     */
    @GET
    @Path("/{playerId}/avatar")
    @Produces(PNG)
    @Transactional
    public Response getAvatar(@PathParam("playerId") UUID playerId, @QueryParam("v") Long version) {
        Player player = findPlayerOrThrow(playerId);
        PlayerAvatar avatar = PlayerAvatar.findById(playerId);
        if (avatar == null) {
            throw new NotFoundException("Player " + playerId + " has no avatar");
        }
        boolean current = version != null && version.equals(player.avatarVersion());
        return Response.ok(avatar.image, PNG)
                .header("Cache-Control", current ? "public, max-age=31536000, immutable" : "no-cache")
                .build();
    }

    /** Saves the painted Avatar, overwriting any earlier one — no history is kept (ADR-0016). */
    @PUT
    @Path("/{playerId}/avatar")
    @Consumes(PNG)
    @Transactional
    public PlayerDetailDto setAvatar(@PathParam("playerId") UUID playerId, byte[] image) {
        validateAvatar(image);
        Player player = findPlayerOrThrow(playerId);
        PlayerAvatar avatar = PlayerAvatar.findById(playerId);
        if (avatar == null) {
            avatar = new PlayerAvatar();
            avatar.playerId = playerId;
        }
        avatar.image = image;
        avatar.persist();
        player.avatarUpdatedAt = Instant.now();
        return PlayerDetailDto.from(player, PlayerSkillRating.listForPlayer(playerId));
    }

    /** Back to initials. Removing an Avatar the Player doesn't have is a no-op, not an error. */
    @DELETE
    @Path("/{playerId}/avatar")
    @Transactional
    public Response removeAvatar(@PathParam("playerId") UUID playerId) {
        Player player = findPlayerOrThrow(playerId);
        PlayerAvatar.deleteById(playerId);
        player.avatarUpdatedAt = null;
        return Response.noContent().build();
    }

    /** A PNG by signature, square, and no bigger than the painter produces. */
    private static void validateAvatar(byte[] image) {
        if (image == null || image.length < 24 || !Arrays.equals(image, 0, 8, PNG_SIGNATURE, 0, 8)) {
            throw new BadRequestException("avatar must be a PNG image");
        }
        if (image.length > AVATAR_MAX_BYTES) {
            throw new BadRequestException("avatar must be at most " + AVATAR_MAX_BYTES / 1024 + " KB");
        }
        // IHDR is always the first chunk: width and height are big-endian ints at bytes 16 and 20.
        ByteBuffer header = ByteBuffer.wrap(image, 16, 8);
        int width = header.getInt();
        int height = header.getInt();
        if (width != height || width < 1 || width > AVATAR_MAX_SIDE) {
            throw new BadRequestException("avatar must be square and at most " + AVATAR_MAX_SIDE + " px");
        }
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
