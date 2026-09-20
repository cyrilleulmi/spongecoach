package com.spongecoach.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A Player's answer for one Event: {@link AttendanceStatus}, plus an optional message when
 * declined. Exactly one row per (Event, Player) regardless of how many attending Lines that
 * Player is snapshotted onto for the Event (see {@link EventLinePlayer}).
 */
@Entity
@Table(name = "event_attendance")
public class EventAttendance extends PanacheEntityBase {

    @EmbeddedId
    public EventAttendanceId id = new EventAttendanceId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("event")
    @JoinColumn(name = "event_id")
    public Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("player")
    @JoinColumn(name = "player_id")
    public Player player;

    @Enumerated(EnumType.STRING)
    public AttendanceStatus status = AttendanceStatus.PENDING;

    @Column(name = "decline_message")
    public String declineMessage;

    public static EventAttendance find(UUID eventId, UUID playerId) {
        return findById(new EventAttendanceId(eventId, playerId));
    }
}
