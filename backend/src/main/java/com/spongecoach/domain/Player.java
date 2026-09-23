package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "player")
public class Player extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    public Team team;

    public String name;

    /** When the painted Avatar was last saved; null while the Player shows initials (ADR-0016). */
    @Column(name = "avatar_updated_at")
    public Instant avatarUpdatedAt;

    /** Includes soft-deleted Lines; callers filter. */
    @ManyToMany(mappedBy = "players", fetch = FetchType.LAZY)
    @OrderBy("name asc")
    public List<Line> lines = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "player_player_development_goal",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "player_development_goal_id"))
    @OrderBy("name asc")
    public List<PlayerDevelopmentGoal> developmentGoals = new ArrayList<>();

    public static List<Player> listAllOrderedByName() {
        return list("order by name asc");
    }

    /** Cache-busting token for the Avatar image URL; null means no Avatar, show initials. */
    public Long avatarVersion() {
        return avatarUpdatedAt == null ? null : avatarUpdatedAt.toEpochMilli();
    }

    public List<Line> activeLines() {
        return lines.stream().filter(line -> line.deletedAt == null).toList();
    }
}
