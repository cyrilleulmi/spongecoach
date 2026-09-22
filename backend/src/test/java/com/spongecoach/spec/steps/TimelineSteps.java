package com.spongecoach.spec.steps;

import com.spongecoach.spec.support.Datetimes;
import com.spongecoach.spec.support.Fixtures;
import com.spongecoach.spec.support.Kind;
import com.spongecoach.spec.support.Names;
import com.spongecoach.spec.support.ScenarioWorld;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/** Proves docs/spec/timeline.feature. */
@ScenarioScope
public class TimelineSteps {

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    /** Which Iteration each spoken Event sits on, for the Iteration-scoped endpoints. */
    private final Map<String, String> iterationOfEvent = new HashMap<>();

    /** The Event the last {@code When} acted on — what a following "it" refers to. */
    private String subjectEvent;

    // --- Given ------------------------------------------------------------------

    @Given("an Iteration {string}")
    public void anIteration(String name) {
        fixtures.iteration(name);
    }

    @Given("a Training {string} in {string} at {string}")
    public void aTrainingIn(String eventName, String iterationName, String datetime) {
        anEventIn("Training", eventName, iterationName, datetime);
    }

    @Given("a Match {string} in {string} at {string}")
    public void aMatchIn(String eventName, String iterationName, String datetime) {
        anEventIn("Match", eventName, iterationName, datetime);
    }

    private void anEventIn(String type, String eventName, String iterationName, String datetime) {
        fixtures.event(iterationName, type, eventName, Datetimes.parse(datetime));
        iterationOfEvent.put(eventName, iterationName);
    }

    // --- When -------------------------------------------------------------------

    @When("the Iteration list is read")
    public void theIterationListIsRead() {
        world.setResponse(given().when().get("/api/iterations"));
    }

    @When("an Iteration that does not exist is read")
    public void anIterationThatDoesNotExistIsRead() {
        world.setResponse(given().when().get("/api/iterations/" + UUID.randomUUID()));
    }

    @When("the coach creates an Iteration {string} holding a Match {string} at {string} and a Training at {string}")
    public void theCoachCreatesAnIterationHolding(
            String iterationName, String matchName, String matchAt, String trainingAt) {
        String actualIterationName = world.uniquify(iterationName);
        String actualMatchName = world.uniquify(matchName);
        var response = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "name", actualIterationName,
                        "position", 900,
                        "events", List.of(
                                Map.of(
                                        "eventTypeId", fixtures.testData().eventType("Match").id.toString(),
                                        "name", actualMatchName,
                                        "scheduledOn", Datetimes.parse(matchAt).toString()),
                                Map.of(
                                        "eventTypeId", fixtures.testData().eventType("Training").id.toString(),
                                        "scheduledOn", Datetimes.parse(trainingAt).toString()))))
                .when().post("/api/iterations");
        world.setResponse(response);
        world.note("matchName", actualMatchName);
        world.note("trainingAt", trainingAt);
        world.note("matchAt", matchAt);
        if (response.statusCode() == 201) {
            UUID id = UUID.fromString(response.path("id"));
            world.register(Kind.ITERATION, iterationName, id, actualIterationName);
            world.onCleanup(() -> fixtures.testData().deleteIteration(id));
        }
    }

    @When("the coach creates an Iteration with a blank name")
    public void theCoachCreatesAnIterationWithABlankName() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("position", 1))
                .when().post("/api/iterations"));
    }

    @When("the coach renames it to {string} at position {int}")
    public void theCoachRenamesItToAtPosition(String name, int position) {
        String actualName = world.uniquify(name);
        world.note("iterationName", actualName);
        world.note("position", position);
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName, "position", position))
                .when().put("/api/iterations/" + world.current(Kind.ITERATION)));
    }

    @When("the coach deletes the Iteration")
    public void theCoachDeletesTheIteration() {
        world.setResponse(given().when().delete("/api/iterations/" + world.current(Kind.ITERATION)));
    }

    @When("the coach adds a Training to {string} with no datetime")
    public void theCoachAddsATrainingWithNoDatetime(String iterationName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("eventTypeId", fixtures.testData().eventType("Training").id.toString()))
                .when().post("/api/iterations/" + world.id(Kind.ITERATION, iterationName) + "/events"));
    }

    @When("the coach adds a Training to {string} at {string}")
    public void theCoachAddsATrainingAt(String iterationName, String datetime) {
        world.note("scheduledOn", datetime);
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "eventTypeId", fixtures.testData().eventType("Training").id.toString(),
                        "scheduledOn", Datetimes.parse(datetime).toString()))
                .when().post("/api/iterations/" + world.id(Kind.ITERATION, iterationName) + "/events"));
    }

    @When("the coach adds an Event of an unknown Event type to {string}")
    public void theCoachAddsAnEventOfAnUnknownType(String iterationName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "eventTypeId", UUID.randomUUID().toString(),
                        "scheduledOn", "2026-08-05T18:00:00"))
                .when().post("/api/iterations/" + world.id(Kind.ITERATION, iterationName) + "/events"));
    }

    @When("the coach moves {string} to {string}")
    public void theCoachMovesTo(String eventName, String datetime) {
        world.note("scheduledOn", datetime);
        subjectEvent = eventName;
        String iterationId = world.id(Kind.ITERATION, iterationOfEvent.get(eventName)).toString();
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("scheduledOn", Datetimes.parse(datetime).toString()))
                .when().put("/api/iterations/" + iterationId + "/events/" + world.id(Kind.EVENT, eventName)));
    }

    @When("the coach moves {string} to {string} by Event id alone")
    public void theCoachMovesToByEventIdAlone(String eventName, String datetime) {
        world.note("scheduledOn", datetime);
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("scheduledOn", Datetimes.parse(datetime).toString()))
                .when().put("/api/events/" + world.id(Kind.EVENT, eventName)));
    }

    @When("the coach renames {string} to {string}")
    public void theCoachRenamesTo(String eventName, String newName) {
        String actualName = world.uniquify(newName);
        world.note("eventName", actualName);
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", actualName))
                .when().put("/api/events/" + world.id(Kind.EVENT, eventName)));
    }

    @When("the coach deletes the Event {string}")
    public void theCoachDeletesTheEvent(String eventName) {
        String iterationId = world.id(Kind.ITERATION, iterationOfEvent.get(eventName)).toString();
        world.setResponse(given().when().delete(
                "/api/iterations/" + iterationId + "/events/" + world.id(Kind.EVENT, eventName)));
    }

    @When("an Event that does not exist is updated")
    public void anEventThatDoesNotExistIsUpdated() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "egal"))
                .when().put("/api/events/" + UUID.randomUUID()));
    }

    // --- Then -------------------------------------------------------------------

    @Then("^\"([^\"]*)\" nests its Events in the order (.+)$")
    public void nestsItsEventsInTheOrder(String iterationName, String eventNames) {
        List<String> expected = Names.in(eventNames).stream()
                .map(name -> world.id(Kind.EVENT, name).toString())
                .toList();
        world.response().then()
                .statusCode(200)
                .body("find { it.id == '" + world.id(Kind.ITERATION, iterationName) + "' }.events.id",
                        equalTo(expected));
    }

    @Then("the Iteration and both Events are persisted, in datetime order")
    public void theIterationAndBothEventsArePersisted() {
        String matchName = world.recall("matchName");
        String trainingAt = world.recall("trainingAt");
        String matchAt = world.recall("matchAt");
        world.response().then().statusCode(201).body("events.size()", equalTo(2));

        // The Events were sent Match-first; the timeline read is what must be in datetime order.
        given().when().get("/api/iterations/" + world.current(Kind.ITERATION)).then()
                .statusCode(200)
                .body("events.type", equalTo(List.of("Training", "Match")))
                .body("events[0].scheduledOn", equalTo(Datetimes.echoed(trainingAt)))
                .body("events[1].name", equalTo(matchName))
                .body("events[1].scheduledOn", equalTo(Datetimes.echoed(matchAt)));
    }

    @Then("the new name and position come back on the next read")
    public void theNewNameAndPositionComeBack() {
        String name = world.recall("iterationName");
        int position = world.recall("position");
        world.response().then().statusCode(200);
        given().when().get("/api/iterations/" + world.current(Kind.ITERATION)).then()
                .statusCode(200)
                .body("name", equalTo(name))
                .body("position", equalTo(position));
    }

    @Then("neither it nor its Events appear on the timeline")
    public void neitherItNorItsEventsAppear() {
        String iterationId = world.current(Kind.ITERATION).toString();
        world.response().then().statusCode(204);
        given().when().get("/api/iterations").then()
                .statusCode(200)
                .body("id", not(hasItem(iterationId)));
        given().when().get("/api/iterations/" + iterationId).then().statusCode(404);
    }

    @Then("that datetime is returned on the created Event")
    public void thatDatetimeIsReturnedOnTheCreatedEvent() {
        world.response().then()
                .statusCode(201)
                .body("scheduledOn", equalTo(Datetimes.echoed(world.recall("scheduledOn"))));
    }

    @Then("the Event is created")
    public void theEventIsCreated() {
        world.response().then().statusCode(201);
    }

    @Then("its datetime is {string}, and it now comes after {string} on the timeline")
    public void itsDatetimeIsAndItComesAfter(String datetime, String otherEventName) {
        world.response().then().statusCode(200).body("scheduledOn", equalTo(Datetimes.echoed(datetime)));
        given().when().get("/api/iterations/" + world.id(Kind.ITERATION, iterationOfEvent.get(subjectEvent)))
                .then()
                .statusCode(200)
                .body("events.id", equalTo(List.of(
                        world.id(Kind.EVENT, otherEventName).toString(),
                        world.id(Kind.EVENT, subjectEvent).toString())));
    }

    @Then("its datetime is {string} — an Event never collides with itself")
    public void itsDatetimeIsUnchanged(String datetime) {
        world.response().then().statusCode(200).body("scheduledOn", equalTo(Datetimes.echoed(datetime)));
    }

    @Then("the name is stored, and the Event's Focus attachments are left untouched")
    public void theNameIsStoredAndAttachmentsUntouched() {
        world.response().then()
                .statusCode(200)
                .body("name", equalTo(world.recall("eventName")))
                .body("focusAttachments.size()", equalTo(1));
    }

    @Then("only {string} is gone and {string} remains")
    public void onlyIsGoneAndRemains(String goneEventName, String keptEventName) {
        world.response().then().statusCode(204);
        given().when().get("/api/iterations/" + world.id(Kind.ITERATION, iterationOfEvent.get(keptEventName)))
                .then()
                .statusCode(200)
                .body("events.id", hasItem(world.id(Kind.EVENT, keptEventName).toString()))
                .body("events.id", not(hasItem(world.id(Kind.EVENT, goneEventName).toString())));
    }
}
