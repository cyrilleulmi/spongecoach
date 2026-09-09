package com.spongecoach.api.dto;

import java.util.List;

/** Create an Iteration with its initial Events nested in one call. */
public record IterationCreateRequest(String name, Integer position, List<EventCreateRequest> events) {
}
