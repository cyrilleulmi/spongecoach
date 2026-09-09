package com.spongecoach.domain;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class LineFocusEventId implements Serializable {

    public UUID event;

    public UUID line;

    public LineFocusEventId() {
    }

    public LineFocusEventId(UUID event, UUID line) {
        this.event = event;
        this.line = line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineFocusEventId that)) return false;
        return Objects.equals(event, that.event) && Objects.equals(line, that.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, line);
    }
}
