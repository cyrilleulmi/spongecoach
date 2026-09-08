package com.spongecoach.api.dto;

import com.spongecoach.domain.Focus;

import java.util.List;
import java.util.UUID;

public record FocusDto(UUID id, String name, List<UUID> goalIds) {

    public static FocusDto from(Focus focus) {
        return new FocusDto(
                focus.id,
                focus.name,
                focus.developmentGoals.stream().map(g -> g.id).toList());
    }
}
