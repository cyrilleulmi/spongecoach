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

/**
 * Snapshot of a Player's Line membership for an Event, frozen when the attendance snapshot is
 * taken (Event creation, or a later roster change while the Event is still upcoming — see
 * {@code LineResource#update}). A Player on two attending Lines gets two rows here for the same
 * Event; {@link EventAttendance} still holds exactly one answer per (Event, Player).
 */
@Entity
@Table(name = "event_line_player")
public class EventLinePlayer extends PanacheEntityBase {

    @EmbeddedId
    public EventLinePlayerId id = new EventLinePlayerId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("event")
    @JoinColumn(name = "event_id")
    public Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("line")
    @JoinColumn(name = "line_id")
    public Line line;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("player")
    @JoinColumn(name = "player_id")
    public Player player;

    public static List<EventLinePlayer> listForEvent(UUID eventId) {
        return list("event.id", eventId);
    }

    public static EventLinePlayer find(UUID eventId, UUID lineId, UUID playerId) {
        return findById(new EventLinePlayerId(eventId, lineId, playerId));
    }

    public static long countForEventAndPlayer(UUID eventId, UUID playerId) {
        return count("event.id = ?1 and player.id = ?2", eventId, playerId);
    }
}
