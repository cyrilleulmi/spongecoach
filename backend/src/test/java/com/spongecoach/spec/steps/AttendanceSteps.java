package com.spongecoach.spec.steps;

import com.spongecoach.spec.support.Fixtures;
import com.spongecoach.spec.support.Kind;
import com.spongecoach.spec.support.Names;
import com.spongecoach.spec.support.ScenarioWorld;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/** Proves docs/spec/attendance.feature. */
@ScenarioScope
public class AttendanceSteps {

    private static final LocalDateTime DEFAULT_SLOT = LocalDateTime.of(2026, 8, 5, 18, 0);

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    /** The Player the last answer step acted on — what a following "her" refers to. */
    private String subjectPlayer;

    // --- Given ------------------------------------------------------------------

    @Given("an Event {string}")
    public void anEvent(String eventName) {
        fixtures.event("Planung", "Training", eventName, DEFAULT_SLOT);
    }

    @Given("an upcoming Event {string}")
    public void anUpcomingEvent(String eventName) {
        fixtures.event("Planung", "Training", eventName, LocalDateTime.now().plusDays(7));
    }

    @Given("a done Event {string}")
    public void aDoneEvent(String eventName) {
        fixtures.event("Planung", "Training", eventName, LocalDateTime.now().minusDays(1));
    }

    @Given("the coach has set {string} to {string} on {string} with the message {string}")
    public void theCoachHasSetTo(String playerName, String status, String eventName, String message) {
        setAttendance(playerName, status, eventName, message).then().statusCode(200);
    }

    // --- When -------------------------------------------------------------------

    @When("an Event {string} is created")
    public void anEventIsCreated(String eventName) {
        anEvent(eventName);
    }

    @When("^the coach creates a Line \"([^\"]*)\" rostering (.+)$")
    public void theCoachCreatesALineRostering(String lineName, String players) {
        fixtures.line(lineName);
        Names.in(players).forEach(player -> fixtures.playerOn(lineName, player));
    }

    @When("the coach deletes the Line {string}")
    public void theCoachDeletesTheLine(String lineName) {
        world.setResponse(given().when().delete("/api/lines/" + world.id(Kind.LINE, lineName)));
        world.response().then().statusCode(204);
    }

    @When("{string}'s attendance is read")
    public void theEventsAttendanceIsRead(String eventName) {
        world.setResponse(readTimeline());
    }

    @When("the coach clears {string}'s roster")
    public void theCoachClearsTheRoster(String lineName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of()))
                .when().put("/api/lines/" + world.id(Kind.LINE, lineName)));
        world.response().then().statusCode(200);
    }

    @When("the coach sets {string} to {string} on {string}")
    public void theCoachSetsTo(String playerName, String status, String eventName) {
        subjectPlayer = playerName;
        world.setResponse(setAttendance(playerName, status, eventName, null));
    }

    @When("the coach sets {string} to {string} on {string} with the message {string}")
    public void theCoachSetsToWithMessage(String playerName, String status, String eventName, String message) {
        subjectPlayer = playerName;
        world.setResponse(setAttendance(playerName, status, eventName, message));
    }

    private Response setAttendance(String playerName, String status, String eventName, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("status", status);
        if (message != null) {
            body.put("declineMessage", message);
        }
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().put("/api/events/" + world.id(Kind.EVENT, eventName)
                        + "/attendance/" + world.id(Kind.PLAYER, playerName));
    }

    private Response readTimeline() {
        return given().when().get("/api/iterations/" + world.current(Kind.ITERATION));
    }

    // --- Then -------------------------------------------------------------------

    @Then("^(.+) are on its attendance list with status \"([^\"]*)\"$")
    public void arePlayersOnTheAttendanceListWithStatus(String players, String status) {
        var assertion = readTimeline().then().statusCode(200);
        for (String player : Names.in(players)) {
            assertion.body(eventPath() + ".attendance.find { it.playerId == '"
                    + world.id(Kind.PLAYER, player) + "' }.status", equalTo(status));
        }
    }

    @Then("{string} is on {string}'s attendance list with status {string}")
    public void isOnTheAttendanceListWithStatus(String playerName, String eventName, String status) {
        readTimeline().then()
                .statusCode(200)
                .body(eventPath(eventName) + ".attendance.find { it.playerId == '"
                        + world.id(Kind.PLAYER, playerName) + "' }.status", equalTo(status));
    }

    @Then("{string} is not on {string}'s attendance list")
    public void isNotOnTheAttendanceList(String playerName, String eventName) {
        readTimeline().then()
                .statusCode(200)
                .body(eventPath(eventName) + ".attendance.playerId",
                        not(hasItem(world.id(Kind.PLAYER, playerName).toString())));
    }

    @Then("{string} does not appear among {string}'s attending Lines")
    public void doesNotAppearAmongAttendingLines(String lineName, String eventName) {
        readTimeline().then()
                .statusCode(200)
                .body(eventPath(eventName) + ".lines.id",
                        not(hasItem(world.id(Kind.LINE, lineName).toString())));
    }

    @Then("{string} still appears among {string}'s attending Lines")
    public void stillAppearsAmongAttendingLines(String lineName, String eventName) {
        readTimeline().then()
                .statusCode(200)
                .body(eventPath(eventName) + ".lines.id",
                        hasItem(world.id(Kind.LINE, lineName).toString()));
    }

    @Then("{string} appears once, carrying both {string}'s and {string}'s line ids")
    public void appearsOnceCarryingBothLineIds(String playerName, String firstLine, String secondLine) {
        String playerId = world.id(Kind.PLAYER, playerName).toString();
        world.response().then()
                .statusCode(200)
                .body(eventPath() + ".attendance.findAll { it.playerId == '" + playerId + "' }", hasSize(1))
                .body(eventPath() + ".attendance.find { it.playerId == '" + playerId + "' }.lineIds",
                        hasItem(world.id(Kind.LINE, firstLine).toString()))
                .body(eventPath() + ".attendance.find { it.playerId == '" + playerId + "' }.lineIds",
                        hasItem(world.id(Kind.LINE, secondLine).toString()));
    }

    @Then("{string} is still on {string}'s attendance list through {string} but not through {string}")
    public void isStillOnTheListThroughOneLineOnly(
            String playerName, String eventName, String keptLine, String droppedLine) {
        String lineIds = eventPath(eventName) + ".attendance.find { it.playerId == '"
                + world.id(Kind.PLAYER, playerName) + "' }.lineIds";
        readTimeline().then()
                .statusCode(200)
                .body(lineIds, hasItem(world.id(Kind.LINE, keptLine).toString()))
                .body(lineIds, not(hasItem(world.id(Kind.LINE, droppedLine).toString())));
    }

    @Then("her status is {string} and no decline message is kept")
    public void herStatusIsAndNoMessageIsKept(String status) {
        world.response().then()
                .statusCode(200)
                .body(answerPath() + ".status", equalTo(status))
                .body(answerPath() + ".declineMessage", nullValue());
    }

    @Then("her status is {string} and the message {string} is stored")
    public void herStatusIsAndTheMessageIsStored(String status, String message) {
        world.response().then()
                .statusCode(200)
                .body(answerPath() + ".status", equalTo(status))
                .body(answerPath() + ".declineMessage", equalTo(message));
    }

    private String answerPath() {
        return "attendance.find { it.playerId == '" + world.id(Kind.PLAYER, subjectPlayer) + "' }";
    }

    /** The current Event, inside an Iteration read. */
    private String eventPath() {
        return "events.find { it.id == '" + world.current(Kind.EVENT) + "' }";
    }

    private String eventPath(String eventName) {
        return "events.find { it.id == '" + world.id(Kind.EVENT, eventName) + "' }";
    }
}
