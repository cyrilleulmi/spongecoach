package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Training or a Match — one kind of thing distinguished by its {@link EventType}. Belongs to
 * exactly one {@link Iteration} and holds an integer position within it (ADR-0007). Trainings are
 * numbered by position and carry no name; Matches carry the opponent as their name.
 */
@Entity
@Table(name = "event")
public class Event extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iteration_id", nullable = false)
    public Iteration iteration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id", nullable = false)
    public EventType eventType;

    public String name;

    public int position;

    @Column(name = "scheduled_on")
    public LocalDate scheduledOn;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<LineFocusEvent> focusAttachments = new ArrayList<>();

    public static List<Event> listForIteration(UUID iterationId) {
        return list("iteration.id = ?1 order by position asc", iterationId);
    }
}
