package com.spongecoach.api.dto;

import com.spongecoach.domain.Line;

import java.util.UUID;

public record LineSummaryDto(UUID id, String name, int playerCount) {

    public static LineSummaryDto from(Line line) {
        return new LineSummaryDto(line.id, line.name, line.players.size());
    }
}
