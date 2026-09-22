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

/** Proves docs/spec/lines.feature. */
@ScenarioScope
public class LineSteps {

    /** Mirrors {@code LineResource.COLOR_PALETTE} — the fixed dial palette the spec names. */
    private static final List<String> DIAL_PALETTE = List.of(
            "#4c8c3d", "#8b5e34", "#6a4c93", "#c9702c", "#3b6ea5", "#b1467a", "#8a7a2e", "#2c7a68");

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    /** What the scenario associated with the Line, by spoken name. */
    private String associatedGoal;

    // --- Given ------------------------------------------------------------------

    @Given("the single seeded Team")
    public void theSingleSeededTeam() {
        assertTrue(fixtures.testData().teamExists(), "the single Team should be seeded");
    }

    @Given("^a Line \"([^\"]*)\"$")
    public void aLine(String name) {
        fixtures.line(name);
    }

    @Given("^a Line \"([^\"]*)\" rostering (.+)$")
    public void aLineRostering(String lineName, String players) {
        fixtures.line(lineName);
        for (String player : Names.in(players)) {
            fixtures.playerOn(lineName, player);
        }
    }

    @Given("a deleted Line {string}")
    public void aDeletedLine(String name) {
        fixtures.line(name);
        theCoachHasDeleted(name);
    }

    @Given("the coach has deleted {string}")
    public void theCoachHasDeleted(String name) {
        given().when().delete("/api/lines/" + world.id(Kind.LINE, name)).then().statusCode(204);
    }

    @Given("^the Team Players? (.+), on no Line$")
    public void theTeamPlayersOnNoLine(String players) {
        Names.in(players).forEach(fixtures::player);
    }

    @Given("{string} rates the Skill {string} at {int}")
    public void ratesTheSkillAt(String lineName, String skillName, int rating) {
        fixtures.testData().setRating(
                world.id(Kind.LINE, lineName), fixtures.skill(skillName, "#2c7a68"), rating);
        world.note("rating", rating);
    }

    @Given("{string} is associated with the Development goal {string}")
    public void isAssociatedWithTheDevelopmentGoal(String lineName, String goalName) {
        UUID goalId = fixtures.goal(goalName, "#c8722e");
        associatedGoal = goalName;
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("developmentGoalIds", List.of(goalId.toString())))
                .when().put("/api/lines/" + world.id(Kind.LINE, lineName))
                .then().statusCode(200);
    }

    @Given("a Line {string} not yet associated with the Skill {string}")
    public void aLineNotYetAssociatedWithTheSkill(String lineName, String skillName) {
        fixtures.line(lineName);
        fixtures.skill(skillName, "#5b5ea6");
    }

    @Given("the coach has rated it {int}")
    public void theCoachHasRatedIt(int rating) {
        rateCurrentSkill(rating).then().statusCode(200);
    }

    // --- When -------------------------------------------------------------------

    @When("the coach creates a Line named {string}")
    public void theCoachCreatesALineNamed(String name) {
        String actualName = world.uniquify(name);
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName))
                .when().post("/api/lines");
        world.setResponse(response);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.register(Kind.LINE, name, id, actualName);
            world.onCleanup(() -> fixtures.testData().deleteLine(id));
        }
    }

    @When("the coach creates a Line with a blank name")
    public void theCoachCreatesALineWithABlankName() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "  "))
                .when().post("/api/lines"));
    }

    @When("the Line list is read")
    public void theLineListIsRead() {
        world.setResponse(given().when().get("/api/lines"));
    }

    @When("the deleted-Line list is read")
    public void theDeletedLineListIsRead() {
        world.setResponse(given().when().get("/api/lines/deleted"));
    }

    @When("{string} is read by id")
    public void isReadById(String lineName) {
        world.setResponse(given().when().get("/api/lines/" + world.id(Kind.LINE, lineName)));
    }

    @When("a Line that does not exist is read")
    public void aLineThatDoesNotExistIsRead() {
        world.setResponse(given().when().get("/api/lines/" + UUID.randomUUID()));
    }

    @When("the coach deletes it")
    public void theCoachDeletesIt() {
        world.setResponse(given().when().delete("/api/lines/" + world.current(Kind.LINE)));
    }

    @When("the coach deletes it again")
    public void theCoachDeletesItAgain() {
        theCoachDeletesIt();
    }

    @When("the coach restores it")
    public void theCoachRestoresIt() {
        world.setResponse(given().when().post("/api/lines/" + world.current(Kind.LINE) + "/restore"));
    }

    @When("^the coach sets \"([^\"]*)\"'s roster to (.+)$")
    public void theCoachSetsTheRosterTo(String lineName, String players) {
        List<String> playerIds = Names.in(players).stream()
                .map(player -> world.id(Kind.PLAYER, player).toString())
                .toList();
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", playerIds))
                .when().put("/api/lines/" + world.id(Kind.LINE, lineName)));
    }

    @When("the coach sets its roster to a player id that does not exist")
    public void theCoachSetsItsRosterToAnUnknownPlayer() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("playerIds", List.of(UUID.randomUUID().toString())))
                .when().put("/api/lines/" + world.current(Kind.LINE)));
    }

    @When("the Team's Players are listed")
    public void theTeamsPlayersAreListed() {
        world.setResponse(given().when().get("/api/players"));
    }

    @When("the coach rates it {int}")
    public void theCoachRatesIt(int rating) {
        world.setResponse(rateCurrentSkill(rating));
    }

    @When("the coach removes that association")
    public void theCoachRemovesThatAssociation() {
        world.setResponse(given().when().delete(
                "/api/lines/" + world.current(Kind.LINE) + "/skills/" + world.current(Kind.SKILL)));
    }

    private Response rateCurrentSkill(int rating) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("rating", rating))
                .when().put("/api/lines/" + world.current(Kind.LINE) + "/skills/" + world.current(Kind.SKILL));
    }

    // --- Then -------------------------------------------------------------------

    @Then("it appears in the Line list with a player count of {int}")
    public void itAppearsInTheLineListWithAPlayerCountOf(int playerCount) {
        world.response().then().statusCode(201).body("playerCount", equalTo(playerCount));
        given().when().get("/api/lines").then()
                .statusCode(200)
                .body("find { it.id == '" + world.current(Kind.LINE) + "' }.name",
                        equalTo(world.currentName(Kind.LINE)));
    }

    @Then("it is assigned a color from the fixed dial palette, kept even once it is deleted")
    public void itIsAssignedAPaletteColorKeptOnceDeleted() {
        String color = world.response().path("color");
        assertTrue(DIAL_PALETTE.contains(color), color + " should be one of the fixed dial palette");

        given().when().delete("/api/lines/" + world.current(Kind.LINE)).then().statusCode(204);
        given().when().get("/api/lines/deleted").then()
                .statusCode(200)
                .body("find { it.id == '" + world.current(Kind.LINE) + "' }.color", equalTo(color));
    }

    @Then("{string} is listed with a player count of {int}")
    public void isListedWithAPlayerCountOf(String lineName, int playerCount) {
        world.response().then()
                .statusCode(200)
                .body("find { it.id == '" + world.id(Kind.LINE, lineName) + "' }.playerCount",
                        equalTo(playerCount));
    }

    @Then("{string} is listed there with its name and a player count of {int}")
    public void isListedThereWithItsNameAndAPlayerCountOf(String lineName, int playerCount) {
        String id = world.id(Kind.LINE, lineName).toString();
        world.response().then()
                .statusCode(200)
                .body("find { it.id == '" + id + "' }.name", equalTo(world.actualName(Kind.LINE, lineName)))
                .body("find { it.id == '" + id + "' }.playerCount", equalTo(playerCount));
    }

    @Then("its roster, its Skill ratings and its Development goals come back inline")
    public void itsAssociationsComeBackInline() {
        int rating = world.recall("rating");
        world.response().then()
                .statusCode(200)
                .body("id", equalTo(world.current(Kind.LINE).toString()))
                .body("players.name", hasItem(world.currentName(Kind.PLAYER)))
                .body("skills.find { it.skillId == '" + world.current(Kind.SKILL) + "' }.rating", equalTo(rating))
                .body("developmentGoals.id", hasItem(world.id(Kind.GOAL, associatedGoal).toString()));
    }

    @Then("it no longer appears in the Line list, and reading it by id is not found")
    public void itNoLongerAppearsInTheLineList() {
        world.response().then().statusCode(204);
        given().when().get("/api/lines").then()
                .statusCode(200)
                .body("id", not(hasItem(world.current(Kind.LINE).toString())));
        given().when().get("/api/lines/" + world.current(Kind.LINE)).then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Then("its row and its roster still exist, so Events it attended still show it")
    public void itsRowAndRosterStillExist() {
        assertTrue(fixtures.testData().lineRowExists(world.current(Kind.LINE)),
                "the soft-deleted Line's row should survive");
        assertEquals(1, fixtures.testData().lineRosterSize(world.current(Kind.LINE)),
                "the soft-deleted Line's roster should survive");
    }

    @Then("it appears in the Line list again with its roster intact, and no longer among the deleted")
    public void itAppearsInTheLineListAgain() {
        String id = world.current(Kind.LINE).toString();
        world.response().then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo(world.currentName(Kind.LINE)))
                .body("playerCount", equalTo(1));
        given().when().get("/api/lines").then().statusCode(200).body("id", hasItem(id));
        given().when().get("/api/lines/deleted").then().statusCode(200).body("id", not(hasItem(id)));
    }

    @Then("^its roster is (.+), and \"([^\"]*)\" has left it$")
    public void itsRosterIsAndHasLeftIt(String kept, String dropped) {
        var assertion = world.response().then().statusCode(200);
        for (String player : Names.in(kept)) {
            assertion.body("players.id", hasItem(world.id(Kind.PLAYER, player).toString()));
        }
        assertion.body("players.id", not(hasItem(world.id(Kind.PLAYER, dropped).toString())));
    }

    @Then("both {string} and {string} appear in its roster")
    public void bothAppearInItsRoster(String first, String second) {
        world.response().then()
                .statusCode(200)
                .body("players.id", hasItem(world.id(Kind.PLAYER, first).toString()))
                .body("players.id", hasItem(world.id(Kind.PLAYER, second).toString()));
    }

    @Then("{string} appears in both {string}'s and {string}'s rosters")
    public void appearsInBothRosters(String playerName, String firstLine, String secondLine) {
        world.response().then().statusCode(200);
        String playerId = world.id(Kind.PLAYER, playerName).toString();
        for (String lineName : List.of(firstLine, secondLine)) {
            given().when().get("/api/lines/" + world.id(Kind.LINE, lineName)).then()
                    .statusCode(200)
                    .body("players.id", hasItem(playerId));
        }
    }

    @Then("both {string} and {string} are returned")
    public void bothAreReturned(String first, String second) {
        world.response().then()
                .statusCode(200)
                .body("id", hasItem(world.id(Kind.PLAYER, first).toString()))
                .body("id", hasItem(world.id(Kind.PLAYER, second).toString()));
    }

    @Then("the Line-Skill association is created carrying {int}")
    public void theAssociationIsCreatedCarrying(int rating) {
        world.response().then()
                .statusCode(200)
                .body("rating", equalTo(rating))
                .body("skillId", equalTo(world.current(Kind.SKILL).toString()));
        given().when().get("/api/lines/" + world.current(Kind.LINE) + "/skills").then()
                .statusCode(200)
                .body("skillId", hasItem(world.current(Kind.SKILL).toString()));
    }

    @Then("the association carries {int} and no earlier value is kept")
    public void theAssociationCarriesAndNoHistoryIsKept(int rating) {
        world.response().then().statusCode(200).body("rating", equalTo(rating));
        given().when().get("/api/lines/" + world.current(Kind.LINE) + "/skills").then()
                .statusCode(200)
                .body("findAll { it.skillId == '" + world.current(Kind.SKILL) + "' }", hasSize(1))
                .body("find { it.skillId == '" + world.current(Kind.SKILL) + "' }.rating", equalTo(rating));
    }

    @Then("the Skill no longer appears on the Line, and the catalog Skill itself is untouched")
    public void theSkillNoLongerAppearsOnTheLine() {
        world.response().then().statusCode(204);
        given().when().get("/api/lines/" + world.current(Kind.LINE) + "/skills").then()
                .statusCode(200)
                .body("skillId", not(hasItem(world.current(Kind.SKILL).toString())));
        given().when().get("/api/skills").then()
                .statusCode(200)
                .body("id", hasItem(world.current(Kind.SKILL).toString()));
    }
}
