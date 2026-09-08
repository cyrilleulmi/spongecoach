package com.spongecoach.domain;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class LineSkillId implements Serializable {

    public UUID line;

    public UUID skill;

    public LineSkillId() {
    }

    public LineSkillId(UUID line, UUID skill) {
        this.line = line;
        this.skill = skill;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineSkillId that)) return false;
        return Objects.equals(line, that.line) && Objects.equals(skill, that.skill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, skill);
    }
}
