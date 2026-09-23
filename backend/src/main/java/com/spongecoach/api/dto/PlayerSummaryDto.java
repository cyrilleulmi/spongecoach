package com.spongecoach.api.dto;

import com.spongecoach.domain.Player;

import java.util.List;
import java.util.UUID;

public record PlayerSummaryDto(UUID id, String name, List<CatalogRefDto> lines, Long avatarVersion) {

    public static PlayerSummaryDto from(Player player) {
        return new PlayerSummaryDto(
                player.id,
                player.name,
                player.activeLines().stream().map(CatalogRefDto::from).toList(),
                player.avatarVersion());
    }
}
