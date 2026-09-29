package com.spongecoach.spec.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.spongecoach.domain.DrillStatus;
import com.spongecoach.drill.DrillJson;
import com.spongecoach.drill.StubDrillInterpreter;
import com.spongecoach.spec.support.DrillClient;
import com.spongecoach.spec.support.Fixtures;
import com.spongecoach.spec.support.Kind;
import com.spongecoach.spec.support.Pngs;
import com.spongecoach.spec.support.ScenarioWorld;
import com.spongecoach.support.Jpegs;
import com.spongecoach.support.TestData;
import com.spongecoach.support.TestScripts;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.response.Response;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves docs/spec/drills.feature. "The coach" is the seeded SysAdmin every scenario starts as. */
@ScenarioScope
public class DrillSteps {

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    @Inject
    DrillClient drills;

    @Inject
    StubDrillInterpreter interpreter;

    // --- Given: the interpreter -------------------------------------------------

    @Given("the interpreter asks {string} about the Drill {string}")
    public void theInterpreterAsks(String question, String drill) {
        interpreter.askFirst(fixtures.drillName(drill), question);
    }

    @Given("the interpreter's first script for the Drill {string} is unplayable")
    public void theFirstScriptIsUnplayable(String drill) {
        interpreter.unplayable(fixtures.drillName(drill), 1);
    }

    @Given("the interpreter's scripts for the Drill {string} are always unplayable")
    public void theScriptsAreAlwaysUnplayable(String drill) {
        interpreter.unplayable(fixtures.drillName(drill), Integer.MAX_VALUE);
    }

    @Given("the interpreter refuses the Drill {string}")
    public void theInterpreterRefuses(String drill) {
        interpreter.refuse(fixtures.drillName(drill));
    }

    // --- Given: Drills ----------------------------------------------------------

    @Given("a Drill {string} waiting for answers")
    public void aDrillWaitingForAnswers(String drill) {
        fixtures.drill(drill, DrillStatus.NEEDS_INPUT);
    }

    @Given("a ready Drill {string}")
    public void aReadyDrill(String drill) {
        fixtures.drill(drill, DrillStatus.READY);
    }

    @Given("a failed Drill {string}")
    public void aFailedDrill(String drill) {
        fixtures.drill(drill, DrillStatus.FAILED);
    }

    @Given("a Drill {string} being interpreted")
    public void aDrillBeingInterpreted(String drill) {
        fixtures.drill(drill, DrillStatus.PENDING);
    }

    @Given("the coach saved a hand-edited script for the Drill {string}")
    public void theCoachSavedAHandEditedScript(String drill) {
        drills.saveHandEdit(drill).then().statusCode(200);
    }

    // --- When: uploading --------------------------------------------------------

    @When("the coach uploads the Drill {string} with {int} sketch(es)")
    public void theCoachUploadsWithSketches(String drill, int count) {
        world.setResponse(drills.upload(drill, count));
    }

    @When("the coach uploads the Drill {string} with no sketches")
    public void theCoachUploadsWithNoSketches(String drill) {
        world.setResponse(drills.upload(drill, 0));
    }

    @When("the coach uploads the Drill {string} with a PNG sketch")
    public void theCoachUploadsWithAPngSketch(String drill) {
        world.setResponse(drills.upload(drill, List.of(Pngs.painted(32, 32)), List.of(), List.of(), null));
    }

    @When("the coach uploads the Drill {string} with sketches noted {string} and {string}")
    public void theCoachUploadsWithNotes(String drill, String firstNote, String secondNote) {
        world.setResponse(drills.upload(drill, List.of(Jpegs.sketch(), Jpegs.sketch()),
                List.of(firstNote, secondNote), List.of(), null));
    }

    @When("the coach uploads the Drill {string} tagged {string} and {string} as a progression")
    public void theCoachUploadsTaggedAsAProgression(String drill, String firstTag, String secondTag) {
        TestData testData = fixtures.testData();
        world.setResponse(drills.upload(drill, List.of(Jpegs.sketch(), Jpegs.sketch()), List.of(),
                List.of(testData.drillTagId(firstTag), testData.drillTagId(secondTag)), "PROGRESSION"));
    }

    @When("the coach uploads a Drill with a blank name")
    public void theCoachUploadsWithABlankName() {
        world.setResponse(given()
                .multiPart("name", " ")
                .multiPart("sketches", "sketch-1.jpg", Jpegs.sketch(), "image/jpeg")
                .when().post("/api/drills"));
    }

    @When("the coach uploads the Drill {string} tagged with a tag that does not exist")
    public void theCoachUploadsWithAnUnknownTag(String drill) {
        world.setResponse(drills.upload(drill, List.of(Jpegs.sketch()), List.of(), List.of(UUID.randomUUID()), null));
    }

    @When("the Drill tags are listed")
    public void theDrillTagsAreListed() {
        world.setResponse(given().when().get("/api/drill-tags"));
    }

    // --- When: answering and correcting -----------------------------------------

    @When("the coach answers {string} on the Drill {string}")
    public void theCoachAnswers(String answer, String drill) {
        world.setResponse(drills.answer(drill, "q1", answer));
    }

    @When("the coach tells the interpreter to guess on the Drill {string}")
    public void theCoachTellsTheInterpreterToGuess(String drill) {
        world.setResponse(drills.guess(drill));
    }

    @When("the coach answers a question the Drill {string} did not ask")
    public void theCoachAnswersAnUnaskedQuestion(String drill) {
        world.setResponse(drills.answer(drill, "q9", "Hütchen"));
    }

    @When("the coach retries the Drill {string}")
    public void theCoachRetries(String drill) {
        world.setResponse(drills.retry(drill));
    }

    @When("the coach sends the correction {string} for the Drill {string}")
    public void theCoachSendsTheCorrection(String message, String drill) {
        world.setResponse(drills.chat(drill, message));
    }

    @When("the coach saves a hand-edited script for the Drill {string}")
    public void theCoachSavesAHandEditedScript(String drill) {
        world.setResponse(drills.saveHandEdit(drill));
    }

    @When("^the coach saves a hand-edited script for the Drill \"([^\"]*)\" with (.+)$")
    public void theCoachSavesABrokenScript(String drill, String problem) {
        world.setResponse(drills.saveScript(drill, TestScripts.broken(problem)));
    }

    @When("the coach reverts the Drill {string} to version {int}")
    public void theCoachReverts(String drill, int version) {
        world.setResponse(drills.revert(drill, version));
    }

    // --- When: the list ---------------------------------------------------------

    @When("the Drills are listed")
    public void theDrillsAreListed() {
        world.setResponse(given().when().get("/api/drills"));
    }

    @When("the coach renames the Drill {string} to {string} and tags it {string}")
    public void theCoachRenamesAndRetags(String drill, String newName, String tag) {
        world.setResponse(drills.rename(drill, newName, List.of(fixtures.testData().drillTagId(tag))));
    }

    @When("the coach deletes the Drill {string}")
    public void theCoachDeletes(String drill) {
        world.setResponse(drills.delete(drill));
    }

    // --- Then -------------------------------------------------------------------

    @Then("the upload is accepted while the Drill {string} is being interpreted")
    public void theUploadIsAccepted(String drill) {
        world.response().then().statusCode(202)
                .body("id", equalTo(drills.id(drill).toString()))
                .body("status", equalTo("PENDING"));
    }

    @Then("the Drill {string} becomes ready with script version {int}")
    public void theDrillBecomesReady(String drill, int version) {
        drills.awaitJob(drills.id(drill));
        drills.read(drill).then().statusCode(200)
                .body("status", equalTo("READY"))
                .body("error", equalTo(null))
                .body("currentVersion", equalTo(version))
                .body("script.stages", not(hasSize(0)));
    }

    @Then("the Drill {string} has sketch 1 noted {string} and sketch 2 noted {string}")
    public void theDrillHasSketchesNoted(String drill, String firstNote, String secondNote) {
        drills.read(drill).then().statusCode(200)
                .body("sketches.position", equalTo(List.of(1, 2)))
                .body("sketches.note", equalTo(List.of(firstNote, secondNote)));
    }

    @Then("each of its sketches is served as a JPEG")
    public void eachOfItsSketchesIsServedAsAJpeg() {
        UUID drillId = world.current(Kind.DRILL);
        for (int position : List.of(1, 2)) {
            Response response = given().when().get("/api/drills/" + drillId + "/sketches/" + position);
            response.then().statusCode(200).contentType("image/jpeg");
            byte[] image = response.asByteArray();
            assertTrue(image.length > 3 && (image[0] & 0xff) == 0xff && (image[1] & 0xff) == 0xd8,
                    "sketch " + position + " is not a JPEG");
        }
    }

    @Then("each of its sketches carries what the interpreter read on it")
    public void eachOfItsSketchesCarriesAReading() {
        UUID drillId = world.current(Kind.DRILL);
        given().when().get("/api/drills/" + drillId).then().statusCode(200)
                .body("sketches.findAll { it.reading == null }", hasSize(0))
                .body("sketches[0].reading.symbols", not(hasSize(0)))
                .body("sketches[1].reading.symbols", not(hasSize(0)));
    }

    @Then("the Drill {string} is tagged {string} and {string}, as a progression")
    public void theDrillIsTaggedAsAProgression(String drill, String firstTag, String secondTag) {
        drills.read(drill).then().statusCode(200)
                .body("tags.name", containsInAnyOrder(firstTag, secondTag))
                .body("sketchRelation", equalTo("PROGRESSION"));
    }

    @Then("they are the {int} seeded tags, from {string} to {string}")
    public void theyAreTheSeededTags(int count, String first, String last) {
        world.response().then().statusCode(200)
                .body("", hasSize(count))
                .body("[0].name", equalTo(first))
                .body("[" + (count - 1) + "].name", equalTo(last));
    }

    @Then("the Drill {string} waits for an answer to {string}")
    public void theDrillWaitsForAnAnswer(String drill, String question) {
        drills.awaitJob(drills.id(drill));
        drills.read(drill).then().statusCode(200)
                .body("status", equalTo("NEEDS_INPUT"))
                .body("openQuestions.text", hasItem(question));
    }

    @Then("the conversation of {string} ends with the answer {string} and the interpreter's reply")
    public void theConversationEndsWithTheAnswer(String drill, String answer) {
        List<JsonNode> messages = new ArrayList<>();
        DrillJson.tree(drills.read(drill).asString()).get("messages").forEach(messages::add);
        JsonNode coach = messages.get(messages.size() - 2);
        JsonNode reply = messages.get(messages.size() - 1);
        assertEquals("COACH", coach.get("author").asText());
        assertEquals(answer, coach.get("answers").get(0).get("answer").asText());
        assertEquals("INTERPRETER", reply.get("author").asText());
        assertEquals(2, reply.get("scriptVersion").asInt());
    }

    @Then("the Drill {string} fails, saying the script was invalid")
    public void theDrillFailsInvalid(String drill) {
        theDrillFails(drill, "ungültig");
    }

    @Then("the Drill {string} fails, saying the request was refused")
    public void theDrillFailsRefused(String drill) {
        theDrillFails(drill, "abgelehnt");
    }

    @Then("the request is refused because the Drill is busy")
    public void theRequestIsRefusedBecauseBusy() {
        world.response().then().statusCode(409).body("error", equalTo("drill_busy"));
    }

    @Then("version {int} of {string} is summarised as {string}")
    public void theVersionIsSummarisedAs(int version, String drill, String summary) {
        drills.read(drill).then().statusCode(200)
                .body("versions.find { it.version == " + version + " }.changeSummary", equalTo(summary));
    }

    @Then("version {int} of {string} is current, as a hand edit")
    public void theVersionIsCurrentAsAHandEdit(int version, String drill) {
        world.response().then().statusCode(200);
        drills.read(drill).then().statusCode(200)
                .body("currentVersion", equalTo(version))
                .body("versions.find { it.version == " + version + " }.source", equalTo("EDIT"))
                .body("script.stages[0].name", equalTo("Handarbeit"));
    }

    @Then("version {int} of {string} is current, as a revert with the script of version {int}")
    public void theVersionIsCurrentAsARevert(int version, String drill, int earlier) {
        world.response().then().statusCode(200);
        JsonNode detail = DrillJson.tree(drills.read(drill).asString());
        assertEquals(version, detail.get("currentVersion").asInt());
        JsonNode revert = null;
        for (JsonNode candidate : detail.get("versions")) {
            if (candidate.get("version").asInt() == version) {
                revert = candidate;
            }
        }
        assertEquals("REVERT", revert.get("source").asText());
        assertEquals("Zurück zu Version " + earlier, revert.get("changeSummary").asText());
        // The fixture's version 1 is the stub's playable script, before the hand edit renamed it.
        assertEquals(DrillJson.mapper().valueToTree(TestScripts.playable("Stufe 1")), detail.get("script"));
    }

    @Then("{string} comes before {string}, each with its sketch count")
    public void comesBefore(String first, String second) {
        List<String> ids = world.response().then().statusCode(200).extract().path("id");
        int firstIndex = ids.indexOf(drills.id(first).toString());
        int secondIndex = ids.indexOf(drills.id(second).toString());
        assertTrue(firstIndex >= 0 && secondIndex > firstIndex,
                "expected " + first + " before " + second + " in " + ids);
        world.response().then()
                .body("find { it.id == '" + drills.id(first) + "' }.sketchCount", equalTo(1))
                .body("find { it.id == '" + drills.id(second) + "' }.sketchCount", equalTo(1));
    }

    @Then("the Drill {string} is named {string} and tagged {string}")
    public void theDrillIsNamedAndTagged(String drill, String name, String tag) {
        world.response().then().statusCode(200);
        drills.read(drill).then().statusCode(200)
                .body("name", equalTo(name))
                .body("tags.name", equalTo(List.of(tag)));
    }

    @Then("the Drill {string} is not listed, and reading it is not found")
    public void theDrillIsNotListed(String drill) {
        world.response().then().statusCode(204);
        List<String> ids = given().when().get("/api/drills").then().statusCode(200).extract().path("id");
        assertTrue(!ids.contains(drills.id(drill).toString()), "a deleted Drill is still listed");
        drills.read(drill).then().statusCode(404);
    }

    private void theDrillFails(String drill, String reason) {
        drills.awaitJob(drills.id(drill));
        drills.read(drill).then().statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("error", containsString(reason))
                .body("currentVersion", equalTo(null));
    }
}
