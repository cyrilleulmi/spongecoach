package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "line")
public class Line extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    public Team team;

    public String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "line_player",
            joinColumns = @JoinColumn(name = "line_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id"))
    @OrderBy("name asc")
    public List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<LineSkill> lineSkills = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "line_development_goal",
            joinColumns = @JoinColumn(name = "line_id"),
            inverseJoinColumns = @JoinColumn(name = "development_goal_id"))
    public List<DevelopmentGoal> developmentGoals = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "line_focus",
            joinColumns = @JoinColumn(name = "line_id"),
            inverseJoinColumns = @JoinColumn(name = "focus_id"))
    public List<Focus> focuses = new ArrayList<>();

    public static List<Line> listAllOrderedByName() {
        return list("order by name asc");
    }
}
