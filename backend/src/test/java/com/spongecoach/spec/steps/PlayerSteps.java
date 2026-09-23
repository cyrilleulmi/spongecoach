package com.spongecoach.spec.steps;

import com.spongecoach.spec.support.Fixtures;
import com.spongecoach.spec.support.Kind;
import com.spongecoach.spec.support.Pngs;
import com.spongecoach.spec.support.ScenarioWorld;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Proves docs/spec/players.feature. */
@ScenarioScope
public class PlayerSteps {

    private static final String SEED_COLOR = "#2c7a68";

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    // --- Given ------------------------------------------------------------------

    @Given("the Player skill {string}")
    public void thePlayerSkill(String name) {
        fixtures.playerSkill(name, SEED_COLOR);
    }

    @Given("the Player development goal {string}")
    public void thePlayerDevelopmentGoal(String name) {
        fixtures.playerGoal(name, SEED_COLOR);
    }

    @Given("{string} is rated {int} on the Player skill {string}")
    public void isRatedOnThePlayerSkill(String playerName, int rating, String skillName) {
        UUID skillId = fixtures.playerSkill(skillName, SEED_COLOR);
        fixtures.testData().setPlayerRating(world.id(Kind.PLAYER, playerName), skillId, rating);
        world.note("rating", rating);
    }

    @Given("{string} has the Player development goal {string}")
    public void hasThePlayerDevelopmentGoal(String playerName, String goalName) {
        UUID goalId = fixtures.playerGoal(goalName, SEED_COLOR);
        fixtures.testData().linkPlayerGoal(world.id(Kind.PLAYER, playerName), goalId);
    }

    @Given("{string} has a painted Avatar")
    public void hasAPaintedAvatar(String playerName) {
        fixtures.avatar(playerName);
    }

    // --- When -------------------------------------------------------------------

    @When("the seeded Player {string} is read by id")
    public void theSeededPlayerIsReadById(String playerName) {
        // Seed rows keep their real names; look the Player up by name rather than through Fixtures.
        String id = given().when().get("/api/players").then()
                .statusCode(200)
                .extract().path("find { it.name == '" + playerName + "' }.id");
        world.setResponse(given().when().get("/api/players/" + id));
    }

    @When("the coach saves a painted Avatar for {string}")
    public void theCoachSavesAPaintedAvatarFor(String playerName) {
        byte[] image = Pngs.painted(512, 512);
        world.note("newAvatar", image);
        world.setResponse(saveAvatar(world.id(Kind.PLAYER, playerName), image));
    }

    @When("the coach saves a painted Avatar for a Player that does not exist")
    public void theCoachSavesAPaintedAvatarForAnUnknownPlayer() {
        world.setResponse(saveAvatar(UUID.randomUUID(), Pngs.painted(512, 512)));
    }

    @When("^the coach saves (a text file|a \\d+x\\d+ PNG|a noisy PNG over 200 KB) as \"([^\"]*)\"'s Avatar$")
    public void theCoachSavesAsAvatar(String image, String playerName) {
        world.setResponse(saveAvatar(world.id(Kind.PLAYER, playerName), invalidAvatar(image)));
    }

    @When("{string}'s Avatar is read")
    public void avatarIsRead(String playerName) {
        world.setResponse(given().when().get(avatarPath(world.id(Kind.PLAYER, playerName))));
    }

    @When("the coach removes {string}'s Avatar")
    public void theCoachRemovesAvatar(String playerName) {
        world.setResponse(given().when().delete(avatarPath(world.id(Kind.PLAYER, playerName))));
    }

    @When("the Player {string} is read by id")
    public void thePlayerIsReadById(String playerName) {
        world.setResponse(given().when().get("/api/players/" + world.id(Kind.PLAYER, playerName)));
    }

    @When("a Player that does not exist is read")
    public void aPlayerThatDoesNotExistIsRead() {
        world.setResponse(given().when().get("/api/players/" + UUID.randomUUID()));
    }

    @When("the coach rates {string} {int} on that Player skill")
    public void theCoachRatesOnThatPlayerSkill(String playerName, int rating) {
        world.setResponse(rate(world.id(Kind.PLAYER, playerName), world.current(Kind.PLAYER_SKILL), rating));
    }

    @When("the coach rates {string} {int} on the Line Skill {string}")
    public void theCoachRatesOnTheLineSkill(String playerName, int rating, String skillName) {
        world.setResponse(rate(world.id(Kind.PLAYER, playerName), world.id(Kind.SKILL, skillName), rating));
    }

    @When("the coach removes that Player skill from {string}")
    public void theCoachRemovesThatPlayerSkillFrom(String playerName) {
        world.setResponse(given().when().delete(
                "/api/players/" + world.id(Kind.PLAYER, playerName) + "/skills/" + world.current(Kind.PLAYER_SKILL)));
    }

    @When("the coach creates the Player skill {string} with the color {string}")
    public void theCoachCreatesThePlayerSkill(String name, String color) {
        String actualName = world.uniquify(name);
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName, "color", color))
                .when().post("/api/player-skills");
        world.setResponse(response);
        world.note("color", color);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.register(Kind.PLAYER_SKILL, name, id, actualName);
            world.onCleanup(() -> fixtures.testData().deletePlayerSkill(id));
        }
    }

    @When("the coach creates the Player development goal {string} with the color {string}")
    public void theCoachCreatesThePlayerDevelopmentGoal(String name, String color) {
        String actualName = world.uniquify(name);
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName, "color", color))
                .when().post("/api/player-development-goals");
        world.setResponse(response);
        world.note("color", color);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.register(Kind.PLAYER_GOAL, name, id, actualName);
            world.onCleanup(() -> fixtures.testData().deletePlayerGoal(id));
        }
    }

    @When("the coach creates a Player skill with a blank name")
    public void theCoachCreatesAPlayerSkillWithABlankName() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "  ", "color", "#4f7a3f"))
                .when().post("/api/player-skills"));
    }

    @When("the coach sets {string}'s Player development goals to {string}")
    public void theCoachSetsThePlayerDevelopmentGoalsTo(String playerName, String goalName) {
        setGoals(playerName, List.of(world.id(Kind.PLAYER_GOAL, goalName).toString()));
    }

    @When("the coach sets {string}'s Player development goals to an id that does not exist")
    public void theCoachSetsThePlayerDevelopmentGoalsToAnUnknownId(String playerName) {
        setGoals(playerName, List.of(UUID.randomUUID().toString()));
    }

    private void setGoals(String playerName, List<String> goalIds) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("developmentGoalIds", goalIds))
                .when().put("/api/players/" + world.id(Kind.PLAYER, playerName)));
    }

    private io.restassured.response.Response saveAvatar(UUID playerId, byte[] image) {
        return given().contentType("image/png").body(image).when().put(avatarPath(playerId));
    }

    private static byte[] invalidAvatar(String description) {
        if (description.equals("a text file")) {
            return "not a picture".getBytes(StandardCharsets.UTF_8);
        }
        if (description.startsWith("a noisy PNG")) {
            return Pngs.noise(512);
        }
        String[] size = description.substring(2, description.indexOf(' ', 2)).split("x");
        return Pngs.painted(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
    }

    private static String avatarPath(UUID playerId) {
        return "/api/players/" + playerId + "/avatar";
    }

    private io.restassured.response.Response rate(UUID playerId, UUID skillId, int rating) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("rating", rating))
                .when().put("/api/players/" + playerId + "/skills/" + skillId);
    }

    // --- Then -------------------------------------------------------------------

    @Then("{string} is listed with the Line {string}, and {string} with no Line")
    public void isListedWithTheLineAndWithNoLine(String rostered, String lineName, String unrostered) {
        world.response().then()
                .statusCode(200)
                .body(linesOf(rostered) + ".id", hasItem(world.id(Kind.LINE, lineName).toString()))
                .body(linesOf(rostered) + ".name", hasItem(world.actualName(Kind.LINE, lineName)))
                .body(linesOf(unrostered), empty());
    }

    @Then("{string} is listed with no Line")
    public void isListedWithNoLine(String playerName) {
        world.response().then().statusCode(200).body(linesOf(playerName), empty());
    }

    @Then("their Lines, Player skill Ratings and Player development goals come back inline")
    public void theirProfileComesBackInline() {
        int rating = world.recall("rating");
        world.response().then()
                .statusCode(200)
                .body("id", equalTo(world.current(Kind.PLAYER).toString()))
                .body("name", equalTo(world.currentName(Kind.PLAYER)))
                .body("lines.id", hasItem(world.current(Kind.LINE).toString()))
                .body("skills.find { it.skillId == '" + world.current(Kind.PLAYER_SKILL) + "' }.rating",
                        equalTo(rating))
                .body("developmentGoals.id", hasItem(world.current(Kind.PLAYER_GOAL).toString()));
    }

    @Then("{string} carries {int} on that Player skill")
    public void carriesOnThatPlayerSkill(String playerName, int rating) {
        String skillId = world.current(Kind.PLAYER_SKILL).toString();
        world.response().then()
                .statusCode(200)
                .body("skillId", equalTo(skillId))
                .body("rating", equalTo(rating));
        given().when().get("/api/players/" + world.id(Kind.PLAYER, playerName) + "/skills").then()
                .statusCode(200)
                .body("find { it.skillId == '" + skillId + "' }.rating", equalTo(rating));
    }

    @Then("{string} carries {int} on that Player skill, and no earlier value is kept")
    public void carriesOnThatPlayerSkillWithoutHistory(String playerName, int rating) {
        carriesOnThatPlayerSkill(playerName, rating);
        given().when().get("/api/players/" + world.id(Kind.PLAYER, playerName) + "/skills").then()
                .statusCode(200)
                .body("findAll { it.skillId == '" + world.current(Kind.PLAYER_SKILL) + "' }", hasSize(1));
    }

    @Then("{string} no longer carries it, and the Player skill itself is still listed")
    public void noLongerCarriesIt(String playerName) {
        String skillId = world.current(Kind.PLAYER_SKILL).toString();
        world.response().then().statusCode(204);
        given().when().get("/api/players/" + world.id(Kind.PLAYER, playerName) + "/skills").then()
                .statusCode(200)
                .body("skillId", not(hasItem(skillId)));
        given().when().get("/api/player-skills").then()
                .statusCode(200)
                .body("id", hasItem(skillId));
    }

    @Then("they carry no Player skill Ratings")
    public void theyCarryNoPlayerSkillRatings() {
        world.response().then().statusCode(200).body("skills", empty());
    }

    @Then("it is listed among the Player skills with that color, and not among the Line Skills")
    public void itIsListedAmongThePlayerSkills() {
        String id = world.current(Kind.PLAYER_SKILL).toString();
        String color = world.recall("color");
        world.response().then().statusCode(201).body("color", equalTo(color));
        given().when().get("/api/player-skills").then()
                .statusCode(200)
                .body("find { it.id == '" + id + "' }.name", equalTo(world.currentName(Kind.PLAYER_SKILL)))
                .body("find { it.id == '" + id + "' }.color", equalTo(color));
        given().when().get("/api/skills").then().statusCode(200).body("id", not(hasItem(id)));
    }

    @Then("it is listed among the Player development goals with that color, and not among the Line Development goals")
    public void itIsListedAmongThePlayerDevelopmentGoals() {
        String id = world.current(Kind.PLAYER_GOAL).toString();
        String color = world.recall("color");
        world.response().then().statusCode(201).body("color", equalTo(color));
        given().when().get("/api/player-development-goals").then()
                .statusCode(200)
                .body("find { it.id == '" + id + "' }.color", equalTo(color));
        given().when().get("/api/development-goals").then().statusCode(200).body("id", not(hasItem(id)));
    }

    @Then("{string} has {string} but no longer {string}")
    public void hasButNoLonger(String playerName, String kept, String dropped) {
        world.response().then()
                .statusCode(200)
                .body("id", equalTo(world.id(Kind.PLAYER, playerName).toString()))
                .body("developmentGoals.id", hasItem(world.id(Kind.PLAYER_GOAL, kept).toString()))
                .body("developmentGoals.id", not(hasItem(world.id(Kind.PLAYER_GOAL, dropped).toString())));
    }

    @Then("{string}'s Avatar is that image, and the Player list and detail carry its version")
    public void avatarIsThatImage(String playerName) {
        UUID playerId = world.id(Kind.PLAYER, playerName);
        Long version = world.response().then()
                .statusCode(200)
                .body("avatarVersion", notNullValue())
                .extract().jsonPath().getLong("avatarVersion");
        assertAvatarImage(playerId, version, world.recall("newAvatar"));
        given().when().get("/api/players").then()
                .statusCode(200)
                .body("find { it.id == '" + playerId + "' }.avatarVersion", equalTo(version));
        given().when().get("/api/players/" + playerId).then()
                .statusCode(200)
                .body("avatarVersion", equalTo(version));
    }

    @Then("{string}'s Avatar is the new image, under a new version")
    public void avatarIsTheNewImage(String playerName) {
        Long previous = world.recall("avatarVersion");
        Long version = world.response().then()
                .statusCode(200)
                .extract().jsonPath().getLong("avatarVersion");
        assertThat(version, not(equalTo(previous)));
        assertAvatarImage(world.id(Kind.PLAYER, playerName), version, world.recall("newAvatar"));
    }

    @Then("{string} carries no Avatar version, and their Avatar is not found")
    public void carriesNoAvatar(String playerName) {
        UUID playerId = world.id(Kind.PLAYER, playerName);
        world.response().then().statusCode(204);
        given().when().get("/api/players/" + playerId).then()
                .statusCode(200)
                .body("avatarVersion", nullValue());
        given().when().get(avatarPath(playerId)).then().statusCode(404);
    }

    @Then("they carry an Avatar version, and their Avatar is a 512 px PNG")
    public void theyCarryASeededAvatar() {
        String id = world.response().then().statusCode(200).extract().path("id");
        long version = world.response().then()
                .body("avatarVersion", notNullValue())
                .extract().jsonPath().getLong("avatarVersion");
        byte[] png = given().queryParam("v", version).when().get(avatarPath(UUID.fromString(id))).then()
                .statusCode(200)
                .contentType("image/png")
                .extract().asByteArray();
        // IHDR width, big-endian at byte 16.
        assertThat(ByteBuffer.wrap(png, 16, 4).getInt(), equalTo(512));
    }

    @Then("{string} is on the roster with their Avatar version")
    public void isOnTheRosterWithTheirAvatarVersion(String playerName) {
        Long version = world.recall("avatarVersion");
        world.response().then()
                .statusCode(200)
                .body("players.find { it.id == '" + world.id(Kind.PLAYER, playerName) + "' }.avatarVersion",
                        equalTo(version));
    }

    private void assertAvatarImage(UUID playerId, Long version, byte[] expected) {
        byte[] served = given().queryParam("v", version).when().get(avatarPath(playerId)).then()
                .statusCode(200)
                .contentType("image/png")
                .header("Cache-Control", containsString("immutable"))
                .extract().asByteArray();
        assertArrayEquals(expected, served);
    }

    private String linesOf(String playerName) {
        return "find { it.id == '" + world.id(Kind.PLAYER, playerName) + "' }.lines";
    }
}
