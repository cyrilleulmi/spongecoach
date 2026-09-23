package com.spongecoach.api.dto;

import com.spongecoach.domain.Player;
import com.spongecoach.domain.PlayerSkillRating;

import java.util.List;
import java.util.UUID;

public record PlayerDetailDto(
        UUID id,
        String name,
        List<CatalogRefDto> lines,
        List<PlayerSkillRatingDto> skills,
        List<CatalogRefDto> developmentGoals) {

    public static PlayerDetailDto from(Player player, List<PlayerSkillRating> ratings) {
        return new PlayerDetailDto(
                player.id,
                player.name,
                player.activeLines().stream().map(CatalogRefDto::from).toList(),
                ratings.stream().map(PlayerSkillRatingDto::from).toList(),
                player.developmentGoals.stream()
                        .filter(goal -> goal.deletedAt == null)
                        .map(CatalogRefDto::from)
                        .toList());
    }
}
