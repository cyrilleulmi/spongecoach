package com.spongecoach.api.dto;

import com.spongecoach.domain.Player;

import java.util.UUID;

public record PlayerDto(UUID id, String name) {

    public static PlayerDto from(Player player) {
        return new PlayerDto(player.id, player.name);
    }
}
