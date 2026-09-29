package com.spongecoach.api;

import com.spongecoach.api.dto.DrillTagDto;
import com.spongecoach.domain.DrillTag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** The fixed, seeded Drill tag list (ADR-0018). Read-only in v1. */
@Path("/api/drill-tags")
@Produces(MediaType.APPLICATION_JSON)
public class DrillTagResource {

    @GET
    public List<DrillTagDto> list() {
        return DrillTag.listOrdered().stream().map(DrillTagDto::from).toList();
    }
}
