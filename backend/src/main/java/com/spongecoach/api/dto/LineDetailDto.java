package com.spongecoach.api.dto;

import com.spongecoach.domain.Line;
import com.spongecoach.domain.LineSkill;

import java.util.List;
import java.util.UUID;

public record LineDetailDto(
        UUID id,
        String name,
        List<PlayerDto> players,
        List<LineSkillDto> skills,
        List<CatalogRefDto> developmentGoals,
        List<FocusDto> focuses) {

    public static LineDetailDto from(Line line, List<LineSkill> lineSkills) {
        return new LineDetailDto(
                line.id,
                line.name,
                line.players.stream().map(PlayerDto::from).toList(),
                lineSkills.stream().map(LineSkillDto::from).toList(),
                line.developmentGoals.stream().map(CatalogRefDto::from).toList(),
                line.focuses.stream().map(FocusDto::from).toList());
    }
}
