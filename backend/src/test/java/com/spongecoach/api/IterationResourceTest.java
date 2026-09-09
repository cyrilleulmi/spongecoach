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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class IterationResourceTest {

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
    void givenAnIterationWithEvents_whenListing_thenItsEventsAreNestedInPositionOrder() {
        Iteration iteration = testData.createIteration("Vorbereitung " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        testData.addTraining(iteration.id, 2);
        testData.addTraining(iteration.id, 1);
        testData.createEvent(iteration.id, testData.eventType("Match").id, "Testspiel", 3, null);

        given()
                .when().get("/api/iterations")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + iteration.id + "' }.events.size()", equalTo(3))
                .body("find { it.id == '" + iteration.id + "' }.events[0].position", equalTo(1))
                .body("find { it.id == '" + iteration.id + "' }.events[0].type", equalTo("Training"))
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
                                Map.of("eventTypeId", trainingType.toString(), "position", 1),
                                Map.of("eventTypeId", trainingType.toString(), "position", 2),
                                Map.of("eventTypeId", matchType.toString(), "name", "Derby", "position", 3))))
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
        testData.addTraining(iteration.id, 1);

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
    void whenAddingAnEventWithNoPosition_thenItIsAppendedAfterTheLast() {
        Iteration iteration = testData.createIteration("Anhängen " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        testData.addTraining(iteration.id, 1);
        testData.addTraining(iteration.id, 2);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("eventTypeId", testData.eventType("Match").id.toString(), "name", "Schluss"))
                .when().post("/api/iterations/" + iteration.id + "/events")
                .then()
                .statusCode(201)
                .body("position", equalTo(3))
                .body("type", equalTo("Match"));
    }

    @Test
    void whenReorderingAnEventViaIterationScopedPut_thenItsPositionChanges() {
        Iteration iteration = testData.createIteration("Sortieren " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event training = testData.addTraining(iteration.id, 1);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("position", 9))
                .when().put("/api/iterations/" + iteration.id + "/events/" + training.id)
                .then()
                .statusCode(200)
                .body("position", equalTo(9));
    }

    @Test
    void whenDeletingASingleEvent_thenOnlyThatEventIsRemoved() {
        Iteration iteration = testData.createIteration("Einzeln " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event kept = testData.addTraining(iteration.id, 1);
        Event dropped = testData.addTraining(iteration.id, 2);

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
                .body(Map.of("eventTypeId", UUID.randomUUID().toString()))
                .when().post("/api/iterations/" + iteration.id + "/events")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void givenEventsWithFocusAttachments_whenListing_thenAttachmentsAreInlineWithLineAndFocusNames() {
        Iteration iteration = testData.createIteration("Fokusse " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event training = testData.addTraining(iteration.id, 1);

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
                .body("events[0].scheduledOn", nullValue());
    }

    @Test
    void whenAddingAnEventWithADate_thenItIsReturnedOnTheEvent() {
        Iteration iteration = testData.createIteration("Datum " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "eventTypeId", testData.eventType("Training").id.toString(),
                        "position", 1,
                        "scheduledOn", LocalDate.of(2026, 10, 5).toString()))
                .when().post("/api/iterations/" + iteration.id + "/events")
                .then()
                .statusCode(201)
                .body("scheduledOn", equalTo("2026-10-05"));
    }
}
