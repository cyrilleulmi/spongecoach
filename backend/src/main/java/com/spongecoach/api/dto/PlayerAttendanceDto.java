package com.spongecoach.api.dto;

import java.util.List;
import java.util.UUID;

/** One Player's attendance answer for an Event, with every attending Line they belong to for it. */
public record PlayerAttendanceDto(
        UUID playerId,
        String playerName,
        Long avatarVersion,
        List<UUID> lineIds,
        String status,
        String declineMessage) {
}
