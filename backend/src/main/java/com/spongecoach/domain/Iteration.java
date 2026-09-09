package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A named group of {@link Event}s, ordered relative to other Iterations by a plain integer
 * position (ADR-0007). The unit a coach plans around — a training block or phase.
 */
@Entity
@Table(name = "iteration")
public class Iteration extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    public Team team;

    public String name;

    public int position;

    @OneToMany(mappedBy = "iteration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position asc")
    public List<Event> events = new ArrayList<>();

    public static List<Iteration> listAllOrderedByPosition() {
        return list("order by position asc");
    }
}
