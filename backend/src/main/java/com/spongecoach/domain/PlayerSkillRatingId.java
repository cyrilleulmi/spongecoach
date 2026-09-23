package com.spongecoach.domain;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PlayerSkillRatingId implements Serializable {

    public UUID player;

    public UUID playerSkill;

    public PlayerSkillRatingId() {
    }

    public PlayerSkillRatingId(UUID player, UUID playerSkill) {
        this.player = player;
        this.playerSkill = playerSkill;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerSkillRatingId that)) return false;
        return Objects.equals(player, that.player) && Objects.equals(playerSkill, that.playerSkill);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, playerSkill);
    }
}
