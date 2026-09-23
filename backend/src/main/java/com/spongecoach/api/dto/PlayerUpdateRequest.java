package com.spongecoach.api.dto;

import java.util.List;
import java.util.UUID;

public record PlayerUpdateRequest(List<UUID> developmentGoalIds) {
}
