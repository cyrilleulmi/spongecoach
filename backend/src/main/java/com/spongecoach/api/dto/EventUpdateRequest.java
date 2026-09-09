package com.spongecoach.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Update an Event. Null fields are left unchanged. A non-null {@code focusAttachments} replaces the
 * event's entire set of per-Line Focus attachments; an empty list clears them all.
 */
public record EventUpdateRequest(
        UUID eventTypeId,
        String name,
        Integer position,
        LocalDate scheduledOn,
        List<FocusAttachmentRequest> focusAttachments) {
}
