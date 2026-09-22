package com.spongecoach.api.dto;

import com.spongecoach.domain.LineFocusEvent;

import java.util.UUID;

/** One Line's free-text Focus set for an Event, denormalised with its Line name for the
 * read-side timeline. */
public record FocusAttachmentDto(UUID lineId, String lineName, String focus) {

    public static FocusAttachmentDto from(LineFocusEvent attachment) {
        return new FocusAttachmentDto(attachment.line.id, attachment.line.name, attachment.focus);
    }
}
