package com.spongecoach.api;

import com.spongecoach.api.dto.AttendanceUpdateRequest;
import com.spongecoach.api.dto.EventDto;
import com.spongecoach.api.dto.EventUpdateRequest;
import com.spongecoach.api.dto.FocusAttachmentRequest;
import com.spongecoach.api.dto.FocusRequest;
import com.spongecoach.auth.Access;
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
import jakarta.inject.Inject;
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
 * Editing a single Event by id. A coach may replace the Event's whole set of Focus attachments via
 * {@code focusAttachments} — one Focus per Line, an empty list clears them all — and set its own
 * fields (type/name/date). One Line's Focus alone is set via {@code /focus/{lineId}}, which is what
 * a Player on that Line may use (ADR-0017). A
 * {@code scheduledOn} change must land on a datetime not already used by another Event in the same
 * Iteration (ADR-0011); a collision is rejected with 409.
 */
@Path("/api/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    @Inject
    Access access;

    @PUT
    @Path("/{eventId}")
    @Transactional
    public EventDto update(@PathParam("eventId") UUID eventId, EventUpdateRequest request) {
        Event event = findEventOrThrow(eventId);
        access.requireCoach(event.iteration.team);

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
     * Sets or clears one Line's Focus for the Event, leaving every other Line's alone. The Line must
     * be one of the Event's attending Lines.
     */
    @PUT
    @Path("/{eventId}/focus/{lineId}")
    @Transactional
    public EventDto setFocus(
            @PathParam("eventId") UUID eventId, @PathParam("lineId") UUID lineId, FocusRequest request) {
        Event event = findEventOrThrow(eventId);
        Line line = Line.findById(lineId);
        if (line == null) {
            throw new NotFoundException("Line " + lineId + " not found");
        }
        if (event.lines.stream().noneMatch(l -> l.id.equals(lineId))) {
            throw new BadRequestException("Line " + lineId + " is not attending event " + eventId);
        }
        access.requireFocus(event, line);

        String focus = request == null ? null : blankToNull(request.focus());
        LineFocusEvent attachment = LineFocusEvent.find(eventId, lineId);
        if (focus == null) {
            if (attachment != null) {
                event.focusAttachments.remove(attachment);
                attachment.delete();
            }
            return EventDto.from(event);
        }
        if (attachment == null) {
            attachment = new LineFocusEvent();
            attachment.id = new LineFocusEventId(eventId, lineId);
            attachment.event = event;
            attachment.line = line;
            event.focusAttachments.add(attachment);
        }
        attachment.focus = focus;
        attachment.persist();
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
        access.requireAttendance(event, playerId);
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
