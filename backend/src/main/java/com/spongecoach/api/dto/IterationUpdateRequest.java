package com.spongecoach.api.dto;

/** Full-resource update of an Iteration's own fields; null fields are left unchanged. */
public record IterationUpdateRequest(String name, Integer position) {
}
