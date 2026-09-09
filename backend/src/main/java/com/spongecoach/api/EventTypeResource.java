package com.spongecoach.api;

import com.spongecoach.api.dto.EventTypeDto;
import com.spongecoach.domain.EventType;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** The Event types a coach can pick from — seeded with Training and Match (no create endpoint in v1). */
@Path("/api/event-types")
@Produces(MediaType.APPLICATION_JSON)
public class EventTypeResource {

    @GET
    public List<EventTypeDto> list() {
        return EventType.listAllOrderedByName().stream().map(EventTypeDto::from).toList();
    }
}
