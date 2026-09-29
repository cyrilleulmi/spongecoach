package com.spongecoach.api.dto;

import com.spongecoach.domain.Drill;
import com.spongecoach.domain.DrillStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A Drill in the list: enough to show and filter it, without its sketches or script. */
public record DrillSummaryDto(
        UUID id, String name, DrillStatus status, List<DrillTagDto> tags, long sketchCount, Instant updatedAt) {

    public static DrillSummaryDto from(Drill drill, long sketchCount) {
        return new DrillSummaryDto(
                drill.id,
                drill.name,
                drill.status,
                drill.tags.stream().map(DrillTagDto::from).toList(),
                sketchCount,
                drill.updatedAt);
    }
}
