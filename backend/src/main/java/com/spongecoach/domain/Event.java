package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Training or a Match — one kind of thing distinguished by its {@link EventType}. Belongs to
 * exactly one {@link Iteration} and is ordered within it by {@code scheduledOn}, which is
 * mandatory and unique per Iteration (ADR-0011). Trainings are numbered by that order and carry no
 * name; Matches carry the opponent as their name.
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

    @Column(name = "scheduled_on", nullable = false)
    public LocalDateTime scheduledOn;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<LineFocusEvent> focusAttachments = new ArrayList<>();

    /**
     * The Lines attending this Event — a snapshot taken once at creation (every currently active
     * Line, for now; a future "don't invite every Line" feature would let this be edited per
     * Event), not a live/derived set. A Line added afterwards never joins retroactively; a
     * since-deleted Line still resolves here since it's a plain FK (ADR-0012).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_line",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "line_id"))
    @OrderBy("name asc")
    public List<Line> lines = new ArrayList<>();

    /** Snapshot of each attending Line's roster at the time attendance was frozen. */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<EventLinePlayer> linePlayers = new ArrayList<>();

    /** One answer per Player who's on this Event's attendance list (see {@link EventLinePlayer}). */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<EventAttendance> attendance = new ArrayList<>();

    public static List<Event> listForIteration(UUID iterationId) {
        return list("iteration.id = ?1 order by scheduledOn asc", iterationId);
    }

    /** Not-done Events (see ADR-0012) that the given Line attends — the events whose attendance
     * snapshot a Line roster change should still be able to reach. */
    public static List<Event> listAttendingLineNotDone(UUID lineId, LocalDateTime now) {
        return list("select e from Event e join e.lines l where l.id = ?1 and e.scheduledOn >= ?2", lineId, now);
    }

    public static boolean existsAtSlot(UUID iterationId, LocalDateTime scheduledOn, UUID excludingEventId) {
        if (excludingEventId == null) {
            return count("iteration.id = ?1 and scheduledOn = ?2", iterationId, scheduledOn) > 0;
        }
        return count(
                "iteration.id = ?1 and scheduledOn = ?2 and id <> ?3",
                iterationId, scheduledOn, excludingEventId)
                > 0;
    }
}
