package com.spongecoach.spec.steps;

import com.spongecoach.spec.support.ScenarioWorld;
import io.cucumber.java.en.Then;
import io.quarkiverse.cucumber.ScenarioScope;
import jakarta.inject.Inject;

import static org.hamcrest.Matchers.equalTo;

/**
 * The outcomes many scenarios end on, phrased once. Codes come from
 * {@code ApiExceptionMapper}: {@code bad_request}, {@code not_found}, {@code scheduling_conflict}.
 */
@ScenarioScope
public class SharedSteps {

    @Inject
    ScenarioWorld world;

    @Then("the request is rejected as a bad request")
    public void theRequestIsRejectedAsABadRequest() {
        world.response().then().statusCode(400).body("error", equalTo("bad_request"));
    }

    @Then("the response is not found")
    public void theResponseIsNotFound() {
        world.response().then().statusCode(404);
    }

    @Then("the response is a not-found error envelope")
    public void theResponseIsANotFoundErrorEnvelope() {
        world.response().then().statusCode(404).body("error", equalTo("not_found"));
    }

    @Then("the request is rejected as a scheduling conflict")
    public void theRequestIsRejectedAsASchedulingConflict() {
        world.response().then().statusCode(409).body("error", equalTo("scheduling_conflict"));
    }
}
