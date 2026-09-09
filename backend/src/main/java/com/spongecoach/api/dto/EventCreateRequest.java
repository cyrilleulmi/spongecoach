package com.spongecoach.api.dto;

import java.time.LocalDate;
import java.util.UUID;

/** A single Event to create — nested inside {@link IterationCreateRequest} or posted on its own. */
public record EventCreateRequest(UUID eventTypeId, String name, Integer position, LocalDate scheduledOn) {
}
