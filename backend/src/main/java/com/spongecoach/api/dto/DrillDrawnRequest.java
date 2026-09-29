package com.spongecoach.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

/** A Drill drawn by hand in the animator: no photos, no interpreter (ADR-0020). */
public record DrillDrawnRequest(String name, List<UUID> tagIds, JsonNode script, String changeSummary) {
}
