package com.spongecoach.api;

import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.Player;
import com.spongecoach.support.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
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
        event = testData.addTraining(iteration.id, LocalDateTime.of(2026, 8, 5, 18, 0));

        kiwi = testData.createLine("Kiwi " + UUID.randomUUID());
        baeri = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(kiwi.id));
        cleanups.add(() -> testData.deleteLine(baeri.id));
        testData.attendEvent(event.id, kiwi.id);
        testData.attendEvent(event.id, baeri.id);

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
    void whenReschedulingOntoASiblingsSlot_thenReturnsConflict() {
        LocalDateTime takenSlot = LocalDateTime.of(2026, 9, 12, 19, 0);
        testData.addTraining(iteration.id, takenSlot);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("scheduledOn", takenSlot.toString()))
                .when().put("/api/events/" + event.id)
                .then()
                .statusCode(409)
                .body("error", equalTo("scheduling_conflict"));
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

    @Test
    void whenAnEventIsCreated_thenEveryAttendingLinesPlayersDefaultToPending() {
        Player carmela = testData.addPlayer(kiwi.id, "Carmela " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(carmela.id));
        testData.attendEvent(event.id, kiwi.id);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events[0].attendance.find { it.playerId == '" + carmela.id + "' }.status",
                        equalTo("PENDING"))
                .body("events[0].attendance.find { it.playerId == '" + carmela.id + "' }.lineIds",
                        hasItem(kiwi.id.toString()));
    }

    @Test
    void whenSettingAPlayerToAttending_thenTheStatusIsUpdatedAndNoDeclineMessageIsKept() {
        Player carmela = testData.addPlayer(kiwi.id, "Carmela " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(carmela.id));
        testData.attendEvent(event.id, kiwi.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "ATTENDING"))
                .when().put("/api/events/" + event.id + "/attendance/" + carmela.id)
                .then()
                .statusCode(200)
                .body("attendance.find { it.playerId == '" + carmela.id + "' }.status", equalTo("ATTENDING"))
                .body("attendance.find { it.playerId == '" + carmela.id + "' }.declineMessage", equalTo(null));
    }

    @Test
    void whenDecliningWithAMessage_thenTheMessageIsStored() {
        Player carmela = testData.addPlayer(kiwi.id, "Carmela " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(carmela.id));
        testData.attendEvent(event.id, kiwi.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "DECLINED", "declineMessage", "Verletzt"))
                .when().put("/api/events/" + event.id + "/attendance/" + carmela.id)
                .then()
                .statusCode(200)
                .body("attendance.find { it.playerId == '" + carmela.id + "' }.status", equalTo("DECLINED"))
                .body("attendance.find { it.playerId == '" + carmela.id + "' }.declineMessage", equalTo("Verletzt"));
    }

    @Test
    void whenSwitchingFromDeclinedBackToAttending_thenTheDeclineMessageIsCleared() {
        Player carmela = testData.addPlayer(kiwi.id, "Carmela " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(carmela.id));
        testData.attendEvent(event.id, kiwi.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "DECLINED", "declineMessage", "Verletzt"))
                .when().put("/api/events/" + event.id + "/attendance/" + carmela.id)
                .then().statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "ATTENDING"))
                .when().put("/api/events/" + event.id + "/attendance/" + carmela.id)
                .then()
                .statusCode(200)
                .body("attendance.find { it.playerId == '" + carmela.id + "' }.status", equalTo("ATTENDING"))
                .body("attendance.find { it.playerId == '" + carmela.id + "' }.declineMessage", equalTo(null));
    }

    @Test
    void whenSettingAttendanceForAPlayerNotOnTheEvent_thenReturnsNotFound() {
        Player benched = testData.createPlayer("Nicht dabei " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(benched.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "ATTENDING"))
                .when().put("/api/events/" + event.id + "/attendance/" + benched.id)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void whenSettingAnUnknownAttendanceStatus_thenReturnsBadRequest() {
        Player carmela = testData.addPlayer(kiwi.id, "Carmela " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(carmela.id));
        testData.attendEvent(event.id, kiwi.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "MAYBE"))
                .when().put("/api/events/" + event.id + "/attendance/" + carmela.id)
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"));
    }

    @Test
    void whenAPlayerIsOnTwoAttendingLines_thenBothLineIdsAppearOnTheirSingleAttendanceEntry() {
        Player allrounder = testData.createPlayer("Sophie " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(allrounder.id));
        testData.linkPlayer(kiwi.id, allrounder.id);
        testData.linkPlayer(baeri.id, allrounder.id);
        testData.attendEvent(event.id, kiwi.id);
        testData.attendEvent(event.id, baeri.id);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events[0].attendance.findAll { it.playerId == '" + allrounder.id + "' }", hasSize(1))
                .body("events[0].attendance.find { it.playerId == '" + allrounder.id + "' }.lineIds",
                        hasItem(kiwi.id.toString()))
                .body("events[0].attendance.find { it.playerId == '" + allrounder.id + "' }.lineIds",
                        hasItem(baeri.id.toString()));
    }
}
