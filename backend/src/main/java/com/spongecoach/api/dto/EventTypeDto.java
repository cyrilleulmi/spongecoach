package com.spongecoach.api.dto;

import com.spongecoach.domain.EventType;

import java.util.UUID;

public record EventTypeDto(UUID id, String name) {

    public static EventTypeDto from(EventType eventType) {
        return new EventTypeDto(eventType.id, eventType.name);
    }
}
