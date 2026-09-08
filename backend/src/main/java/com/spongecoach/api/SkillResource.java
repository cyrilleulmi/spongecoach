package com.spongecoach.api;

import com.spongecoach.api.dto.CatalogRefDto;
import com.spongecoach.api.dto.CatalogUpdateRequest;
import com.spongecoach.domain.Skill;
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

@Path("/api/skills")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SkillResource {

    private static final String DEFAULT_COLOR = "#3b6ea5";

    @GET
    public List<CatalogRefDto> list() {
        return Skill.listActive().stream().map(CatalogRefDto::from).toList();
    }

    @POST
    @Transactional
    public Response create(CatalogUpdateRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name must not be blank");
        }
        Skill skill = new Skill();
        skill.id = UUID.randomUUID();
        skill.name = request.name().trim();
        skill.color = request.color() != null ? request.color() : DEFAULT_COLOR;
        skill.persist();
        return Response.status(Response.Status.CREATED).entity(CatalogRefDto.from(skill)).build();
    }

    @PUT
    @Path("/{skillId}")
    @Transactional
    public CatalogRefDto update(@PathParam("skillId") UUID skillId, CatalogUpdateRequest request) {
        Skill skill = Skill.findById(skillId);
        if (skill == null || skill.deletedAt != null) {
            throw new NotFoundException("Skill " + skillId + " not found");
        }
        if (request.name() != null) {
            skill.name = request.name();
        }
        if (request.color() != null) {
            skill.color = request.color();
        }
        return CatalogRefDto.from(skill);
    }
}
