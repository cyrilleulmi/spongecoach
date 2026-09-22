package com.spongecoach.api;

import com.spongecoach.api.dto.AttendanceUpdateRequest;
import com.spongecoach.api.dto.EventDto;
import com.spongecoach.api.dto.EventUpdateRequest;
import com.spongecoach.api.dto.FocusAttachmentRequest;
import com.spongecoach.domain.AttendanceStatus;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.EventAttendance;
import com.spongecoach.domain.EventAttendanceId;
import com.spongecoach.domain.EventLinePlayer;
import com.spongecoach.domain.EventType;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.LineFocusEvent;
import com.spongecoach.domain.LineFocusEventId;
import com.spongecoach.domain.Player;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Editing a single Event by id. The team-overview timeline attaches/clears a Line's Focus for an
 * Event here via {@code focusAttachments} — a full replacement of the event's set, one Focus per
 * Line (an empty list clears them all). Event fields (type/name/date) may also be set. A
 * {@code scheduledOn} change must land on a datetime not already used by another Event in the same
 * Iteration (ADR-0011); a collision is rejected with 409.
 */
@Path("/api/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    @PUT
    @Path("/{eventId}")
    @Transactional
    public EventDto update(@PathParam("eventId") UUID eventId, EventUpdateRequest request) {
        Event event = findEventOrThrow(eventId);

        if (request.eventTypeId() != null) {
            event.eventType = findEventTypeOrThrow(request.eventTypeId());
        }
        if (request.name() != null) {
            event.name = request.name().isBlank() ? null : request.name().trim();
        }
        if (request.scheduledOn() != null) {
            if (Event.existsAtSlot(event.iteration.id, request.scheduledOn(), event.id)) {
                throw new ConflictException(
                        "Event " + eventId + " cannot be scheduled at " + request.scheduledOn()
                                + ": another event in this iteration already uses that slot");
            }
            event.scheduledOn = request.scheduledOn();
        }
        if (request.focusAttachments() != null) {
            replaceFocusAttachments(event, request.focusAttachments());
        }
        return EventDto.from(event);
    }

    /**
     * Sets one Player's attendance answer for the Event. The Player must already be on the
     * Event's snapshot ({@code event_line_player}) — attendance can't be set for someone who
     * isn't on any attending Line for it. A non-{@code DECLINED} status always clears any
     * decline message, even if one was set previously.
     */
    @PUT
    @Path("/{eventId}/attendance/{playerId}")
    @Transactional
    public EventDto setAttendance(
            @PathParam("eventId") UUID eventId,
            @PathParam("playerId") UUID playerId,
            AttendanceUpdateRequest request) {
        Event event = findEventOrThrow(eventId);
        if (EventLinePlayer.countForEventAndPlayer(eventId, playerId) == 0) {
            throw new NotFoundException(
                    "Player " + playerId + " is not on event " + eventId + "'s attendance list");
        }
        if (request == null || request.status() == null) {
            throw new BadRequestException("status must not be null");
        }
        AttendanceStatus status;
        try {
            status = AttendanceStatus.valueOf(request.status());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unknown attendance status " + request.status());
        }

        EventAttendance attendance = EventAttendance.find(eventId, playerId);
        if (attendance == null) {
            attendance = new EventAttendance();
            attendance.id = new EventAttendanceId(eventId, playerId);
            attendance.event = event;
            attendance.player = Player.findById(playerId);
        }
        attendance.status = status;
        attendance.declineMessage = status == AttendanceStatus.DECLINED
                ? blankToNull(request.declineMessage())
                : null;
        attendance.persist();
        return EventDto.from(event);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Event findEventOrThrow(UUID eventId) {
        Event event = Event.findById(eventId);
        if (event == null) {
            throw new NotFoundException("Event " + eventId + " not found");
        }
        return event;
    }

    private void replaceFocusAttachments(Event event, List<FocusAttachmentRequest> requested) {
        Set<UUID> keepLineIds = new HashSet<>();
        for (FocusAttachmentRequest item : requested) {
            if (item.lineId() == null || item.focus() == null || item.focus().isBlank()) {
                throw new BadRequestException("each focus attachment needs a lineId and a non-blank focus");
            }
            Line line = Line.findById(item.lineId());
            if (line == null) {
                throw new NotFoundException("Line " + item.lineId() + " not found");
            }
            boolean attending = event.lines.stream().anyMatch(l -> l.id.equals(line.id));
            if (!attending) {
                throw new BadRequestException(
                        "Line " + item.lineId() + " is not attending event " + event.id);
            }

            LineFocusEvent attachment = LineFocusEvent.find(event.id, line.id);
            if (attachment == null) {
                attachment = new LineFocusEvent();
                attachment.id = new LineFocusEventId(event.id, line.id);
                attachment.event = event;
                attachment.line = line;
                event.focusAttachments.add(attachment);
            }
            attachment.focus = item.focus().trim();
            attachment.persist();
            keepLineIds.add(line.id);
        }

        List<LineFocusEvent> existing = LineFocusEvent.listForEvent(event.id);
        for (LineFocusEvent attachment : existing) {
            if (!keepLineIds.contains(attachment.line.id)) {
                event.focusAttachments.remove(attachment);
                attachment.delete();
            }
        }
    }

    private EventType findEventTypeOrThrow(UUID eventTypeId) {
        EventType eventType = EventType.findById(eventTypeId);
        if (eventType == null) {
            throw new NotFoundException("Event type " + eventTypeId + " not found");
        }
        return eventType;
    }
}
