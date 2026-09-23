package com.spongecoach.api.dto;

import com.spongecoach.domain.PlayerSkillRating;

import java.util.UUID;

public record PlayerSkillRatingDto(UUID skillId, String name, String color, int rating) {

    public static PlayerSkillRatingDto from(PlayerSkillRating rating) {
        return new PlayerSkillRatingDto(
                rating.playerSkill.id, rating.playerSkill.name, rating.playerSkill.color, rating.rating);
    }
}
