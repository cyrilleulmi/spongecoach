package com.spongecoach.api.dto;

import com.spongecoach.domain.LineFocusEvent;

import java.util.UUID;

/** One Line's Focus set for an Event, denormalised with names for the read-side timeline. */
public record FocusAttachmentDto(UUID lineId, String lineName, UUID focusId, String focusName) {

    public static FocusAttachmentDto from(LineFocusEvent attachment) {
        return new FocusAttachmentDto(
                attachment.line.id,
                attachment.line.name,
                attachment.focus.id,
                attachment.focus.name);
    }
}
