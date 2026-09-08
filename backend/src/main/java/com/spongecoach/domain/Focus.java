package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "focus")
public class Focus extends PanacheEntityBase {

    @Id
    public UUID id;

    public String name;

    @Column(name = "deleted_at")
    public Instant deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "focus_development_goal",
            joinColumns = @JoinColumn(name = "focus_id"),
            inverseJoinColumns = @JoinColumn(name = "development_goal_id"))
    public List<DevelopmentGoal> developmentGoals = new ArrayList<>();

    public static List<Focus> listActive() {
        return list("deletedAt is null order by name asc");
    }
}
