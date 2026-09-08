package com.spongecoach.api.dto;

import java.util.List;
import java.util.UUID;

public record LineUpdateRequest(
        String name, List<UUID> playerIds, List<UUID> developmentGoalIds, List<UUID> focusIds) {
}
