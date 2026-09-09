package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

/**
 * The Focus a Line has set for an Event — a three-way link, since a Focus is no longer implicitly
 * one Line's. Payload-carrying join (like {@link LineSkill}): at most one Focus per (Event, Line).
 */
@Entity
@Table(name = "line_focus_event")
public class LineFocusEvent extends PanacheEntityBase {

    @EmbeddedId
    public LineFocusEventId id = new LineFocusEventId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("event")
    @JoinColumn(name = "event_id")
    public Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("line")
    @JoinColumn(name = "line_id")
    public Line line;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "focus_id", nullable = false)
    public Focus focus;

    public static List<LineFocusEvent> listForEvent(UUID eventId) {
        return list("event.id", eventId);
    }

    public static LineFocusEvent find(UUID eventId, UUID lineId) {
        return findById(new LineFocusEventId(eventId, lineId));
    }
}
