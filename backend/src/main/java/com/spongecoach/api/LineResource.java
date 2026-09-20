package com.spongecoach.api;

import com.spongecoach.api.dto.LineCreateRequest;
import com.spongecoach.api.dto.LineDetailDto;
import com.spongecoach.api.dto.LineSkillDto;
import com.spongecoach.api.dto.LineSummaryDto;
import com.spongecoach.api.dto.LineUpdateRequest;
import com.spongecoach.api.dto.RatingRequest;
import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.EventAttendance;
import com.spongecoach.domain.EventAttendanceId;
import com.spongecoach.domain.EventLinePlayer;
import com.spongecoach.domain.EventLinePlayerId;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.LineSkill;
import com.spongecoach.domain.LineSkillId;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Skill;
import com.spongecoach.domain.Team;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/lines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LineResource {

    /** Fixed dial palette (independent of the Skill/Development-goal palette), assigned once at
     * creation so a Line's color survives its own later deletion in historic Event dials. */
    private static final String[] COLOR_PALETTE = {
        "#4c8c3d", "#8b5e34", "#6a4c93", "#c9702c", "#3b6ea5", "#b1467a", "#8a7a2e", "#2c7a68",
    };

    @GET
    public List<LineSummaryDto> list() {
        return Line.listAllOrderedByName().stream().map(LineSummaryDto::from).toList();
    }

    @GET
    @Path("/{lineId}")
    public LineDetailDto get(@PathParam("lineId") UUID lineId) {
        Line line = findLineOrThrow(lineId);
        return LineDetailDto.from(line, LineSkill.listForLine(lineId));
    }

    @POST
    @Transactional
    public Response create(LineCreateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name must not be blank");
        }
        Line line = new Line();
        line.id = UUID.randomUUID();
        line.team = Team.theTeam();
        line.name = request.name().trim();
        line.color = COLOR_PALETTE[(int) (Line.count() % COLOR_PALETTE.length)];
        line.persist();
        return Response.status(Response.Status.CREATED).entity(LineSummaryDto.from(line)).build();
    }

    @DELETE
    @Path("/{lineId}")
    @Transactional
    public Response delete(@PathParam("lineId") UUID lineId) {
        Line line = findLineOrThrow(lineId);
        line.deletedAt = Instant.now();
        return Response.noContent().build();
    }

    @GET
    @Path("/deleted")
    public List<LineSummaryDto> listDeleted() {
        return Line.listDeletedOrderedByDeletedAt().stream().map(LineSummaryDto::from).toList();
    }

    @POST
    @Path("/{lineId}/restore")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public LineSummaryDto restore(@PathParam("lineId") UUID lineId) {
        Line line = findDeletedLineOrThrow(lineId);
        line.deletedAt = null;
        return LineSummaryDto.from(line);
    }

    @PUT
    @Path("/{lineId}")
    @Transactional
    public LineDetailDto update(@PathParam("lineId") UUID lineId, LineUpdateRequest request) {
        Line line = findLineOrThrow(lineId);
        if (request.name() != null) {
            line.name = request.name();
        }
        if (request.playerIds() != null) {
            Set<UUID> oldPlayerIds = line.players.stream().map(p -> p.id).collect(Collectors.toSet());
            List<Player> newPlayers = request.playerIds().stream().map(this::findPlayerOrThrow).toList();
            Set<UUID> newPlayerIds = newPlayers.stream().map(p -> p.id).collect(Collectors.toSet());
            line.players = newPlayers;

            Set<UUID> added = new HashSet<>(newPlayerIds);
            added.removeAll(oldPlayerIds);
            Set<UUID> removed = new HashSet<>(oldPlayerIds);
            removed.removeAll(newPlayerIds);
            if (!added.isEmpty() || !removed.isEmpty()) {
                syncAttendanceForRosterChange(line, added, removed);
            }
        }
        if (request.developmentGoalIds() != null) {
            line.developmentGoals = request.developmentGoalIds().stream()
                    .map(this::findGoalOrThrow)
                    .toList();
        }
        if (request.focusIds() != null) {
            line.focuses = request.focusIds().stream().map(this::findFocusOrThrow).toList();
        }
        return LineDetailDto.from(line, LineSkill.listForLine(lineId));
    }

    @GET
    @Path("/{lineId}/skills")
    public List<LineSkillDto> listSkills(@PathParam("lineId") UUID lineId) {
        findLineOrThrow(lineId);
        return LineSkill.listForLine(lineId).stream().map(LineSkillDto::from).toList();
    }

    @PUT
    @Path("/{lineId}/skills/{skillId}")
    @Transactional
    public LineSkillDto setRating(
            @PathParam("lineId") UUID lineId, @PathParam("skillId") UUID skillId, RatingRequest request) {
        if (request.rating() == null || request.rating() < 0 || request.rating() > 100) {
            throw new BadRequestException("rating must be between 0 and 100");
        }
        Line line = findLineOrThrow(lineId);
        Skill skill = findSkillOrThrow(skillId);

        LineSkill lineSkill = LineSkill.find(lineId, skillId);
        if (lineSkill == null) {
            lineSkill = new LineSkill();
            lineSkill.line = line;
            lineSkill.skill = skill;
            lineSkill.id = new LineSkillId(lineId, skillId);
        }
        lineSkill.rating = request.rating();
        lineSkill.persist();
        return LineSkillDto.from(lineSkill);
    }

    @DELETE
    @Path("/{lineId}/skills/{skillId}")
    @Transactional
    public Response removeSkill(@PathParam("lineId") UUID lineId, @PathParam("skillId") UUID skillId) {
        findLineOrThrow(lineId);
        LineSkill lineSkill = LineSkill.find(lineId, skillId);
        if (lineSkill == null) {
            throw new NotFoundException("Line " + lineId + " is not associated with skill " + skillId);
        }
        lineSkill.delete();
        return Response.noContent().build();
    }

    /**
     * Keeps not-done Events' attendance snapshots (ADR-0012, one level deeper — see
     * {@link EventLinePlayer}) in step with a Line roster edit: a newly rostered Player joins the
     * attendance list of every upcoming Event this Line attends (defaulted to
     * {@code PENDING}); a dropped Player leaves that Line's snapshot for those Events, and loses
     * their {@link EventAttendance} answer entirely once they're on no other attending Line for
     * that Event. Done Events are untouched — their attendance is history.
     */
    private void syncAttendanceForRosterChange(Line line, Set<UUID> addedPlayerIds, Set<UUID> removedPlayerIds) {
        List<Event> upcomingEvents = Event.listAttendingLineNotDone(line.id, LocalDateTime.now());
        for (Event event : upcomingEvents) {
            for (UUID playerId : addedPlayerIds) {
                if (EventLinePlayer.find(event.id, line.id, playerId) == null) {
                    EventLinePlayer link = new EventLinePlayer();
                    link.id = new EventLinePlayerId(event.id, line.id, playerId);
                    link.event = event;
                    link.line = line;
                    link.player = Player.findById(playerId);
                    link.persist();
                }
                if (EventAttendance.find(event.id, playerId) == null) {
                    EventAttendance attendance = new EventAttendance();
                    attendance.id = new EventAttendanceId(event.id, playerId);
                    attendance.event = event;
                    attendance.player = Player.findById(playerId);
                    attendance.persist();
                }
            }
            for (UUID playerId : removedPlayerIds) {
                EventLinePlayer link = EventLinePlayer.find(event.id, line.id, playerId);
                if (link != null) {
                    link.delete();
                }
                if (EventLinePlayer.countForEventAndPlayer(event.id, playerId) == 0) {
                    EventAttendance attendance = EventAttendance.find(event.id, playerId);
                    if (attendance != null) {
                        attendance.delete();
                    }
                }
            }
        }
    }

    private Line findLineOrThrow(UUID lineId) {
        Line line = Line.findById(lineId);
        if (line == null || line.deletedAt != null) {
            throw new NotFoundException("Line " + lineId + " not found");
        }
        return line;
    }

    private Line findDeletedLineOrThrow(UUID lineId) {
        Line line = Line.findById(lineId);
        if (line == null || line.deletedAt == null) {
            throw new NotFoundException("Deleted line " + lineId + " not found");
        }
        return line;
    }

    private Player findPlayerOrThrow(UUID playerId) {
        Player player = Player.findById(playerId);
        if (player == null) {
            throw new NotFoundException("Player " + playerId + " not found");
        }
        return player;
    }

    private Skill findSkillOrThrow(UUID skillId) {
        Skill skill = Skill.findById(skillId);
        if (skill == null || skill.deletedAt != null) {
            throw new NotFoundException("Skill " + skillId + " not found");
        }
        return skill;
    }

    private DevelopmentGoal findGoalOrThrow(UUID goalId) {
        DevelopmentGoal goal = DevelopmentGoal.findById(goalId);
        if (goal == null || goal.deletedAt != null) {
            throw new NotFoundException("Development goal " + goalId + " not found");
        }
        return goal;
    }

    private Focus findFocusOrThrow(UUID focusId) {
        Focus focus = Focus.findById(focusId);
        if (focus == null || focus.deletedAt != null) {
            throw new NotFoundException("Focus " + focusId + " not found");
        }
        return focus;
    }
}
