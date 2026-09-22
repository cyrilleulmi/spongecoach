package com.spongecoach.api.dto;

import java.util.UUID;

public record FocusAttachmentRequest(UUID lineId, String focus) {
}
