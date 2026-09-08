package com.spongecoach.api;

import com.spongecoach.api.dto.LineDetailDto;
import com.spongecoach.api.dto.LineSkillDto;
import com.spongecoach.api.dto.LineSummaryDto;
import com.spongecoach.api.dto.LineUpdateRequest;
import com.spongecoach.api.dto.RatingRequest;
import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.LineSkill;
import com.spongecoach.domain.LineSkillId;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Skill;
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

import java.util.List;
import java.util.UUID;

@Path("/api/lines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LineResource {

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

    @PUT
    @Path("/{lineId}")
    @Transactional
    public LineDetailDto update(@PathParam("lineId") UUID lineId, LineUpdateRequest request) {
        Line line = findLineOrThrow(lineId);
        if (request.name() != null) {
            line.name = request.name();
        }
        if (request.playerIds() != null) {
            line.players = request.playerIds().stream().map(this::findPlayerOrThrow).toList();
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

    private Line findLineOrThrow(UUID lineId) {
        Line line = Line.findById(lineId);
        if (line == null) {
            throw new NotFoundException("Line " + lineId + " not found");
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
