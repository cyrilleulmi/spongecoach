package com.spongecoach.api;

import com.spongecoach.api.dto.EventCreateRequest;
import com.spongecoach.api.dto.EventDto;
import com.spongecoach.api.dto.EventUpdateRequest;
import com.spongecoach.api.dto.IterationCreateRequest;
import com.spongecoach.api.dto.IterationDto;
import com.spongecoach.api.dto.IterationUpdateRequest;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.EventType;
import com.spongecoach.domain.Iteration;
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

import java.util.List;
import java.util.UUID;

/**
 * The team-overview timeline: Iterations in order, each with its ordered Events and their per-Line
 * Focus attachments. {@code GET /api/iterations} is the one aggregate call the timeline page needs
 * (no N+1). Iterations and Events are user-creatable and deletable; ordering is by plain integer
 * position with nothing enforcing gapless or unique values (ADR-0007).
 */
@Path("/api/iterations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IterationResource {

    @GET
    public List<IterationDto> list() {
        return Iteration.listAllOrderedByPosition().stream().map(IterationDto::from).toList();
    }

    @GET
    @Path("/{iterationId}")
    public IterationDto get(@PathParam("iterationId") UUID iterationId) {
        return IterationDto.from(findIterationOrThrow(iterationId));
    }

    @POST
    @Transactional
    public Response create(IterationCreateRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("name must not be blank");
        }
        Iteration iteration = new Iteration();
        iteration.id = UUID.randomUUID();
        iteration.team = Team.theTeam();
        iteration.name = request.name().trim();
        iteration.position = request.position() != null ? request.position() : nextIterationPosition();
        iteration.persist();

        List<EventCreateRequest> events = request.events() != null ? request.events() : List.of();
        for (int i = 0; i < events.size(); i++) {
            EventCreateRequest draft = events.get(i);
            persistEvent(iteration, draft, i + 1);
        }
        return Response.status(Response.Status.CREATED).entity(IterationDto.from(iteration)).build();
    }

    @PUT
    @Path("/{iterationId}")
    @Transactional
    public IterationDto update(
            @PathParam("iterationId") UUID iterationId, IterationUpdateRequest request) {
        Iteration iteration = findIterationOrThrow(iterationId);
        if (request.name() != null) {
            iteration.name = request.name();
        }
        if (request.position() != null) {
            iteration.position = request.position();
        }
        return IterationDto.from(iteration);
    }

    @DELETE
    @Path("/{iterationId}")
    @Transactional
    public Response delete(@PathParam("iterationId") UUID iterationId) {
        Iteration iteration = findIterationOrThrow(iterationId);
        iteration.delete();
        return Response.noContent().build();
    }

    @POST
    @Path("/{iterationId}/events")
    @Transactional
    public Response addEvent(
            @PathParam("iterationId") UUID iterationId, EventCreateRequest request) {
        Iteration iteration = findIterationOrThrow(iterationId);
        int position = request.position() != null ? request.position() : nextEventPosition(iterationId);
        Event event = persistEvent(iteration, request, position);
        return Response.status(Response.Status.CREATED).entity(EventDto.from(event)).build();
    }

    @PUT
    @Path("/{iterationId}/events/{eventId}")
    @Transactional
    public EventDto updateEvent(
            @PathParam("iterationId") UUID iterationId,
            @PathParam("eventId") UUID eventId,
            EventUpdateRequest request) {
        findIterationOrThrow(iterationId);
        Event event = findEventInIterationOrThrow(iterationId, eventId);
        if (request.eventTypeId() != null) {
            event.eventType = findEventTypeOrThrow(request.eventTypeId());
        }
        if (request.name() != null) {
            event.name = request.name().isBlank() ? null : request.name().trim();
        }
        if (request.position() != null) {
            event.position = request.position();
        }
        if (request.scheduledOn() != null) {
            event.scheduledOn = request.scheduledOn();
        }
        return EventDto.from(event);
    }

    @DELETE
    @Path("/{iterationId}/events/{eventId}")
    @Transactional
    public Response deleteEvent(
            @PathParam("iterationId") UUID iterationId, @PathParam("eventId") UUID eventId) {
        Iteration iteration = findIterationOrThrow(iterationId);
        Event event = findEventInIterationOrThrow(iterationId, eventId);
        iteration.events.remove(event);
        event.delete();
        return Response.noContent().build();
    }

    private Event persistEvent(Iteration iteration, EventCreateRequest draft, int fallbackPosition) {
        if (draft == null || draft.eventTypeId() == null) {
            throw new BadRequestException("eventTypeId must not be null");
        }
        Event event = new Event();
        event.id = UUID.randomUUID();
        event.iteration = iteration;
        event.eventType = findEventTypeOrThrow(draft.eventTypeId());
        event.name = draft.name() != null && !draft.name().isBlank() ? draft.name().trim() : null;
        event.position = draft.position() != null ? draft.position() : fallbackPosition;
        event.scheduledOn = draft.scheduledOn();
        event.persist();
        iteration.events.add(event);
        return event;
    }

    private int nextIterationPosition() {
        return Iteration.<Iteration>listAll().stream().mapToInt(it -> it.position).max().orElse(0) + 1;
    }

    private int nextEventPosition(UUID iterationId) {
        return Event.listForIteration(iterationId).stream().mapToInt(e -> e.position).max().orElse(0) + 1;
    }

    private Iteration findIterationOrThrow(UUID iterationId) {
        Iteration iteration = Iteration.findById(iterationId);
        if (iteration == null) {
            throw new NotFoundException("Iteration " + iterationId + " not found");
        }
        return iteration;
    }

    private Event findEventInIterationOrThrow(UUID iterationId, UUID eventId) {
        Event event = Event.findById(eventId);
        if (event == null || !event.iteration.id.equals(iterationId)) {
            throw new NotFoundException("Event " + eventId + " not found in iteration " + iterationId);
        }
        return event;
    }

    private EventType findEventTypeOrThrow(UUID eventTypeId) {
        EventType eventType = EventType.findById(eventTypeId);
        if (eventType == null) {
            throw new NotFoundException("Event type " + eventTypeId + " not found");
        }
        return eventType;
    }
}
