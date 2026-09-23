package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** A Player's painted Avatar as PNG bytes (ADR-0016). Its version lives on {@link Player#avatarUpdatedAt}. */
@Entity
@Table(name = "player_avatar")
public class PlayerAvatar extends PanacheEntityBase {

    @Id
    @Column(name = "player_id")
    public UUID playerId;

    public byte[] image;
}
