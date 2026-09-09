package com.spongecoach.api;

import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.support.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class EventResourceTest {

    @Inject
    TestData testData;

    private final List<Runnable> cleanups = new ArrayList<>();

    private Iteration iteration;
    private Event event;
    private Line kiwi;
    private Line baeri;
    private Focus focusA;
    private Focus focusB;

    @BeforeEach
    void setUp() {
        iteration = testData.createIteration("Iteration " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        event = testData.addTraining(iteration.id, 1);

        kiwi = testData.createLine("Kiwi " + UUID.randomUUID());
        baeri = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(kiwi.id));
        cleanups.add(() -> testData.deleteLine(baeri.id));

        DevelopmentGoal goal = testData.createGoal("Ziel " + UUID.randomUUID(), "#c0392b");
        cleanups.add(() -> testData.deleteGoal(goal.id));
        focusA = testData.createFocus("Spielaufbau " + UUID.randomUUID(), List.of(goal.id));
        focusB = testData.createFocus("Abschluss " + UUID.randomUUID(), List.of(goal.id));
        cleanups.add(() -> testData.deleteFocus(focusA.id));
        cleanups.add(() -> testData.deleteFocus(focusB.id));
    }

    @AfterEach
    void cleanUp() {
        for (int i = cleanups.size() - 1; i >= 0; i--) {
            cleanups.get(i).run();
        }
        cleanups.clear();
    }

    @Test
    void whenSettingFocusAttachments_thenEachLineGetsItsFocusInline() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(
                        Map.of("lineId", kiwi.id.toString(), "focusId", focusA.id.toString()),
                        Map.of("lineId", baeri.id.toString(), "focusId", focusB.id.toString()))))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(200)
                .body("focusAttachments", hasSize(2))
                .body("focusAttachments.find { it.lineId == '" + kiwi.id + "' }.focusId",
                        equalTo(focusA.id.toString()));
    }

    @Test
    void whenReplacingFocusAttachmentsWithAShorterSet_thenDroppedLinesAreCleared() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(
                        Map.of("lineId", kiwi.id.toString(), "focusId", focusA.id.toString()),
                        Map.of("lineId", baeri.id.toString(), "focusId", focusB.id.toString()))))
                .when().put("/api/events/" + event.id)
                .then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(
                        Map.of("lineId", kiwi.id.toString(), "focusId", focusB.id.toString()))))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(200)
                .body("focusAttachments", hasSize(1))
                .body("focusAttachments[0].lineId", equalTo(kiwi.id.toString()))
                .body("focusAttachments[0].focusId", equalTo(focusB.id.toString()));
    }

    @Test
    void whenSettingAnEmptyAttachmentList_thenAllAreCleared() {
        testData.attachFocus(event.id, kiwi.id, focusA.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of()))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(200)
                .body("focusAttachments", hasSize(0));
    }

    @Test
    void whenAttachingAFocusForAnUnknownLine_thenReturnsNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(
                        Map.of("lineId", UUID.randomUUID().toString(), "focusId", focusA.id.toString()))))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void whenAttachingAnUnknownFocus_thenReturnsNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(
                        Map.of("lineId", kiwi.id.toString(), "focusId", UUID.randomUUID().toString()))))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(404);
    }

    @Test
    void whenUpdatingAnUnknownEvent_thenReturnsNotFound() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "egal"))
                .when().put("/api/events/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void whenRenamingAnEventViaEventPut_thenTheNameIsUpdatedButAttachmentsAreUntouched() {
        testData.attachFocus(event.id, kiwi.id, focusA.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Testspiel gegen Bern"))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(200)
                .body("name", equalTo("Testspiel gegen Bern"))
                .body("focusAttachments", hasSize(1));
    }

    @Test
    void givenAnAttachedFocus_whenChangingOnlyThatLinesFocus_thenTheOthersRemain() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(
                        Map.of("lineId", kiwi.id.toString(), "focusId", focusA.id.toString()),
                        Map.of("lineId", baeri.id.toString(), "focusId", focusA.id.toString()))))
                .when().put("/api/events/" + event.id)
                .then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(
                        Map.of("lineId", kiwi.id.toString(), "focusId", focusA.id.toString()),
                        Map.of("lineId", baeri.id.toString(), "focusId", focusB.id.toString()))))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(200)
                .body("focusAttachments", hasSize(2))
                .body("focusAttachments.find { it.lineId == '" + baeri.id + "' }.focusId",
                        equalTo(focusB.id.toString()))
                .body("focusAttachments.find { it.lineId == '" + kiwi.id + "' }.focusId",
                        equalTo(focusA.id.toString()));
    }
}
