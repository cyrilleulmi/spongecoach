package com.spongecoach.api;

import com.spongecoach.domain.DevelopmentGoal;
import com.spongecoach.domain.Focus;
import com.spongecoach.domain.Skill;
import com.spongecoach.support.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class CatalogResourceTest {

    @Inject
    TestData testData;

    @Test
    void whenListingSkills_thenOnlyActiveCatalogRowsAreReturned() {
        Skill active = testData.createSkill("Transition speed " + UUID.randomUUID(), "#4f7a3f");
        try {
            given()
                    .when().get("/api/skills")
                    .then()
                    .statusCode(200)
                    .body("id", hasItem(active.id.toString()));
        } finally {
            testData.deleteSkill(active.id);
        }
    }

    @Test
    void whenAGoalIsDeleted_thenItIsExcludedFromTheCatalogListing() {
        DevelopmentGoal goal = testData.createGoal("Build compact defensive shape " + UUID.randomUUID(), "#b1467a");
        testData.softDeleteGoal(goal.id);
        try {
            given()
                    .when().get("/api/development-goals")
                    .then()
                    .statusCode(200)
                    .body("id", not(hasItem(goal.id.toString())));
        } finally {
            testData.deleteGoal(goal.id);
        }
    }

    @Test
    void whenChangingASkillsColor_thenTheNewColorIsPersisted() {
        Skill skill = testData.createSkill("Faceoff control " + UUID.randomUUID(), "#111111");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("color", "#c8722e"))
                    .when().put("/api/skills/" + skill.id)
                    .then()
                    .statusCode(200)
                    .body("color", equalTo("#c8722e"));
        } finally {
            testData.deleteSkill(skill.id);
        }
    }

    @Test
    void whenChangingAGoalsColor_thenTheNewColorIsPersisted() {
        DevelopmentGoal goal = testData.createGoal("Improve power-play execution " + UUID.randomUUID(), "#111111");
        try {
            given()
                    .contentType(ContentType.JSON)
                    .body(Map.of("color", "#3b6ea5"))
                    .when().put("/api/development-goals/" + goal.id)
                    .then()
                    .statusCode(200)
                    .body("color", equalTo("#3b6ea5"));
        } finally {
            testData.deleteGoal(goal.id);
        }
    }

    @Test
    void whenListingFocuses_thenEachCarriesItsOriginatingGoalIds() {
        DevelopmentGoal goal = testData.createGoal("Increase shot volume " + UUID.randomUUID(), "#c8722e");
        Focus focus = testData.createFocus("2-on-1 shooting reps " + UUID.randomUUID(), List.of(goal.id));
        try {
            given()
                    .when().get("/api/focuses")
                    .then()
                    .statusCode(200)
                    .body("find { it.id == '" + focus.id + "' }.goalIds", hasItem(goal.id.toString()));
        } finally {
            testData.deleteFocus(focus.id);
            testData.deleteGoal(goal.id);
        }
    }

    @Test
    void whenCreatingASkill_thenItIsPersistedWithItsChosenColorAndListed() {
        String name = "Passgenauigkeit " + UUID.randomUUID();
        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name, "color", "#4f7a3f"))
                .when().post("/api/skills")
                .then()
                .statusCode(201)
                .body("name", equalTo(name))
                .body("color", equalTo("#4f7a3f"))
                .extract().path("id");
        try {
            given().when().get("/api/skills").then().statusCode(200).body("id", hasItem(id));
        } finally {
            testData.deleteSkill(UUID.fromString(id));
        }
    }

    @Test
    void whenCreatingASkillWithABlankName_thenReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "  ", "color", "#4f7a3f"))
                .when().post("/api/skills")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"));
    }

    @Test
    void whenCreatingADevelopmentGoal_thenItIsListed() {
        String name = "Überzahlspiel verbessern " + UUID.randomUUID();
        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name, "color", "#b1467a"))
                .when().post("/api/development-goals")
                .then()
                .statusCode(201)
                .body("color", equalTo("#b1467a"))
                .extract().path("id");
        try {
            given().when().get("/api/development-goals").then().statusCode(200).body("id", hasItem(id));
        } finally {
            testData.deleteGoal(UUID.fromString(id));
        }
    }

    @Test
    void whenCreatingAFocus_thenItCarriesTheChosenGoalAndIsListed() {
        DevelopmentGoal goal = testData.createGoal("Build compact shape " + UUID.randomUUID(), "#16a085");
        String name = "Breakout patterns " + UUID.randomUUID();
        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name, "goalIds", List.of(goal.id.toString())))
                .when().post("/api/focuses")
                .then()
                .statusCode(201)
                .body("name", equalTo(name))
                .body("goalIds", hasItem(goal.id.toString()))
                .extract().path("id");
        try {
            given()
                    .when().get("/api/focuses")
                    .then()
                    .statusCode(200)
                    .body("find { it.id == '" + id + "' }.goalIds", hasItem(goal.id.toString()));
        } finally {
            testData.deleteFocus(UUID.fromString(id));
            testData.deleteGoal(goal.id);
        }
    }

    @Test
    void whenCreatingAFocusFromSeveralGoals_thenAllAreLinked() {
        DevelopmentGoal first = testData.createGoal("Reduce turnovers " + UUID.randomUUID(), "#c0392b");
        DevelopmentGoal second = testData.createGoal("Build compact shape " + UUID.randomUUID(), "#16a085");
        String name = "Transition trap " + UUID.randomUUID();
        String id = given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", name, "goalIds", List.of(first.id.toString(), second.id.toString())))
                .when().post("/api/focuses")
                .then()
                .statusCode(201)
                .body("goalIds", hasItem(first.id.toString()))
                .body("goalIds", hasItem(second.id.toString()))
                .extract().path("id");
        try {
            given()
                    .when().get("/api/focuses")
                    .then()
                    .statusCode(200)
                    .body("find { it.id == '" + id + "' }.goalIds.size()", equalTo(2));
        } finally {
            testData.deleteFocus(UUID.fromString(id));
            testData.deleteGoal(first.id);
            testData.deleteGoal(second.id);
        }
    }

    @Test
    void whenCreatingAFocusWithoutAGoal_thenReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Orphan focus " + UUID.randomUUID(), "goalIds", List.of()))
                .when().post("/api/focuses")
                .then()
                .statusCode(400)
                .body("error", equalTo("bad_request"));
    }
}
