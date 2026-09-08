package com.spongecoach.api;

import com.spongecoach.api.dto.CatalogRefDto;
import com.spongecoach.api.dto.CatalogUpdateRequest;
import com.spongecoach.domain.DevelopmentGoal;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/development-goals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DevelopmentGoalResource {

    private static final String DEFAULT_COLOR = "#8e44ad";

    @GET
    public List<CatalogRefDto> list() {
        return DevelopmentGoal.listActive().stream().map(CatalogRefDto::from).toList();
    }

    @POST
    @Transactional
    public Response create(CatalogUpdateRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name must not be blank");
        }
        DevelopmentGoal goal = new DevelopmentGoal();
        goal.id = UUID.randomUUID();
        goal.name = request.name().trim();
        goal.color = request.color() != null ? request.color() : DEFAULT_COLOR;
        goal.persist();
        return Response.status(Response.Status.CREATED).entity(CatalogRefDto.from(goal)).build();
    }

    @PUT
    @Path("/{goalId}")
    @Transactional
    public CatalogRefDto update(@PathParam("goalId") UUID goalId, CatalogUpdateRequest request) {
        DevelopmentGoal goal = DevelopmentGoal.findById(goalId);
        if (goal == null || goal.deletedAt != null) {
            throw new NotFoundException("Development goal " + goalId + " not found");
        }
        if (request.name() != null) {
            goal.name = request.name();
        }
        if (request.color() != null) {
            goal.color = request.color();
        }
        return CatalogRefDto.from(goal);
    }
}
