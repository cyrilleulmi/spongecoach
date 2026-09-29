package com.spongecoach.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** A hand-edited script. Parsed strictly, so an unknown field is a 400 rather than silently dropped. */
public record DrillScriptRequest(JsonNode script, String changeSummary) {
}
