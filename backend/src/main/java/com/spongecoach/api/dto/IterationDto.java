package com.spongecoach.api.dto;

import com.spongecoach.domain.Iteration;

import java.util.List;
import java.util.UUID;

public record IterationDto(UUID id, String name, int position, List<EventDto> events) {

    public static IterationDto from(Iteration iteration) {
        return new IterationDto(
                iteration.id,
                iteration.name,
                iteration.position,
                iteration.events.stream().map(EventDto::from).toList());
    }
}
