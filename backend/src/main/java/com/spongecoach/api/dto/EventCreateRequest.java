package com.spongecoach.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single Event to create — nested inside {@link IterationCreateRequest} or posted on its own.
 * {@code scheduledOn} is mandatory (ADR-0011): it drives ordering and must be unique within the
 * Iteration.
 */
public record EventCreateRequest(UUID eventTypeId, String name, LocalDateTime scheduledOn) {
}
