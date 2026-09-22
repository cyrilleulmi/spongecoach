package com.spongecoach.spec.steps;

import com.spongecoach.spec.support.Datetimes;
import com.spongecoach.spec.support.Fixtures;
import com.spongecoach.spec.support.Kind;
import com.spongecoach.spec.support.ScenarioWorld;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

/** Proves docs/spec/focus-planning.feature — the three-way Line-Focus-Event link, free text. */
@ScenarioScope
public class FocusSteps {

    private static final LocalDateTime DEFAULT_SLOT = LocalDateTime.of(2026, 8, 5, 18, 0);
    private static final String DEFAULT_FOCUS_TEXT = "Spielaufbau aus der tiefen Zone";

    @Inject
    Fixtures fixtures;

    @Inject
    ScenarioWorld world;

    /** The attending Lines of the Event under discussion, in the order the Background named them. */
    private final List<String> attendingLines = new ArrayList<>();

    // --- Given ------------------------------------------------------------------

    @Given("an Event {string} attended by the Lines {string} and {string}")
    public void anEventAttendedByTheLines(String eventName, String firstLine, String secondLine) {
        // The Lines must exist before the Event: attendance is snapshotted at creation (ADR-0012).
        fixtures.line(firstLine);
        fixtures.line(secondLine);
        attendingLines.add(firstLine);
        attendingLines.add(secondLine);
        fixtures.event("Planung", "Training", eventName, DEFAULT_SLOT);
    }

    @Given("both Lines have a Focus set")
    public void bothLinesHaveAFocusSet() {
        setAttachments(Map.of(
                attendingLines.get(0), DEFAULT_FOCUS_TEXT,
                attendingLines.get(1), DEFAULT_FOCUS_TEXT))
                .then().statusCode(200);
    }

    @Given("{string} has the Focus {string} for {string}")
    public void hasTheFocusFor(String lineName, String focusText, String eventName) {
        fixtures.testData().attachFocus(
                world.id(Kind.EVENT, eventName), world.id(Kind.LINE, lineName), focusText);
    }

    @Given("a Line {string} created after the Event")
    public void aLineCreatedAfterTheEvent(String lineName) {
        fixtures.line(lineName);
    }

    // --- When -------------------------------------------------------------------

    @When("the coach sets {string} for {string} and {string} for {string} in one call")
    public void theCoachSetsAFocusForEachLine(
            String firstFocus, String firstLine, String secondFocus, String secondLine) {
        world.note("expected", Map.of(firstLine, firstFocus, secondLine, secondFocus));
        world.setResponse(setAttachments(Map.of(firstLine, firstFocus, secondLine, secondFocus)));
    }

    @When("the coach sends a set containing only {string}'s")
    public void theCoachSendsASetContainingOnly(String lineName) {
        world.setResponse(setAttachments(Map.of(lineName, DEFAULT_FOCUS_TEXT)));
    }

    @When("the coach sends an empty attachment set")
    public void theCoachSendsAnEmptyAttachmentSet() {
        world.setResponse(setAttachments(Map.of()));
    }

    @When("the coach changes only {string}'s Focus to {string}")
    public void theCoachChangesOnlyOneLinesFocus(String lineName, String focusText) {
        Map<String, String> attachments = new LinkedHashMap<>();
        for (String line : attendingLines) {
            attachments.put(line, line.equals(lineName) ? focusText : DEFAULT_FOCUS_TEXT);
        }
        world.setResponse(setAttachments(attachments));
    }

    @When("the coach attaches a Focus for a line id that does not exist")
    public void theCoachAttachesAFocusForAnUnknownLine() {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(Map.of(
                        "lineId", UUID.randomUUID().toString(),
                        "focus", DEFAULT_FOCUS_TEXT))))
                .when().put("/api/events/" + world.current(Kind.EVENT)));
    }

    @When("the coach attaches blank Focus text for {string}")
    public void theCoachAttachesBlankFocusTextFor(String lineName) {
        world.setResponse(given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", List.of(Map.of(
                        "lineId", world.id(Kind.LINE, lineName).toString(),
                        "focus", "   "))))
                .when().put("/api/events/" + world.current(Kind.EVENT)));
    }

    @When("the coach attaches {string} for {string}")
    public void theCoachAttachesFocusFor(String focusText, String lineName) {
        world.setResponse(setAttachments(Map.of(lineName, focusText)));
    }

    private Response setAttachments(Map<String, String> focusByLine) {
        List<Map<String, String>> attachments = focusByLine.entrySet().stream()
                .map(entry -> Map.of(
                        "lineId", world.id(Kind.LINE, entry.getKey()).toString(),
                        "focus", entry.getValue()))
                .toList();
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("focusAttachments", attachments))
                .when().put("/api/events/" + world.id(Kind.EVENT, "Einheit 1"));
    }

    // --- Then -------------------------------------------------------------------

    @Then("each Line's Focus comes back inline on the Event")
    public void eachLinesFocusComesBackInline() {
        Map<String, String> expected = world.recall("expected");
        var assertion = world.response().then()
                .statusCode(200)
                .body("focusAttachments", hasSize(expected.size()));
        expected.forEach((line, focus) -> assertion.body(
                "focusAttachments.find { it.lineId == '" + world.id(Kind.LINE, line) + "' }.focus",
                equalTo(focus)));
    }

    @Then("{string} has no Focus for this Event any more")
    public void hasNoFocusForThisEventAnyMore(String lineName) {
        world.response().then()
                .statusCode(200)
                .body("focusAttachments", hasSize(1))
                .body("focusAttachments.lineId", not(hasItem(world.id(Kind.LINE, lineName).toString())));
    }

    @Then("no Line has a Focus for this Event")
    public void noLineHasAFocusForThisEvent() {
        world.response().then().statusCode(200).body("focusAttachments", hasSize(0));
    }

    @Then("{string} keeps the Focus it had")
    public void keepsTheFocusItHad(String lineName) {
        world.response().then()
                .statusCode(200)
                .body("focusAttachments", hasSize(2))
                .body("focusAttachments.find { it.lineId == '" + world.id(Kind.LINE, lineName) + "' }.focus",
                        equalTo(DEFAULT_FOCUS_TEXT));
    }

    @Then("its attachments are unchanged")
    public void itsAttachmentsAreUnchanged() {
        world.response().then()
                .statusCode(200)
                .body("focusAttachments", hasSize(1))
                .body("focusAttachments[0].lineId", equalTo(world.id(Kind.LINE, "Kiwi").toString()));
    }

    @Then("{string}'s attachment on {string} carries its Line name and Focus text inline")
    public void theAttachmentCarriesNamesInline(String lineName, String eventName) {
        String iterationId = world.current(Kind.ITERATION).toString();
        String eventPath = "find { it.id == '" + iterationId + "' }.events"
                + ".find { it.id == '" + world.id(Kind.EVENT, eventName) + "' }";
        String attachmentPath = eventPath
                + ".focusAttachments.find { it.lineId == '" + world.id(Kind.LINE, lineName) + "' }";
        world.response().then()
                .statusCode(200)
                .body(attachmentPath + ".lineName", equalTo(world.actualName(Kind.LINE, lineName)))
                .body(attachmentPath + ".focus", equalTo(DEFAULT_FOCUS_TEXT))
                .body(eventPath + ".scheduledOn", equalTo(Datetimes.echoedFrom(DEFAULT_SLOT)));
    }
}
