package com.spongecoach.api.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Update an Event. Null fields are left unchanged. A non-null {@code focusAttachments} replaces the
 * event's entire set of per-Line Focus attachments; an empty list clears them all. A non-null
 * {@code scheduledOn} must be unique within the event's Iteration (ADR-0011); it can never be
 * cleared back to null since every Event requires a datetime.
 */
public record EventUpdateRequest(
        UUID eventTypeId,
        String name,
        LocalDateTime scheduledOn,
        List<FocusAttachmentRequest> focusAttachments) {
}
