package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "player_skill_rating")
public class PlayerSkillRating extends PanacheEntityBase {

    @EmbeddedId
    public PlayerSkillRatingId id = new PlayerSkillRatingId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("player")
    @JoinColumn(name = "player_id")
    public Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playerSkill")
    @JoinColumn(name = "player_skill_id")
    public PlayerSkill playerSkill;

    public int rating;

    public static PlayerSkillRating find(UUID playerId, UUID playerSkillId) {
        return findById(new PlayerSkillRatingId(playerId, playerSkillId));
    }

    public static List<PlayerSkillRating> listForPlayer(UUID playerId) {
        return list("player.id = ?1 and playerSkill.deletedAt is null order by playerSkill.name asc", playerId);
    }
}
