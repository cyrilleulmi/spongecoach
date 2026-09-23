package com.spongecoach.api.dto;

import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.PlayerDevelopmentGoal;
import com.spongecoach.domain.PlayerSkill;
import com.spongecoach.domain.Skill;

import java.util.UUID;

public record CatalogRefDto(UUID id, String name, String color) {

    public static CatalogRefDto from(Skill skill) {
        return new CatalogRefDto(skill.id, skill.name, skill.color);
    }

    public static CatalogRefDto from(DevelopmentGoal goal) {
        return new CatalogRefDto(goal.id, goal.name, goal.color);
    }

    public static CatalogRefDto from(PlayerSkill skill) {
        return new CatalogRefDto(skill.id, skill.name, skill.color);
    }

    public static CatalogRefDto from(PlayerDevelopmentGoal goal) {
        return new CatalogRefDto(goal.id, goal.name, goal.color);
    }

    public static CatalogRefDto from(Line line) {
        return new CatalogRefDto(line.id, line.name, line.color);
    }
}
