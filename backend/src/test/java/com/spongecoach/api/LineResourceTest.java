package com.spongecoach.api;

import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Event;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Iteration;
import com.spongecoach.domain.Line;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Skill;
import com.spongecoach.support.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LineResourceTest {

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
    void givenALine_whenListingLines_thenItAppearsWithItsPlayerCount() {
        Line line = testData.createLine("Kiwi " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Player carmela = testData.addPlayer(line.id, "Carmela");
        Player debi = testData.addPlayer(line.id, "Debi");
        cleanups.add(() -> testData.deletePlayer(carmela.id));
        cleanups.add(() -> testData.deletePlayer(debi.id));

        given()
                .when().get("/api/lines")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + line.id + "' }.playerCount", equalTo(2))
                .body("find { it.id == '" + line.id + "' }.name", equalTo(line.name));
    }

    @Test
    void givenAnUnknownLine_whenFetchingIt_thenReturnsNotFoundEnvelope() {
        given()
                .when().get("/api/lines/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void givenALineWithRoster_whenFetchingDetail_thenRosterSkillsGoalsAndFocusesAreEmbedded() {
        Line line = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Player player = testData.addPlayer(line.id, "Rahel");
        cleanups.add(() -> testData.deletePlayer(player.id));

        Skill skill = testData.createSkill("Passgenauigkeit " + UUID.randomUUID(), "#2c7a68");
        cleanups.add(() -> testData.deleteSkill(skill.id));
        testData.setRating(line.id, skill.id, 72);

        DevelopmentGoal goal = testData.createGoal("Ballverluste reduzieren " + UUID.randomUUID(), "#c8722e");
        cleanups.add(() -> testData.deleteGoal(goal.id));

        Focus focus = testData.createFocus("Spielaufbau " + UUID.randomUUID(), List.of(goal.id));
        cleanups.add(() -> testData.deleteFocus(focus.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("developmentGoalIds", List.of(goal.id.toString()), "focusIds", List.of(focus.id.toString())))
                .when().put("/api/lines/" + line.id)
                .then()
                .statusCode(200);

        given()
                .when().get("/api/lines/" + line.id)
                .then()
                .statusCode(200)
                .body("id", equalTo(line.id.toString()))
                .body("players.name", hasItem(player.name))
                .body("skills.find { it.skillId == '" + skill.id + "' }.rating", equalTo(72))
                .body("developmentGoals.id", hasItem(goal.id.toString()))
                .body("focuses.id", hasItem(focus.id.toString()))
                .body("focuses.find { it.id == '" + focus.id + "' }.goalIds", hasItem(goal.id.toString()));
    }

    @Test
    void whenListingTeamPlayers_thenTheyAreReturnedRegardlessOfLineMembership() {
        Player rostered = testData.createPlayer("Gina " + UUID.randomUUID());
        Player benchWarmer = testData.createPlayer("Nives " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(rostered.id));
        cleanups.add(() -> testData.deletePlayer(benchWarmer.id));

        given()
                .when().get("/api/players")
                .then()
                .statusCode(200)
                .body("id", hasItem(rostered.id.toString()))
                .body("id", hasItem(benchWarmer.id.toString()));
    }

    @Test
    void whenAssociatingExistingTeamPlayers_thenTheyAppearInTheRoster() {
        Line line = testData.createLine("Lama " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Player anita = testData.createPlayer("Anita " + UUID.randomUUID());
        Player samira = testData.createPlayer("Samira " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(anita.id));
        cleanups.add(() -> testData.deletePlayer(samira.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of(anita.id.toString(), samira.id.toString())))
                .when().put("/api/lines/" + line.id)
                .then()
                .statusCode(200)
                .body("players.id", hasItem(anita.id.toString()))
                .body("players.id", hasItem(samira.id.toString()));
    }

    @Test
    void whenReplacingThePlayerIdArray_thenDroppedPlayersLeaveTheRoster() {
        Line line = testData.createLine("Kiwi " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Player kept = testData.addPlayer(line.id, "Debi " + UUID.randomUUID());
        Player dropped = testData.addPlayer(line.id, "Gina " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(kept.id));
        cleanups.add(() -> testData.deletePlayer(dropped.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of(kept.id.toString())))
                .when().put("/api/lines/" + line.id)
                .then()
                .statusCode(200)
                .body("players.id", hasItem(kept.id.toString()))
                .body("players.id", not(hasItem(dropped.id.toString())));
    }

    @Test
    void aPlayerCanBeRosteredOnMoreThanOneLine() {
        Line kiwi = testData.createLine("Kiwi " + UUID.randomUUID());
        Line bari = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(kiwi.id));
        cleanups.add(() -> testData.deleteLine(bari.id));
        Player allrounder = testData.createPlayer("Sophie " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(allrounder.id));

        for (Line line : List.of(kiwi, bari)) {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("playerIds", List.of(allrounder.id.toString())))
                    .when().put("/api/lines/" + line.id)
                    .then()
                    .statusCode(200);
        }

        given().when().get("/api/lines/" + kiwi.id).then()
                .statusCode(200).body("players.id", hasItem(allrounder.id.toString()));
        given().when().get("/api/lines/" + bari.id).then()
                .statusCode(200).body("players.id", hasItem(allrounder.id.toString()));
    }

    @Test
    void whenAssociatingAnUnknownPlayer_thenReturnsNotFound() {
        Line line = testData.createLine("Kiwi " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of(UUID.randomUUID().toString())))
                .when().put("/api/lines/" + line.id)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void whenSettingARatingForTheFirstTime_thenTheAssociationIsCreated() {
        Line line = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Skill skill = testData.createSkill("Schusshärte " + UUID.randomUUID(), "#5b5ea6");
        cleanups.add(() -> testData.deleteSkill(skill.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("rating", 65))
                .when().put("/api/lines/" + line.id + "/skills/" + skill.id)
                .then()
                .statusCode(200)
                .body("rating", equalTo(65))
                .body("skillId", equalTo(skill.id.toString()));

        given()
                .when().get("/api/lines/" + line.id + "/skills")
                .then()
                .statusCode(200)
                .body("skillId", hasItem(skill.id.toString()));
    }

    @Test
    void whenSettingARatingAgain_thenItOverwritesRatherThanAccumulatingHistory() {
        Line line = testData.createLine("Lama " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Skill skill = testData.createSkill("Stellungsspiel " + UUID.randomUUID(), "#3b6ea5");
        cleanups.add(() -> testData.deleteSkill(skill.id));

        given().contentType(ContentType.JSON).body(Map.of("rating", 40))
                .when().put("/api/lines/" + line.id + "/skills/" + skill.id)
                .then().statusCode(200);

        given().contentType(ContentType.JSON).body(Map.of("rating", 90))
                .when().put("/api/lines/" + line.id + "/skills/" + skill.id)
                .then().statusCode(200).body("rating", equalTo(90));

        given()
                .when().get("/api/lines/" + line.id + "/skills")
                .then()
                .statusCode(200)
                .body("findAll { it.skillId == '" + skill.id + "' }", hasSize(1));
    }

    @Test
    void whenRatingIsOutOfRange_thenReturnsBadRequest() {
        Line line = testData.createLine("Kiwi " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Skill skill = testData.createSkill("Stockführung " + UUID.randomUUID(), "#8a7a2e");
        cleanups.add(() -> testData.deleteSkill(skill.id));

        given().contentType(ContentType.JSON).body(Map.of("rating", 101))
                .when().put("/api/lines/" + line.id + "/skills/" + skill.id)
                .then().statusCode(400).body("error", equalTo("bad_request"));
    }

    @Test
    void whenCreatingALine_thenItAppearsInTheList() {
        String name = "Neue Linie " + UUID.randomUUID();

        String lineId = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name))
                .when().post("/api/lines")
                .then()
                .statusCode(201)
                .body("name", equalTo(name))
                .body("playerCount", equalTo(0))
                .extract().path("id");

        cleanups.add(() -> testData.deleteLine(UUID.fromString(lineId)));

        given()
                .when().get("/api/lines")
                .then()
                .statusCode(200)
                .body("id", hasItem(lineId));
    }

    @Test
    void whenCreatingALineWithoutAName_thenReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "  "))
                .when().post("/api/lines")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"));
    }

    @Test
    void whenDeletingALine_thenItDisappearsButItsHistorySurvives() {
        Line line = testData.createLine("Verschwindet " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Player player = testData.addPlayer(line.id, "Sabrina " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(player.id));

        given()
                .when().delete("/api/lines/" + line.id)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/lines")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(line.id.toString())));
        given()
                .when().get("/api/lines/" + line.id)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));

        assertTrue(testData.lineRowExists(line.id));
        assertEquals(1, testData.lineRosterSize(line.id));
    }

    @Test
    void whenDeletingAnAlreadyDeletedLine_thenReturnsNotFound() {
        Line line = testData.createLine("Doppelt " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));

        given().when().delete("/api/lines/" + line.id).then().statusCode(204);

        given()
                .when().delete("/api/lines/" + line.id)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void whenListingDeletedLines_thenItAppearsWithNameAndPlayerCount() {
        Line line = testData.createLine("Gelöscht " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Player player = testData.addPlayer(line.id, "Nives " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(player.id));

        given().when().delete("/api/lines/" + line.id).then().statusCode(204);

        given()
                .when().get("/api/lines/deleted")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + line.id + "' }.name", equalTo(line.name))
                .body("find { it.id == '" + line.id + "' }.playerCount", equalTo(1));
    }

    @Test
    void whenRestoringADeletedLine_thenItReappearsInTheActiveList() {
        Line line = testData.createLine("Comeback " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));

        given().when().delete("/api/lines/" + line.id).then().statusCode(204);

        given()
                .when().post("/api/lines/" + line.id + "/restore")
                .then()
                .statusCode(200)
                .body("id", equalTo(line.id.toString()))
                .body("name", equalTo(line.name));

        given()
                .when().get("/api/lines")
                .then()
                .statusCode(200)
                .body("id", hasItem(line.id.toString()));
        given()
                .when().get("/api/lines/deleted")
                .then()
                .statusCode(200)
                .body("id", not(hasItem(line.id.toString())));
    }

    @Test
    void whenRestoringALineThatIsNotDeleted_thenReturnsNotFound() {
        Line line = testData.createLine("Aktiv " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));

        given()
                .when().post("/api/lines/" + line.id + "/restore")
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void whenRemovingASkillAssociation_thenItNoLongerAppearsOnTheLine() {
        Line line = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Skill skill = testData.createSkill("Bully-Kontrolle " + UUID.randomUUID(), "#7a4f8a");
        cleanups.add(() -> testData.deleteSkill(skill.id));
        testData.setRating(line.id, skill.id, 50);

        given()
                .when().delete("/api/lines/" + line.id + "/skills/" + skill.id)
                .then()
                .statusCode(204);

        given()
                .when().get("/api/lines/" + line.id + "/skills")
                .then()
                .statusCode(200)
                .body("skillId", not(hasItem(skill.id.toString())));
    }

    @Test
    void whenAddingAPlayerToALineThatAttendsAnUpcomingEvent_thenThePlayerJoinsItsAttendanceList() {
        Line line = testData.createLine("Kiwi " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Iteration iteration = testData.createIteration("Sync " + UUID.randomUUID(), 500);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event event = testData.addTraining(iteration.id, LocalDateTime.now().plusDays(7));
        testData.attendEvent(event.id, line.id);

        Player newcomer = testData.createPlayer("Neu " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(newcomer.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of(newcomer.id.toString())))
                .when().put("/api/lines/" + line.id)
                .then().statusCode(200);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events[0].attendance.find { it.playerId == '" + newcomer.id + "' }.status",
                        equalTo("PENDING"))
                .body("events[0].attendance.find { it.playerId == '" + newcomer.id + "' }.lineIds",
                        hasItem(line.id.toString()));
    }

    @Test
    void whenRemovingAPlayerFromALineThatAttendsAnUpcomingEvent_thenTheyLeaveItsAttendanceList() {
        Line line = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Player player = testData.addPlayer(line.id, "Weg " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(player.id));
        Iteration iteration = testData.createIteration("Sync " + UUID.randomUUID(), 501);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event event = testData.addTraining(iteration.id, LocalDateTime.now().plusDays(7));
        testData.attendEvent(event.id, line.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of()))
                .when().put("/api/lines/" + line.id)
                .then().statusCode(200);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events[0].attendance.playerId", not(hasItem(player.id.toString())));
    }

    @Test
    void whenAPlayerIsRemovedFromOneOfTwoAttendingLines_thenTheyStayOnTheOthersAttendanceList() {
        Line kiwi = testData.createLine("Kiwi " + UUID.randomUUID());
        Line bari = testData.createLine("Bäri " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(kiwi.id));
        cleanups.add(() -> testData.deleteLine(bari.id));
        Player allrounder = testData.createPlayer("Sophie " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(allrounder.id));
        testData.linkPlayer(kiwi.id, allrounder.id);
        testData.linkPlayer(bari.id, allrounder.id);

        Iteration iteration = testData.createIteration("Sync " + UUID.randomUUID(), 502);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event event = testData.addTraining(iteration.id, LocalDateTime.now().plusDays(7));
        testData.attendEvent(event.id, kiwi.id);
        testData.attendEvent(event.id, bari.id);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of()))
                .when().put("/api/lines/" + kiwi.id)
                .then().statusCode(200);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events[0].attendance.find { it.playerId == '" + allrounder.id + "' }.lineIds",
                        hasItem(bari.id.toString()))
                .body("events[0].attendance.find { it.playerId == '" + allrounder.id + "' }.lineIds",
                        not(hasItem(kiwi.id.toString())));
    }

    @Test
    void whenRosterChangesForADoneEvent_thenItsAttendanceSnapshotIsUntouched() {
        Line line = testData.createLine("Lama " + UUID.randomUUID());
        cleanups.add(() -> testData.deleteLine(line.id));
        Iteration iteration = testData.createIteration("Vergangen " + UUID.randomUUID(), 503);
        cleanups.add(() -> testData.deleteIteration(iteration.id));
        Event event = testData.addTraining(iteration.id, LocalDateTime.now().minusDays(1));
        testData.attendEvent(event.id, line.id);

        Player latecomer = testData.createPlayer("Zu spät " + UUID.randomUUID());
        cleanups.add(() -> testData.deletePlayer(latecomer.id));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of(latecomer.id.toString())))
                .when().put("/api/lines/" + line.id)
                .then().statusCode(200);

        given()
                .when().get("/api/iterations/" + iteration.id)
                .then()
                .statusCode(200)
                .body("events[0].attendance.playerId", not(hasItem(latecomer.id.toString())));
    }
}
