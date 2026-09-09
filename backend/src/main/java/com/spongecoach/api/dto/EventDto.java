package com.spongecoach.api.dto;

import com.spongecoach.domain.Event;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record EventDto(
        UUID id,
        UUID typeId,
        String type,
        String name,
        int position,
        LocalDate scheduledOn,
        List<FocusAttachmentDto> focusAttachments) {

    public static EventDto from(Event event) {
        return new EventDto(
                event.id,
                event.eventType.id,
                event.eventType.name,
                event.name,
                event.position,
                event.scheduledOn,
                event.focusAttachments.stream()
                        .map(FocusAttachmentDto::from)
                        .sorted(Comparator.comparing(FocusAttachmentDto::lineName))
                        .toList());
    }
}
