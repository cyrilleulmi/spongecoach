package com.spongecoach.api;

import com.spongecoach.api.dto.EventDto;
import com.spongecoach.api.dto.EventUpdateRequest;
import com.spongecoach.api.dto.FocusAttachmentRequest;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.EventType;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.LineFocusEvent;
import com.spongecoach.domain.LineFocusEventId;
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
 * Line (an empty list clears them all). Event fields (type/name/position/date) may also be set.
 */
@Path("/api/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventResource {

    @PUT
    @Path("/{eventId}")
    @Transactional
    public EventDto update(@PathParam("eventId") UUID eventId, EventUpdateRequest request) {
        Event event = Event.findById(eventId);
        if (event == null) {
            throw new NotFoundException("Event " + eventId + " not found");
        }

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
        if (request.focusAttachments() != null) {
            replaceFocusAttachments(event, request.focusAttachments());
        }
        return EventDto.from(event);
    }

    private void replaceFocusAttachments(Event event, List<FocusAttachmentRequest> requested) {
        Set<UUID> keepLineIds = new HashSet<>();
        for (FocusAttachmentRequest item : requested) {
            if (item.lineId() == null || item.focusId() == null) {
                throw new BadRequestException("each focus attachment needs a lineId and a focusId");
            }
            Line line = Line.findById(item.lineId());
            if (line == null) {
                throw new NotFoundException("Line " + item.lineId() + " not found");
            }
            Focus focus = Focus.findById(item.focusId());
            if (focus == null || focus.deletedAt != null) {
                throw new NotFoundException("Focus " + item.focusId() + " not found");
            }

            LineFocusEvent attachment = LineFocusEvent.find(event.id, line.id);
            if (attachment == null) {
                attachment = new LineFocusEvent();
                attachment.id = new LineFocusEventId(event.id, line.id);
                attachment.event = event;
                attachment.line = line;
                event.focusAttachments.add(attachment);
            }
            attachment.focus = focus;
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
