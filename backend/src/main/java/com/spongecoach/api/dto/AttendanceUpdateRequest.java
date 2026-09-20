package com.spongecoach.api.dto;

/** Sets one Player's attendance answer for an Event. {@code declineMessage} is only kept when
 * {@code status} is {@code DECLINED} — the server clears it for any other status. */
public record AttendanceUpdateRequest(String status, String declineMessage) {
}
