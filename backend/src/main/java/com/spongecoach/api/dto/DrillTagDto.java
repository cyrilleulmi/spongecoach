package com.spongecoach.api.dto;

import com.spongecoach.domain.DrillTag;

import java.util.UUID;

public record DrillTagDto(UUID id, String name) {

    public static DrillTagDto from(DrillTag tag) {
        return new DrillTagDto(tag.id, tag.name);
    }
}
