package com.spongecoach.api.dto;

import java.util.UUID;

public record FocusAttachmentRequest(UUID lineId, UUID focusId) {
}
