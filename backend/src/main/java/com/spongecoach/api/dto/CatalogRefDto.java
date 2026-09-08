package com.spongecoach.api.dto;

import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Skill;

import java.util.UUID;

public record CatalogRefDto(UUID id, String name, String color) {

    public static CatalogRefDto from(Skill skill) {
        return new CatalogRefDto(skill.id, skill.name, skill.color);
    }

    public static CatalogRefDto from(DevelopmentGoal goal) {
        return new CatalogRefDto(goal.id, goal.name, goal.color);
    }
}
