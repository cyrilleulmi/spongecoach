package com.spongecoach.api.dto;

import com.spongecoach.domain.AppUser;

import java.util.List;
import java.util.UUID;

/** The current User, with the active Lines their Player is on ({@code lineIds}, empty if none). */
public record CurrentUserDto(UUID id, String name, String role, UUID teamId, UUID playerId, List<UUID> lineIds) {

    public static CurrentUserDto from(AppUser user, List<UUID> lineIds) {
        return new CurrentUserDto(user.id, user.name, user.role.name(), user.teamId, user.playerId, lineIds);
    }
}
