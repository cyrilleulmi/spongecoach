package com.spongecoach.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.spongecoach.api.dto.DrillAnswersRequest;
import com.spongecoach.api.dto.DrillChatRequest;
import com.spongecoach.api.dto.DrillDetailDto;
import com.spongecoach.api.dto.DrillScriptRequest;
import com.spongecoach.api.dto.DrillSummaryDto;
import com.spongecoach.api.dto.DrillUpdateRequest;
import com.spongecoach.auth.Access;
import com.spongecoach.domain.Drill;
import com.spongecoach.domain.DrillMessage;
import com.spongecoach.domain.DrillMessageAuthor;
import com.spongecoach.domain.DrillScriptSource;
import com.spongecoach.domain.DrillScriptVersion;
import com.spongecoach.domain.DrillSketch;
import com.spongecoach.domain.DrillStatus;
import com.spongecoach.domain.DrillTag;
import com.spongecoach.domain.SketchRelation;
import com.spongecoach.domain.Team;
import com.spongecoach.drill.DrillJobs;
import com.spongecoach.drill.DrillJson;
import com.spongecoach.drill.DrillScript;
import com.spongecoach.drill.DrillScriptValidator;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drills animated from tactic-board photos (ADR-0018). Every User may read them; every write is a
 * Coach's, because most of them start a paid interpreter job (ADR-0019). Those jobs run in the
 * background: the request marks the Drill PENDING and answers 202, and the frontend polls the
 * detail until the status moves on.
 */
@Path("/api/drills")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DrillResource {

    static final int MAX_SKETCHES = 12;
    static final int MAX_SKETCH_BYTES = 3 * 1024 * 1024;
    private static final String JPEG = "image/jpeg";

    @Inject
    Access access;

    @Inject
    DrillJobs jobs;

    @GET
    @Transactional
    public List<DrillSummaryDto> list() {
        return Drill.listActive().stream()
                .map(drill -> DrillSummaryDto.from(drill, DrillSketch.count("drill.id", drill.id)))
                .toList();
    }

    @GET
    @Path("/{drillId}")
    @Transactional
    public DrillDetailDto get(@PathParam("drillId") UUID drillId) {
        return DrillDetailDto.from(findOrThrow(drillId));
    }

    /** A sketch never changes once uploaded, so its URL is cached for good. */
    @GET
    @Path("/{drillId}/sketches/{position}")
    @Produces(JPEG)
    @Transactional
    public Response getSketch(@PathParam("drillId") UUID drillId, @PathParam("position") int position) {
        Drill drill = findOrThrow(drillId);
        DrillSketch sketch = drill.sketches.stream()
                .filter(candidate -> candidate.position == position)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Drill " + drillId + " has no sketch " + position));
        return Response.ok(sketch.image, JPEG)
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .build();
    }

    /**
     * Uploads a Drill: its name, 1-12 JPEG sketches in order (downscaled by the browser), a note per
     * sketch, tags and how the sketches relate. Starts the first interpretation.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response create(
            @RestForm String name,
            @RestForm String sketchRelation,
            @RestForm("tagIds") List<String> tagIds,
            @RestForm("sketches") List<FileUpload> sketches,
            @RestForm("notes") List<String> notes) {
        Team team = Team.theTeam();
        access.requireCoach(team);
        String trimmedName = requireName(name);
        SketchRelation relation = parseRelation(sketchRelation);
        List<byte[]> images = readSketches(sketches);

        Instant now = Instant.now();
        Drill drill = new Drill();
        drill.id = UUID.randomUUID();
        drill.team = team;
        drill.name = trimmedName;
        drill.sketchRelation = relation;
        drill.status = DrillStatus.PENDING;
        drill.createdAt = now;
        drill.updatedAt = now;
        drill.tags = findTags(parseIds(tagIds));
        for (int i = 0; i < images.size(); i++) {
            DrillSketch sketch = new DrillSketch();
            sketch.id = UUID.randomUUID();
            sketch.drill = drill;
            sketch.position = i + 1;
            sketch.image = images.get(i);
            String note = notes != null && i < notes.size() ? notes.get(i) : null;
            sketch.note = note == null || note.isBlank() ? null : note.trim();
            drill.sketches.add(sketch);
        }
        drill.persist();
        jobs.startAfterCommit(drill.id);
        return Response.accepted(DrillDetailDto.from(drill)).build();
    }

    @PUT
    @Path("/{drillId}")
    @Transactional
    public DrillDetailDto update(@PathParam("drillId") UUID drillId, DrillUpdateRequest request) {
        Drill drill = findOrThrow(drillId);
        access.requireCoach(drill.team);
        if (request == null) {
            throw new BadRequestException("body is required");
        }
        if (request.name() != null) {
            drill.name = requireName(request.name());
        }
        if (request.tagIds() != null) {
            drill.tags = findTags(request.tagIds());
        }
        drill.updatedAt = Instant.now();
        return DrillDetailDto.from(drill);
    }

    /** Answers the open Clarifying questions, or asks the interpreter to decide them itself. */
    @POST
    @Path("/{drillId}/answers")
    @Transactional
    public Response answer(@PathParam("drillId") UUID drillId, DrillAnswersRequest request) {
        Drill drill = findOrThrow(drillId);
        access.requireCoach(drill.team);
        requireNotBusy(drill);
        if (drill.status != DrillStatus.NEEDS_INPUT) {
            throw new BadRequestException("Drill " + drillId + " has no open questions");
        }
        if (request == null || (!request.guess() && (request.answers() == null || request.answers().isEmpty()))) {
            throw new BadRequestException("answer at least one question, or guess");
        }
        Map<String, String> questions = openQuestions(drill);
        List<Map<String, String>> answers = new ArrayList<>();
        for (DrillAnswersRequest.Answer answer : request.answers() == null ? List.<DrillAnswersRequest.Answer>of() : request.answers()) {
            if (!questions.containsKey(answer.questionId())) {
                throw new BadRequestException("no open question " + answer.questionId());
            }
            if (answer.text() == null || answer.text().isBlank()) {
                continue;
            }
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("questionId", answer.questionId());
            entry.put("question", questions.get(answer.questionId()));
            entry.put("answer", answer.text().trim());
            answers.add(entry);
        }
        String text = request.guess()
                ? "Rate einfach: entscheide alle noch offenen Punkte selbst und liste sie als Annahmen auf."
                : null;
        if (request.message() != null && !request.message().isBlank()) {
            text = text == null ? request.message().trim() : text + "\n\n" + request.message().trim();
        }
        addCoachMessage(drill, text, answers.isEmpty() ? null : DrillJson.write(answers));
        return startJob(drill);
    }

    /** A correction in the coach's words; the interpreter answers with a new version or asks back. */
    @POST
    @Path("/{drillId}/chat")
    @Transactional
    public Response chat(@PathParam("drillId") UUID drillId, DrillChatRequest request) {
        Drill drill = findOrThrow(drillId);
        access.requireCoach(drill.team);
        requireNotBusy(drill);
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new BadRequestException("message must not be empty");
        }
        addCoachMessage(drill, request.message().trim(), null);
        return startJob(drill);
    }

    /** Runs the interpreter again with the conversation as it stands: after a failure, or for a fresh attempt. */
    @POST
    @Path("/{drillId}/retry")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public Response retry(@PathParam("drillId") UUID drillId) {
        Drill drill = findOrThrow(drillId);
        access.requireCoach(drill.team);
        requireNotBusy(drill);
        return startJob(drill);
    }

    /** Saves a hand-edited script as a new version (ADR-0018). */
    @PUT
    @Path("/{drillId}/script")
    @Transactional
    public DrillDetailDto saveScript(@PathParam("drillId") UUID drillId, DrillScriptRequest request) {
        Drill drill = findOrThrow(drillId);
        access.requireCoach(drill.team);
        requireNotBusy(drill);
        if (request == null || request.script() == null) {
            throw new BadRequestException("script is required");
        }
        DrillScript script = parseScript(request.script());
        List<String> errors = DrillScriptValidator.validate(script, drill.sketches.size());
        if (!errors.isEmpty()) {
            throw new BadRequestException("script is not playable: " + String.join("; ", errors));
        }
        DrillJobs.newVersion(drill, script, DrillScriptSource.EDIT, request.changeSummary());
        drill.status = DrillStatus.READY;
        drill.error = null;
        drill.updatedAt = Instant.now();
        return DrillDetailDto.from(drill);
    }

    /** Undo: makes an earlier version current again, as a new version so nothing is lost. */
    @POST
    @Path("/{drillId}/revert/{version}")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public DrillDetailDto revert(@PathParam("drillId") UUID drillId, @PathParam("version") int version) {
        Drill drill = findOrThrow(drillId);
        access.requireCoach(drill.team);
        requireNotBusy(drill);
        DrillScriptVersion earlier = DrillScriptVersion.find(drillId, version);
        if (earlier == null) {
            throw new NotFoundException("Drill " + drillId + " has no version " + version);
        }
        DrillScript script;
        try {
            script = DrillJson.read(earlier.script, DrillScript.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("stored version " + version + " of drill " + drillId + " is unreadable", e);
        }
        DrillJobs.newVersion(drill, script, DrillScriptSource.REVERT, "Zurück zu Version " + version);
        drill.status = DrillStatus.READY;
        drill.error = null;
        drill.updatedAt = Instant.now();
        return DrillDetailDto.from(drill);
    }

    /** Soft delete (ADR-0004): the Drill disappears from the list, its rows stay. */
    @DELETE
    @Path("/{drillId}")
    @Transactional
    public Response delete(@PathParam("drillId") UUID drillId) {
        Drill drill = findOrThrow(drillId);
        access.requireCoach(drill.team);
        drill.deletedAt = Instant.now();
        return Response.noContent().build();
    }

    private Response startJob(Drill drill) {
        drill.status = DrillStatus.PENDING;
        drill.error = null;
        drill.updatedAt = Instant.now();
        jobs.startAfterCommit(drill.id);
        return Response.accepted(DrillDetailDto.from(drill)).build();
    }

    private static void addCoachMessage(Drill drill, String content, String answers) {
        DrillMessage message = new DrillMessage();
        message.id = UUID.randomUUID();
        message.drill = drill;
        message.position = drill.messages.size() + 1;
        message.author = DrillMessageAuthor.COACH;
        message.content = content;
        message.answers = answers;
        message.createdAt = Instant.now();
        message.persist();
        drill.messages.add(message);
    }

    /** The last interpreter message's questions, by id. */
    private static Map<String, String> openQuestions(Drill drill) {
        Map<String, String> questions = new LinkedHashMap<>();
        DrillMessage last = drill.messages.get(drill.messages.size() - 1);
        JsonNode node = DrillJson.tree(last.questions);
        if (node != null) {
            node.forEach(question -> questions.put(question.path("id").asText(), question.path("text").asText()));
        }
        return questions;
    }

    private static void requireNotBusy(Drill drill) {
        if (drill.status == DrillStatus.PENDING) {
            throw new DrillBusyException("Drill " + drill.id + " is still being interpreted");
        }
    }

    private static DrillScript parseScript(JsonNode node) {
        try {
            return DrillJson.convert(node, DrillScript.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("script does not match the drill script format: " + e.getOriginalMessage());
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("name must not be empty");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 200) {
            throw new BadRequestException("name must be at most 200 characters");
        }
        return trimmed;
    }

    private static SketchRelation parseRelation(String relation) {
        if (relation == null || relation.isBlank()) {
            return SketchRelation.MIXED;
        }
        try {
            return SketchRelation.valueOf(relation);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("sketchRelation must be PROGRESSION, CONTINUOUS or MIXED");
        }
    }

    /** JPEGs by signature, 1 to {@link #MAX_SKETCHES} of them, each at most {@link #MAX_SKETCH_BYTES}. */
    private static List<byte[]> readSketches(List<FileUpload> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            throw new BadRequestException("at least one sketch is required");
        }
        if (uploads.size() > MAX_SKETCHES) {
            throw new BadRequestException("at most " + MAX_SKETCHES + " sketches");
        }
        List<byte[]> images = new ArrayList<>();
        for (FileUpload upload : uploads) {
            byte[] image;
            try {
                image = Files.readAllBytes(upload.uploadedFile());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (image.length < 3 || (image[0] & 0xff) != 0xff || (image[1] & 0xff) != 0xd8 || (image[2] & 0xff) != 0xff) {
                throw new BadRequestException("sketches must be JPEG images");
            }
            if (image.length > MAX_SKETCH_BYTES) {
                throw new BadRequestException("a sketch must be at most " + MAX_SKETCH_BYTES / (1024 * 1024) + " MB");
            }
            images.add(image);
        }
        return images;
    }

    private static List<UUID> parseIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        try {
            return ids.stream().filter(id -> !id.isBlank()).map(UUID::fromString).toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("tagIds must be UUIDs");
        }
    }

    private static List<DrillTag> findTags(List<UUID> tagIds) {
        List<DrillTag> tags = new ArrayList<>();
        for (UUID tagId : tagIds) {
            DrillTag tag = DrillTag.findById(tagId);
            if (tag == null) {
                throw new BadRequestException("unknown drill tag " + tagId);
            }
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private static Drill findOrThrow(UUID drillId) {
        Drill drill = Drill.findById(drillId);
        if (drill == null || drill.deletedAt != null) {
            throw new NotFoundException("Drill " + drillId + " not found");
        }
        return drill;
    }
}
