package com.spongecoach.api.dto;

import java.util.List;
import java.util.UUID;

/** Rename and re-tag a Drill. A null field is left alone; {@code tagIds} replaces the set wholesale. */
public record DrillUpdateRequest(String name, List<UUID> tagIds) {
}
