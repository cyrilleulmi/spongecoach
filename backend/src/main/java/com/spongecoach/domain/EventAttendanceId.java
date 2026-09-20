package com.spongecoach.domain;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class EventAttendanceId implements Serializable {

    public UUID event;

    public UUID player;

    public EventAttendanceId() {
    }

    public EventAttendanceId(UUID event, UUID player) {
        this.event = event;
        this.player = player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventAttendanceId that)) return false;
        return Objects.equals(event, that.event) && Objects.equals(player, that.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, player);
    }
}
