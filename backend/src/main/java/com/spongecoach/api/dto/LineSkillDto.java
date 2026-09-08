package com.spongecoach.api.dto;

import com.spongecoach.domain.LineSkill;

import java.util.UUID;

public record LineSkillDto(UUID skillId, String name, String color, int rating) {

    public static LineSkillDto from(LineSkill lineSkill) {
        return new LineSkillDto(
                lineSkill.skill.id, lineSkill.skill.name, lineSkill.skill.color, lineSkill.rating);
    }
}
