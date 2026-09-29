package com.spongecoach.spec.support;

import com.spongecoach.domain.DrillStatus;
import com.spongecoach.support.Jpegs;
import com.spongecoach.support.TestData;
import com.spongecoach.support.TestScripts;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * The drill endpoints as the steps call them (ADR-0018). Every request that starts an interpreter
 * job waits for it to finish before returning, so no job outlives its scenario: the stub answers
 * at once, and a job still writing while the scenario's cleanup deletes the Drill would make a
 * puzzling failure somewhere else.
 */
@ScenarioScope
public class DrillClient {

    private static final Duration JOB_TIMEOUT = Duration.ofSeconds(15);

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    /**
     * Uploads a Drill under the actual name its spoken name goes by. A created Drill is registered
     * under the spoken name, queued for removal, and waited for.
     */
    public Response upload(String spokenName, List<byte[]> sketches, List<String> notes, List<UUID> tagIds, String relation) {
        String actualName = fixtures.drillName(spokenName);
        RequestSpecification request = given().multiPart("name", actualName);
        if (relation != null) {
            request.multiPart("sketchRelation", relation);
        }
        for (int i = 0; i < sketches.size(); i++) {
            request.multiPart("sketches", "sketch-" + (i + 1) + ".jpg", sketches.get(i), "image/jpeg");
            request.multiPart("notes", i < notes.size() ? notes.get(i) : "");
        }
        for (UUID tagId : tagIds) {
            request.multiPart("tagIds", tagId.toString());
        }
        Response response = request.when().post("/api/drills");
        if (response.statusCode() == 202) {
            UUID drillId = UUID.fromString(response.path("id"));
            fixtures.registerDrill(spokenName, drillId, actualName);
            awaitJob(drillId);
        }
        return response;
    }

    public Response upload(String spokenName, int sketchCount) {
        List<byte[]> sketches = new ArrayList<>();
        for (int i = 0; i < sketchCount; i++) {
            sketches.add(Jpegs.sketch());
        }
        return upload(spokenName, sketches, List.of(), List.of(), null);
    }

    /** Draws a Drill by hand: the given script. Registered and queued for removal once created. */
    public Response draw(String spokenName, Object script) {
        String actualName = fixtures.drillName(spokenName);
        Response response = given().contentType(ContentType.JSON)
                .body(Map.of("name", actualName, "tagIds", List.of(), "script", script, "changeSummary", "Gezeichnet"))
                .when().post("/api/drills/drawn");
        if (response.statusCode() == 201) {
            fixtures.registerDrill(spokenName, UUID.fromString(response.path("id")), actualName);
        }
        return response;
    }

    public Response draw(String spokenName) {
        return draw(spokenName, TestScripts.drawn("Doppelpass"));
    }

    public Response addSketch(String spokenName, String note) {
        return given().multiPart("sketches", "sketch.jpg", Jpegs.sketch(), "image/jpeg")
                .multiPart("notes", note)
                .when().post("/api/drills/" + id(spokenName) + "/sketches");
    }

    public Response removeSketch(String spokenName, int position) {
        return given().when().delete("/api/drills/" + id(spokenName) + "/sketches/" + position);
    }

    public Response restoreSketch(String spokenName, int position) {
        return given().when().post("/api/drills/" + id(spokenName) + "/sketches/" + position + "/restore");
    }

    public Response restore(String spokenName) {
        return given().when().post("/api/drills/" + id(spokenName) + "/restore");
    }

    public Response listDeleted() {
        return given().when().get("/api/drills/deleted");
    }

    public Response read(String spokenName) {
        return given().when().get("/api/drills/" + id(spokenName));
    }

    public Response rename(String spokenName, String newName, List<UUID> tagIds) {
        return given().contentType(ContentType.JSON)
                .body(tagIds == null ? Map.of("name", newName) : Map.of("name", newName, "tagIds", tagIds))
                .when().put("/api/drills/" + id(spokenName));
    }

    public Response answer(String spokenName, String questionId, String text) {
        return startingJob(spokenName, given().contentType(ContentType.JSON)
                .body(Map.of("answers", List.of(Map.of("questionId", questionId, "text", text)), "guess", false))
                .when().post("/api/drills/" + id(spokenName) + "/answers"));
    }

    public Response guess(String spokenName) {
        return startingJob(spokenName, given().contentType(ContentType.JSON)
                .body(Map.of("answers", List.of(), "guess", true))
                .when().post("/api/drills/" + id(spokenName) + "/answers"));
    }

    public Response chat(String spokenName, String message) {
        return startingJob(spokenName, given().contentType(ContentType.JSON)
                .body(Map.of("message", message))
                .when().post("/api/drills/" + id(spokenName) + "/chat"));
    }

    public Response retry(String spokenName) {
        return startingJob(spokenName, given().when().post("/api/drills/" + id(spokenName) + "/retry"));
    }

    public Response saveScript(String spokenName, Object script) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("script", script, "changeSummary", "Von Hand angepasst"))
                .when().put("/api/drills/" + id(spokenName) + "/script");
    }

    public Response saveHandEdit(String spokenName) {
        return saveScript(spokenName, TestScripts.playable("Handarbeit"));
    }

    public Response revert(String spokenName, int version) {
        return given().when().post("/api/drills/" + id(spokenName) + "/revert/" + version);
    }

    public Response delete(String spokenName) {
        return given().when().delete("/api/drills/" + id(spokenName));
    }

    /** Waits until the Drill's job has finished, whatever its outcome. */
    public void awaitJob(UUID drillId) {
        TestData testData = fixtures.testData();
        Instant deadline = Instant.now().plus(JOB_TIMEOUT);
        while (testData.drillStatus(drillId) == DrillStatus.PENDING) {
            if (Instant.now().isAfter(deadline)) {
                throw new AssertionError("drill " + drillId + " was still being interpreted after " + JOB_TIMEOUT);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting for drill " + drillId, e);
            }
        }
    }

    public UUID id(String spokenName) {
        return world.id(Kind.DRILL, spokenName);
    }

    private Response startingJob(String spokenName, Response response) {
        if (response.statusCode() == 202) {
            awaitJob(id(spokenName));
        }
        return response;
    }
}
