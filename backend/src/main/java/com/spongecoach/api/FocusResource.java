package com.spongecoach.api;

import com.spongecoach.api.dto.FocusCreateRequest;
import com.spongecoach.api.dto.FocusDto;
import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Focus;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/focuses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FocusResource {

    @GET
    public List<FocusDto> list() {
        return Focus.listActive().stream().map(FocusDto::from).toList();
    }

    @POST
    @Transactional
    public Response create(FocusCreateRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name must not be blank");
        }
        if (request.goalIds() == null || request.goalIds().isEmpty()) {
            throw new BadRequestException("a Focus must be derived from at least one Development goal");
        }
        List<DevelopmentGoal> goals = request.goalIds().stream()
                .map(id -> {
                    DevelopmentGoal goal = DevelopmentGoal.findById(id);
                    if (goal == null || goal.deletedAt != null) {
                        throw new NotFoundException("Development goal " + id + " not found");
                    }
                    return goal;
                })
                .toList();

        Focus focus = new Focus();
        focus.id = UUID.randomUUID();
        focus.name = request.name().trim();
        focus.developmentGoals = goals;
        focus.persist();
        return Response.status(Response.Status.CREATED).entity(FocusDto.from(focus)).build();
    }
}
