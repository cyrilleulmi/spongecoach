package com.spongecoach.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.spongecoach.domain.Drill;
import com.spongecoach.domain.DrillMessage;
import com.spongecoach.domain.DrillMessageAuthor;
import com.spongecoach.domain.DrillScriptSource;
import com.spongecoach.domain.DrillScriptVersion;
import com.spongecoach.domain.DrillSketch;
import com.spongecoach.domain.DrillStatus;
import com.spongecoach.domain.SketchRelation;
import com.spongecoach.drill.DrillJson;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Everything the drill screen shows, in one read: the sketches with what the interpreter read on
 * them, the current script, its version history and the conversation. What the frontend polls
 * while a job runs (ADR-0019). The JSON documents are handed out as stored.
 *
 * @param openQuestions the Clarifying questions waiting for an answer; empty unless NEEDS_INPUT
 */
public record DrillDetailDto(
        UUID id,
        String name,
        DrillStatus status,
        String error,
        SketchRelation sketchRelation,
        List<DrillTagDto> tags,
        List<Sketch> sketches,
        Integer currentVersion,
        JsonNode script,
        List<Version> versions,
        List<Message> messages,
        JsonNode openQuestions) {

    public record Sketch(int position, String note, JsonNode reading) {
    }

    public record Version(int version, DrillScriptSource source, String changeSummary, Instant createdAt) {
    }

    public record Message(
            int position,
            DrillMessageAuthor author,
            String content,
            JsonNode questions,
            JsonNode answers,
            Integer scriptVersion,
            Instant createdAt) {
    }

    public static DrillDetailDto from(Drill drill) {
        List<DrillScriptVersion> versions = DrillScriptVersion.listForDrill(drill.id);
        JsonNode script = versions.stream()
                .filter(version -> drill.currentVersion != null && version.version == drill.currentVersion)
                .findFirst()
                .map(version -> DrillJson.tree(version.script))
                .orElse(null);
        DrillMessage last = drill.messages.isEmpty() ? null : drill.messages.get(drill.messages.size() - 1);
        JsonNode openQuestions = drill.status == DrillStatus.NEEDS_INPUT && last != null && last.questions != null
                ? DrillJson.tree(last.questions)
                : DrillJson.tree("[]");
        return new DrillDetailDto(
                drill.id,
                drill.name,
                drill.status,
                drill.error,
                drill.sketchRelation,
                drill.tags.stream().map(DrillTagDto::from).toList(),
                drill.sketches.stream().map(DrillDetailDto::sketch).toList(),
                drill.currentVersion,
                script,
                versions.stream()
                        .map(version -> new Version(version.version, version.source, version.changeSummary, version.createdAt))
                        .toList(),
                drill.messages.stream().map(DrillDetailDto::message).toList(),
                openQuestions);
    }

    private static Sketch sketch(DrillSketch sketch) {
        return new Sketch(sketch.position, sketch.note, DrillJson.tree(sketch.reading));
    }

    private static Message message(DrillMessage message) {
        return new Message(
                message.position,
                message.author,
                message.content,
                DrillJson.tree(message.questions),
                DrillJson.tree(message.answers),
                message.scriptVersion,
                message.createdAt);
    }
}
