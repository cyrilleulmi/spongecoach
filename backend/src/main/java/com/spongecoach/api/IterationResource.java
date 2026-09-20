package com.spongecoach.api;

import com.spongecoach.api.dto.EventCreateRequest;
import com.spongecoach.api.dto.EventDto;
import com.spongecoach.api.dto.EventUpdateRequest;
import com.spongecoach.api.dto.IterationCreateRequest;
import com.spongecoach.api.dto.IterationDto;
import com.spongecoach.api.dto.IterationUpdateRequest;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.EventAttendance;
import com.spongecoach.domain.EventAttendanceId;
import com.spongecoach.domain.EventLinePlayer;
import com.spongecoach.domain.EventLinePlayerId;
import com.spongecoach.domain.EventType;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.Player;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The team-overview timeline: Iterations in order, each with its ordered Events and their per-Line
 * Focus attachments. {@code GET /api/iterations} is the one aggregate call the timeline page needs
 * (no N+1). Iterations are user-creatable and deletable, ordered by a plain integer position
 * (ADR-0007). Events are ordered by their mandatory {@code scheduledOn}, unique within the
 * Iteration (ADR-0011) — a collision on create or update is rejected with 409.
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
        for (EventCreateRequest draft : events) {
            persistEvent(iteration, draft);
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
        Event event = persistEvent(iteration, request);
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
        if (request.scheduledOn() != null) {
            if (Event.existsAtSlot(iterationId, request.scheduledOn(), event.id)) {
                throw new ConflictException(
                        "Event " + eventId + " cannot be scheduled at " + request.scheduledOn()
                                + ": another event in this iteration already uses that slot");
            }
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

    private Event persistEvent(Iteration iteration, EventCreateRequest draft) {
        if (draft == null || draft.eventTypeId() == null) {
            throw new BadRequestException("eventTypeId must not be null");
        }
        if (draft.scheduledOn() == null) {
            throw new BadRequestException("scheduledOn must not be null");
        }
        if (Event.existsAtSlot(iteration.id, draft.scheduledOn(), null)) {
            throw new ConflictException(
                    "Cannot schedule at " + draft.scheduledOn()
                            + ": another event in this iteration already uses that slot");
        }
        Event event = new Event();
        event.id = UUID.randomUUID();
        event.iteration = iteration;
        event.eventType = findEventTypeOrThrow(draft.eventTypeId());
        event.name = draft.name() != null && !draft.name().isBlank() ? draft.name().trim() : null;
        event.scheduledOn = draft.scheduledOn();
        event.lines = new ArrayList<>(Line.listAllOrderedByName());
        event.persist();
        iteration.events.add(event);
        snapshotAttendance(event);
        return event;
    }

    /** Freezes attendance for a newly created Event: every attending Line's current roster,
     * defaulted to {@code PENDING} (ADR-0012's Line snapshot, one level deeper). */
    private void snapshotAttendance(Event event) {
        Set<UUID> seenPlayers = new HashSet<>();
        for (Line line : event.lines) {
            for (Player player : line.players) {
                EventLinePlayer link = new EventLinePlayer();
                link.id = new EventLinePlayerId(event.id, line.id, player.id);
                link.event = event;
                link.line = line;
                link.player = player;
                link.persist();

                if (seenPlayers.add(player.id)) {
                    EventAttendance attendance = new EventAttendance();
                    attendance.id = new EventAttendanceId(event.id, player.id);
                    attendance.event = event;
                    attendance.player = player;
                    attendance.persist();
                }
            }
        }
    }

    private int nextIterationPosition() {
        return Iteration.<Iteration>listAll().stream().mapToInt(it -> it.position).max().orElse(0) + 1;
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
