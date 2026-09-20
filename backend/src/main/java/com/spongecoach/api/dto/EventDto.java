package com.spongecoach.api.dto;

import com.spongecoach.domain.AttendanceStatus;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.EventAttendance;
import com.spongecoach.domain.EventLinePlayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record EventDto(
        UUID id,
        UUID typeId,
        String type,
        String name,
        LocalDateTime scheduledOn,
        List<FocusAttachmentDto> focusAttachments,
        List<CatalogRefDto> lines,
        List<PlayerAttendanceDto> attendance) {

    public static EventDto from(Event event) {
        return new EventDto(
                event.id,
                event.eventType.id,
                event.eventType.name,
                event.name,
                event.scheduledOn,
                event.focusAttachments.stream()
                        .map(FocusAttachmentDto::from)
                        .sorted(Comparator.comparing(FocusAttachmentDto::lineName))
                        .toList(),
                event.lines.stream().map(CatalogRefDto::from).toList(),
                attendanceFor(event));
    }

    private static List<PlayerAttendanceDto> attendanceFor(Event event) {
        Map<UUID, List<UUID>> lineIdsByPlayer = new LinkedHashMap<>();
        Map<UUID, String> nameByPlayer = new HashMap<>();
        for (EventLinePlayer link : event.linePlayers) {
            lineIdsByPlayer.computeIfAbsent(link.player.id, id -> new ArrayList<>()).add(link.line.id);
            nameByPlayer.put(link.player.id, link.player.name);
        }
        Map<UUID, EventAttendance> attendanceByPlayer = event.attendance.stream()
                .collect(Collectors.toMap(a -> a.player.id, a -> a));

        return lineIdsByPlayer.keySet().stream()
                .map(playerId -> {
                    EventAttendance attendance = attendanceByPlayer.get(playerId);
                    return new PlayerAttendanceDto(
                            playerId,
                            nameByPlayer.get(playerId),
                            lineIdsByPlayer.get(playerId),
                            (attendance != null ? attendance.status : AttendanceStatus.PENDING).name(),
                            attendance != null ? attendance.declineMessage : null);
                })
                .sorted(Comparator.comparing(PlayerAttendanceDto::playerName))
                .toList();
    }
}
