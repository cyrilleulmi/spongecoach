package com.spongecoach.api.dto;

import java.util.List;
import java.util.UUID;

/** A Focus is derived from one or more Development goals; goalIds must be non-empty. */
public record FocusCreateRequest(String name, List<UUID> goalIds) {
}
