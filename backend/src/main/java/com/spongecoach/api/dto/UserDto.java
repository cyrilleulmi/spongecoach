package com.spongecoach.api.dto;

import com.spongecoach.domain.AppUser;

import java.util.UUID;

public record UserDto(UUID id, String name, String role, UUID playerId) {

    public static UserDto from(AppUser user) {
        return new UserDto(user.id, user.name, user.role.name(), user.playerId);
    }
}
