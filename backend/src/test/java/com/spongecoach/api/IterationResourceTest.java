package com.spongecoach.api;

import com.spongecoach.domain.Event;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.support.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class IterationResourceTest {

    /** The API always echoes scheduledOn with seconds, unlike LocalDateTime#toString(). */
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Inject
    TestData testData;

    private final List<Runnable> cleanups = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (int i = cleanups.size() - 1; i >= 0; i--) {
            cleanups.get(i).run();
        }
        cleanups.clear();
    }

    @Test
    void givenAnIterationWithEvents_whenListing_thenItsEventsAreNestedInScheduledOnOrder() {
        Iteration iteration = testData.createIteration("Vorbereitung " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        LocalDateTime earliest = LocalDateTime.of(2026, 8, 5, 18, 0);
        testData.addTraining(iteration.id, LocalDateTime.of(2026, 8, 12, 18, 0));
        testData.addTraining(iteration.id, earliest);
        testData.createEvent(
                iteration.id, testData.eventType("Match").id, "Testspiel", LocalDateTime.of(2026, 8, 19, 19, 0));

        given()
                .when().get("/api/iterations")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + iteration.id + "' }.events.size()", equalTo(3))
                .body("find { it.id == '" + iteration.id + "' }.events[0].type", equalTo("Training"))
                .body("find { it.id == '" + iteration.id + "' }.events[0].scheduledOn",
                        equalTo(earliest.format(ISO)))
                .body("find { it.id == '" + iteration.id + "' }.events[2].type", equalTo("Match"))
                .body("find { it.id == '" + iteration.id + "' }.events[2].name", equalTo("Testspiel"));
    }

    @Test
    void givenAnUnknownIteration_whenFetchingIt_thenReturnsNotFoundEnvelope() {
        given()
                .when().get("/api/iterations/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void whenCreatingAnIterationWithNestedEvents_thenAllArePersistedInOrder() {
        UUID trainingType = testData.eventType("Training").id;
        UUID matchType = testData.eventType("Match").id;

        String iterationId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "Rückrunde " + UUID.randomUUID(),
                        "position", 900,
                        "events", List.of(
                                Map.of("eventTypeId", trainingType.toString(), "scheduledOn", "2026-09-01T18:00:00"),
                                Map.of("eventTypeId", trainingType.toString(), "scheduledOn", "2026-09-08T18:00:00"),
                                Map.of(
                                        "eventTypeId", matchType.toString(),
                                        "name", "Derby",
                                        "scheduledOn", "2026-09-15T19:00:00"))))
                .when().post("/api/iterations")
                .then()
                .statusCode(201)
                .body("events.size()", equalTo(3))
                .body("events[2].name", equalTo("Derby"))
                .extract().path("id");

        cleanups.add(() -> testData.deleteIteration(UUID.fromString(iterationId)));

        given()
                .when().get("/api/iterations/" + iterationId)
                .then()
                .statusCode(200)
                .body("events.type", hasItem("Match"));
    }

    @Test
    void whenCreatingAnIterationWithoutAName_thenReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("position", 1))
                .when().post("/api/iterations")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"));
    }

    @Test
    void whenUpdatingAnIterationName_thenItIsChanged() {
        Iteration iteration = testData.createIteration("Alt " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Neu", "position", 7))
                .when().put("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Neu"))
                .body("position", equalTo(7));
    }

    @Test
    void whenDeletingAnIteration_thenItAndItsEventsDisappear() {
        Iteration iteration = testData.createIteration("Weg " + UUID.randomUUID(), 500);
        testData.addTraining(iteration.id, LocalDateTime.of(2026, 8, 5, 18, 0));

        given()
                .when().delete("/api/iterations/" + iteration.id)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/iterations")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(iteration.id.toString())));
        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(404);
    }

    @Test
    void whenAddingAnEventWithoutScheduledOn_thenReturnsBadRequest() {
        Iteration iteration = testData.createIteration("Ohne Datum " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("eventTypeId", testData.eventType("Match").id.toString(), "name", "Schluss"))
                .when().post("/api/iterations/" + iteration.id + "/events")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"));
    }

    @Test
    void whenAddingAnEventAtAnAlreadyUsedSlot_thenReturnsConflict() {
        Iteration iteration = testData.createIteration("Kollision " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        LocalDateTime slot = LocalDateTime.of(2026, 8, 5, 18, 0);
        testData.addTraining(iteration.id, slot);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "eventTypeId", testData.eventType("Training").id.toString(),
                        "scheduledOn", slot.toString()))
                .when().post("/api/iterations/" + iteration.id + "/events")
                .then()
                .statusCode(409)
                .body("error", equalTo("scheduling_conflict"));
    }

    @Test
    void givenTwoIterations_whenAddingEventsAtTheSameSlotInEach_thenBothSucceed() {
        Iteration first = testData.createIteration("Erste " + UUID.randomUUID(), 500);
        Iteration second = testData.createIteration("Zweite " + UUID.randomUUID(), 501);
        cleanups.add(() -> testData.deleteIteration(first.id));
        cleanups.add(() -> testData.deleteIteration(second.id));
        LocalDateTime slot = LocalDateTime.of(2026, 8, 5, 18, 0);
        testData.addTraining(first.id, slot);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "eventTypeId", testData.eventType("Training").id.toString(),
                        "scheduledOn", slot.toString()))
                .when().post("/api/iterations/" + second.id + "/events")
                .then()
                .statusCode(201);
    }

    @Test
    void whenReschedulingAnEventViaIterationScopedPut_thenItsScheduledOnChanges() {
        Iteration iteration = testData.createIteration("Umplanen " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event training = testData.addTraining(iteration.id, LocalDateTime.of(2026, 8, 5, 18, 0));
        LocalDateTime rescheduled = LocalDateTime.of(2026, 8, 12, 18, 0);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("scheduledOn", rescheduled.toString()))
                .when().put("/api/iterations/" + iteration.id + "/events/" + training.id)
                .then()
                .statusCode(200)
                .body("scheduledOn", equalTo(rescheduled.format(ISO)));
    }

    @Test
    void whenReschedulingAnEventToItsOwnCurrentSlot_thenItIsAccepted() {
        Iteration iteration = testData.createIteration("Selbst " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        LocalDateTime slot = LocalDateTime.of(2026, 8, 5, 18, 0);
        Event training = testData.addTraining(iteration.id, slot);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("scheduledOn", slot.toString()))
                .when().put("/api/iterations/" + iteration.id + "/events/" + training.id)
                .then()
                .statusCode(200)
                .body("scheduledOn", equalTo(slot.format(ISO)));
    }

    @Test
    void whenReschedulingAnEventOntoASiblingsSlot_thenReturnsConflict() {
        Iteration iteration = testData.createIteration("Doppelt " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        LocalDateTime takenSlot = LocalDateTime.of(2026, 8, 12, 18, 0);
        testData.addTraining(iteration.id, takenSlot);
        Event toMove = testData.addTraining(iteration.id, LocalDateTime.of(2026, 8, 5, 18, 0));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("scheduledOn", takenSlot.toString()))
                .when().put("/api/iterations/" + iteration.id + "/events/" + toMove.id)
                .then()
                .statusCode(409)
                .body("error", equalTo("scheduling_conflict"));
    }

    @Test
    void whenDeletingASingleEvent_thenOnlyThatEventIsRemoved() {
        Iteration iteration = testData.createIteration("Einzeln " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event kept = testData.addTraining(iteration.id, LocalDateTime.of(2026, 8, 5, 18, 0));
        Event dropped = testData.addTraining(iteration.id, LocalDateTime.of(2026, 8, 12, 18, 0));

        given()
                .when().delete("/api/iterations/" + iteration.id + "/events/" + dropped.id)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events.id", hasItem(kept.id.toString()))
                .body("events.id", not(hasItem(dropped.id.toString())));
    }

    @Test
    void whenAddingAnEventWithUnknownType_thenReturnsNotFound() {
        Iteration iteration = testData.createIteration("Unbekannt " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "eventTypeId", UUID.randomUUID().toString(),
                        "scheduledOn", "2026-08-05T18:00:00"))
                .when().post("/api/iterations/" + iteration.id + "/events")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void givenEventsWithFocusAttachments_whenListing_thenAttachmentsAreInlineWithLineAndFocusNames() {
        Iteration iteration = testData.createIteration("Fokusse " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        LocalDateTime slot = LocalDateTime.of(2026, 8, 5, 18, 0);
        Event training = testData.addTraining(iteration.id, slot);

        Line line = testData.createLine("Kiwi " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        var goal = testData.createGoal("Ziel " + UUID.randomUUID(), "#c0392b");
        cleanups.add(() -> testData.deleteGoal(goal.id));
        Focus focus = testData.createFocus("Cross-Pässe " + UUID.randomUUID(), List.of(goal.id));
        cleanups.add(() -> testData.deleteFocus(focus.id));
        testData.attachFocus(training.id, line.id, focus.id);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events[0].focusAttachments.find { it.lineId == '" + line.id + "' }.focusName",
                        equalTo(focus.name))
                .body("events[0].focusAttachments.find { it.lineId == '" + line.id + "' }.lineName",
                        equalTo(line.name))
                .body("events[0].scheduledOn", equalTo(slot.format(ISO)));
    }

    @Test
    void whenAddingAnEventWithADatetime_thenItIsReturnedOnTheEvent() {
        Iteration iteration = testData.createIteration("Datum " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        LocalDateTime slot = LocalDateTime.of(2026, 10, 5, 18, 0);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "eventTypeId", testData.eventType("Training").id.toString(),
                        "scheduledOn", slot.toString()))
                .when().post("/api/iterations/" + iteration.id + "/events")
                .then()
                .statusCode(201)
                .body("scheduledOn", equalTo(slot.format(ISO)));
    }
}
