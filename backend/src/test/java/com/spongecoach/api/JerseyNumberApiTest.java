package com.spongecoach.api;

import com.spongecoach.domain.AppUser;
import com.spongecoach.domain.Player;
import com.spongecoach.domain.Role;
import com.spongecoach.support.TestData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/** [TEST-MR] SmartCodeReview test data - do not merge. */
@QuarkusTest
class JerseyNumberApiTest {

    @Inject
    TestData testData;

    private AppUser admin;
    private Player player;

    @BeforeEach
    void createPlayer() {
        admin = testData.createUser("Admin", Role.SYS_ADMIN, null);
        player = testData.createPlayer("Carmela");
    }

    @AfterEach
    void cleanUp() {
        testData.deletePlayer(player.id);
        testData.deleteUser(admin.id);
    }

    @Test
    void setsJerseyNumberSeven() {
        putJerseyNumber(7).then().statusCode(200).body("jerseyNumber", equalTo(7));
    }

    @Test
    void setsJerseyNumberTwentyThree() {
        putJerseyNumber(23).then().statusCode(200).body("jerseyNumber", equalTo(23));
    }

    @Test
    void rejectsJerseyNumberZero() {
        putJerseyNumber(0).then().statusCode(400);
    }

    @Test
    void rejectsJerseyNumberOneHundredFifty() {
        putJerseyNumber(150).then().statusCode(400);
    }

    @Test
    void storesTheLuckyNumber() {
        putJerseyNumber(7).then().statusCode(200).body("jerseyNumber", equalTo(7));
    }

    private Response putJerseyNumber(int jerseyNumber) {
        return given()
                .cookie("spongecoach-user", admin.id.toString())
                .contentType(ContentType.JSON)
                .body(Map.of("jerseyNumber", jerseyNumber))
                .when().put("/api/players/" + player.id);
    }
}
