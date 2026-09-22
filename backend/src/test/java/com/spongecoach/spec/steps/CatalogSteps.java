package com.spongecoach.spec.steps;

import com.spongecoach.spec.support.Fixtures;
import com.spongecoach.spec.support.Kind;
import com.spongecoach.spec.support.ScenarioWorld;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/** Proves docs/spec/catalogs.feature. */
@ScenarioScope
public class CatalogSteps {

    private static final String SEED_COLOR = "#111111";

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    /** The goals a {@code Given} has named, in the order the scenario named them. */
    private final List<String> namedGoals = new ArrayList<>();

    // --- Given ------------------------------------------------------------------

    @Given("the Skill {string}")
    public void theSkill(String name) {
        fixtures.skill(name, SEED_COLOR);
    }

    @Given("the soft-deleted Skill {string}")
    public void theSoftDeletedSkill(String name) {
        UUID skillId = fixtures.skill(name, SEED_COLOR);
        fixtures.testData().softDeleteSkill(skillId);
    }

    @Given("the Skill {string}, rated {int} by the Line {string}")
    public void theSkillRatedByTheLine(String skillName, int rating, String lineName) {
        UUID skillId = fixtures.skill(skillName, SEED_COLOR);
        UUID lineId = fixtures.line(lineName);
        fixtures.testData().setRating(lineId, skillId, rating);
    }

    @Given("the Development goal {string}")
    public void theDevelopmentGoal(String name) {
        fixtures.goal(name, SEED_COLOR);
        namedGoals.add(name);
    }

    @Given("the soft-deleted Development goal {string}")
    public void theSoftDeletedDevelopmentGoal(String name) {
        UUID goalId = fixtures.goal(name, SEED_COLOR);
        fixtures.testData().softDeleteGoal(goalId);
    }

    @Given("the Focus {string} derived from it")
    public void theFocusDerivedFromIt(String focusName) {
        fixtures.focus(focusName, List.of(world.current(Kind.GOAL)));
    }

    // --- When -------------------------------------------------------------------

    @When("the Skill catalog is listed")
    public void theSkillCatalogIsListed() {
        world.setResponse(given().when().get("/api/skills"));
    }

    @When("the Development goal catalog is listed")
    public void theDevelopmentGoalCatalogIsListed() {
        world.setResponse(given().when().get("/api/development-goals"));
    }

    @When("the Focus catalog is listed")
    public void theFocusCatalogIsListed() {
        world.setResponse(given().when().get("/api/focuses"));
    }

    @When("the coach creates the Skill {string} with the color {string}")
    public void theCoachCreatesTheSkill(String name, String color) {
        String actualName = world.uniquify(name);
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName, "color", color))
                .when().post("/api/skills");
        world.setResponse(response);
        world.note("color", color);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.register(Kind.SKILL, name, id, actualName);
            world.onCleanup(() -> fixtures.testData().deleteSkill(id));
        }
    }

    @When("the coach creates the Development goal {string} with the color {string}")
    public void theCoachCreatesTheDevelopmentGoal(String name, String color) {
        String actualName = world.uniquify(name);
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName, "color", color))
                .when().post("/api/development-goals");
        world.setResponse(response);
        world.note("color", color);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.register(Kind.GOAL, name, id, actualName);
            world.onCleanup(() -> fixtures.testData().deleteGoal(id));
        }
    }

    @When("the coach creates a Skill with a blank name")
    public void theCoachCreatesASkillWithABlankName() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "  ", "color", "#4f7a3f"))
                .when().post("/api/skills"));
    }

    @When("the coach changes that Skill's color to {string}")
    public void theCoachChangesThatSkillsColor(String color) {
        world.note("color", color);
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("color", color))
                .when().put("/api/skills/" + world.current(Kind.SKILL)));
    }

    @When("the coach changes that Development goal's color to {string}")
    public void theCoachChangesThatGoalsColor(String color) {
        world.note("color", color);
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("color", color))
                .when().put("/api/development-goals/" + world.current(Kind.GOAL)));
    }

    @When("the coach creates the Focus {string} from it")
    public void theCoachCreatesTheFocusFromIt(String name) {
        createFocus(name, List.of(world.current(Kind.GOAL)));
    }

    @When("the coach creates the Focus {string} from both")
    public void theCoachCreatesTheFocusFromBoth(String name) {
        createFocus(name, namedGoals.stream().map(goal -> world.id(Kind.GOAL, goal)).toList());
    }

    @When("the coach creates a Focus with an empty goal list")
    public void theCoachCreatesAFocusWithAnEmptyGoalList() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", world.uniquify("Orphan focus"), "goalIds", List.of()))
                .when().post("/api/focuses"));
    }

    private void createFocus(String name, List<UUID> goalIds) {
        String actualName = world.uniquify(name);
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName, "goalIds", goalIds.stream().map(UUID::toString).toList()))
                .when().post("/api/focuses");
        world.setResponse(response);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.register(Kind.FOCUS, name, id, actualName);
            world.onCleanup(() -> fixtures.testData().deleteFocus(id));
        }
    }

    // --- Then -------------------------------------------------------------------

    @Then("{string} is in the Skill catalog but {string} is not")
    public void isInTheSkillCatalogButIsNot(String listed, String hidden) {
        world.response().then()
                .statusCode(200)
                .body("id", hasItem(world.id(Kind.SKILL, listed).toString()))
                .body("id", not(hasItem(world.id(Kind.SKILL, hidden).toString())));
    }

    @Then("{string} is in the Development goal catalog but {string} is not")
    public void isInTheGoalCatalogButIsNot(String listed, String hidden) {
        world.response().then()
                .statusCode(200)
                .body("id", hasItem(world.id(Kind.GOAL, listed).toString()))
                .body("id", not(hasItem(world.id(Kind.GOAL, hidden).toString())));
    }

    @Then("it is listed in the Skill catalog with that color")
    public void itIsListedInTheSkillCatalogWithThatColor() {
        String id = world.current(Kind.SKILL).toString();
        String color = world.recall("color");
        world.response().then().statusCode(201).body("color", equalTo(color));
        given().when().get("/api/skills").then()
                .statusCode(200)
                .body("find { it.id == '" + id + "' }.name", equalTo(world.currentName(Kind.SKILL)))
                .body("find { it.id == '" + id + "' }.color", equalTo(color));
    }

    @Then("it is listed in the Development goal catalog with that color")
    public void itIsListedInTheGoalCatalogWithThatColor() {
        String id = world.current(Kind.GOAL).toString();
        String color = world.recall("color");
        world.response().then().statusCode(201).body("color", equalTo(color));
        given().when().get("/api/development-goals").then()
                .statusCode(200)
                .body("find { it.id == '" + id + "' }.color", equalTo(color));
    }

    @Then("the new color comes back, and {string} sees it on that Skill")
    public void theNewColorComesBackAndTheLineSeesIt(String lineName) {
        String color = world.recall("color");
        world.response().then().statusCode(200).body("color", equalTo(color));
        given().when().get("/api/lines/" + world.id(Kind.LINE, lineName)).then()
                .statusCode(200)
                .body("skills.find { it.skillId == '" + world.current(Kind.SKILL) + "' }.color", equalTo(color));
    }

    @Then("the new color comes back on the Development goal")
    public void theNewColorComesBackOnTheGoal() {
        String color = world.recall("color");
        world.response().then().statusCode(200).body("color", equalTo(color));
    }

    @Then("it is listed carrying that goal's id")
    public void itIsListedCarryingThatGoalsId() {
        String focusId = world.current(Kind.FOCUS).toString();
        String goalId = world.current(Kind.GOAL).toString();
        world.response().then().statusCode(201).body("goalIds", hasItem(goalId));
        given().when().get("/api/focuses").then()
                .statusCode(200)
                .body("find { it.id == '" + focusId + "' }.goalIds", hasItem(goalId));
    }

    @Then("both goal ids are linked to it")
    public void bothGoalIdsAreLinkedToIt() {
        String focusId = world.current(Kind.FOCUS).toString();
        world.response().then().statusCode(201);
        for (String goal : namedGoals) {
            world.response().then().body("goalIds", hasItem(world.id(Kind.GOAL, goal).toString()));
        }
        given().when().get("/api/focuses").then()
                .statusCode(200)
                .body("find { it.id == '" + focusId + "' }.goalIds.size()", equalTo(namedGoals.size()));
    }

    @Then("{string} carries the id of the goal it came from")
    public void carriesTheIdOfTheGoalItCameFrom(String focusName) {
        world.response().then()
                .statusCode(200)
                .body("find { it.id == '" + world.id(Kind.FOCUS, focusName) + "' }.goalIds",
                        hasItem(world.current(Kind.GOAL).toString()));
    }
}
