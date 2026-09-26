package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

/**
 * Someone using the app, with a {@link Role} (ADR-0017). Seeded only in v1, and picked from a
 * dropdown rather than logged in to. Team and Player are plain id columns rather than associations:
 * a User is read once per request, outside the resource's transaction, and only its ids are needed.
 */
@Entity
@Table(name = "app_user")
public class AppUser extends PanacheEntityBase {

    @Id
    public UUID id;

    public String name;

    @Enumerated(EnumType.STRING)
    public Role role;

    /** The Team a Coach or Player belongs to; null for a SysAdmin. */
    @Column(name = "team_id")
    public UUID teamId;

    /** The Player this User acts as; required for a Player, optional for a Coach. */
    @Column(name = "player_id")
    public UUID playerId;

    public static List<AppUser> listAllOrdered() {
        return list("order by role asc, name asc");
    }
}
