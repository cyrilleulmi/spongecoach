package com.spongecoach.domain;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class EventLinePlayerId implements Serializable {

    public UUID event;

    public UUID line;

    public UUID player;

    public EventLinePlayerId() {
    }

    public EventLinePlayerId(UUID event, UUID line, UUID player) {
        this.event = event;
        this.line = line;
        this.player = player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventLinePlayerId that)) return false;
        return Objects.equals(event, that.event)
                && Objects.equals(line, that.line)
                && Objects.equals(player, that.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, line, player);
    }
}
