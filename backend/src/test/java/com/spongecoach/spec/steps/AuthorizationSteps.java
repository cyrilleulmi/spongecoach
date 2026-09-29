package com.spongecoach.spec.steps;

import com.spongecoach.domain.Role;
import com.spongecoach.spec.support.DrillClient;
import com.spongecoach.spec.support.Fixtures;
import com.spongecoach.spec.support.Kind;
import com.spongecoach.spec.support.ScenarioWorld;
import com.spongecoach.spec.support.Session;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Proves docs/spec/authorization.feature. "They" is whichever User the last {@code Given ... is
 * using the app} chose; every request goes out as that User through {@link Session}.
 */
@ScenarioScope
public class AuthorizationSteps {

    private static final String SEED_COLOR = "#3b6ea5";

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    @Inject
    Session session;

    @Inject
    DrillClient drills;

    // --- Given ------------------------------------------------------------------

    @Given("{string} is using the app as a Player")
    public void isUsingTheAppAsAPlayer(String playerName) {
        session.actAs(fixtures.user(playerName, Role.PLAYER, playerName));
    }

    @Given("a Coach {string} is using the app")
    public void aCoachIsUsingTheApp(String name) {
        session.actAs(fixtures.user(name, Role.COACH, null));
    }

    @Given("the SysAdmin is using the app")
    public void theSysAdminIsUsingTheApp() {
        session.actAs(Session.SEEDED_SYS_ADMIN);
    }

    // --- When: who is asking ----------------------------------------------------

    @When("the Line list is read with no User chosen")
    public void theLineListIsReadWithNoUserChosen() {
        session.actAsNobody();
        world.setResponse(given().when().get("/api/lines"));
    }

    @When("the Line list is read as a User that does not exist")
    public void theLineListIsReadAsAnUnknownUser() {
        session.actAs(UUID.randomUUID());
        world.setResponse(given().when().get("/api/lines"));
    }

    @When("the Users are listed with no User chosen")
    public void theUsersAreListedWithNoUserChosen() {
        session.actAsNobody();
        world.setResponse(given().when().get("/api/users"));
    }

    @When("the current User is read")
    public void theCurrentUserIsRead() {
        world.setResponse(given().when().get("/api/me"));
    }

    // --- When: what they do -----------------------------------------------------

    @When("they read the Line {string}")
    public void theyReadTheLine(String lineName) {
        world.setResponse(given().when().get("/api/lines/" + world.id(Kind.LINE, lineName)));
    }

    @When("they create a Line {string}")
    public void theyCreateALine(String name) {
        Response response = post("/api/lines", Map.of("name", world.uniquify(name)));
        world.setResponse(response);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.onCleanup(() -> fixtures.testData().deleteLine(id));
        }
    }

    @When("they rename the Line {string} to {string}")
    public void theyRenameTheLine(String lineName, String newName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", world.uniquify(newName)))
                .when().put("/api/lines/" + world.id(Kind.LINE, lineName)));
    }

    @When("they delete the Line {string}")
    public void theyDeleteTheLine(String lineName) {
        world.setResponse(given().when().delete("/api/lines/" + world.id(Kind.LINE, lineName)));
    }

    @When("they rate {string} {int} on the Skill {string}")
    public void theyRateTheLineOnTheSkill(String lineName, int rating, String skillName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("rating", rating))
                .when().put("/api/lines/" + world.id(Kind.LINE, lineName)
                        + "/skills/" + world.id(Kind.SKILL, skillName)));
    }

    @When("they create the Skill {string}")
    public void theyCreateTheSkill(String name) {
        Response response = post("/api/skills", Map.of("name", world.uniquify(name), "color", SEED_COLOR));
        world.setResponse(response);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.onCleanup(() -> fixtures.testData().deleteSkill(id));
        }
    }

    @When("they rate {string} {int} on the Player skill {string}")
    public void theyRateThePlayerOnThePlayerSkill(String playerName, int rating, String skillName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("rating", rating))
                .when().put("/api/players/" + world.id(Kind.PLAYER, playerName)
                        + "/skills/" + world.id(Kind.PLAYER_SKILL, skillName)));
    }

    @When("they set {string} to {string} on {string}")
    public void theySetTheAnswer(String playerName, String status, String eventName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", status))
                .when().put("/api/events/" + world.id(Kind.EVENT, eventName)
                        + "/attendance/" + world.id(Kind.PLAYER, playerName)));
    }

    @When("they set {string}'s Focus on {string} to {string}")
    public void theySetTheLinesFocus(String lineName, String eventName, String focus) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("focus", focus))
                .when().put("/api/events/" + world.id(Kind.EVENT, eventName)
                        + "/focus/" + world.id(Kind.LINE, lineName)));
    }

    @When("they replace {string}'s Focus set with {string} for {string}")
    public void theyReplaceTheFocusSet(String eventName, String focus, String lineName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(Map.of(
                        "lineId", world.id(Kind.LINE, lineName).toString(), "focus", focus))))
                .when().put("/api/events/" + world.id(Kind.EVENT, eventName)));
    }

    @When("they create an Iteration {string}")
    public void theyCreateAnIteration(String name) {
        Response response = post("/api/iterations", Map.of("name", world.uniquify(name)));
        world.setResponse(response);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.onCleanup(() -> fixtures.testData().deleteIteration(id));
        }
    }

    @When("they read the Drill {string}")
    public void theyReadTheDrill(String drill) {
        world.setResponse(drills.read(drill));
    }

    @When("they upload another drill like the Drill {string}")
    public void theyUploadAnotherDrill(String drill) {
        world.setResponse(drills.upload(drill + " 2", 1));
    }

    @When("they rename the Drill {string}")
    public void theyRenameTheDrill(String drill) {
        world.setResponse(drills.rename(drill, world.uniquify(drill), null));
    }

    @When("they answer the questions of the Drill {string}")
    public void theyAnswerTheQuestionsOfTheDrill(String drill) {
        world.setResponse(drills.answer(drill, "q1", "Verteidigerinnen"));
    }

    @When("they send a correction for the Drill {string}")
    public void theySendACorrectionForTheDrill(String drill) {
        world.setResponse(drills.chat(drill, "Mehr Tempo"));
    }

    @When("they retry the Drill {string}")
    public void theyRetryTheDrill(String drill) {
        world.setResponse(drills.retry(drill));
    }

    @When("they save a hand-edited script for the Drill {string}")
    public void theySaveAHandEditedScriptForTheDrill(String drill) {
        world.setResponse(drills.saveHandEdit(drill));
    }

    @When("they revert the Drill {string}")
    public void theyRevertTheDrill(String drill) {
        world.setResponse(drills.revert(drill, 1));
    }

    @When("they delete the Drill {string}")
    public void theyDeleteTheDrill(String drill) {
        world.setResponse(drills.delete(drill));
    }

    // --- Then -------------------------------------------------------------------

    @Then("the seeded SysAdmin {string} and the seeded Coaches {string}, {string}, {string} and {string} are among them")
    public void theSeededUsersAreAmongThem(
            String admin, String coach1, String coach2, String coach3, String coach4) {
        var assertion = world.response().then().statusCode(200)
                .body("find { it.name == '" + admin + "' }.role", equalTo("SYS_ADMIN"));
        for (String coach : List.of(coach1, coach2, coach3, coach4)) {
            assertion.body("find { it.name == '" + coach + "' }.role", equalTo("COACH"));
        }
        // Anita coaches and plays: her one User acts as her seeded Player.
        assertion.body("find { it.name == '" + coach4 + "' }.playerId", equalTo("20000000-0000-0000-0000-000000000009"));
    }

    @Then("it is {string} as a Player, on the Line {string}")
    public void itIsAsAPlayerOnTheLine(String playerName, String lineName) {
        world.response().then()
                .statusCode(200)
                .body("name", equalTo(world.actualName(Kind.USER, playerName)))
                .body("role", equalTo("PLAYER"))
                .body("playerId", equalTo(world.id(Kind.PLAYER, playerName).toString()))
                .body("lineIds", contains(world.id(Kind.LINE, lineName).toString()));
    }

    private Response post(String path, Map<String, ?> body) {
        return given().contentType(ContentType.JSON).body(body).when().post(path);
    }
}
