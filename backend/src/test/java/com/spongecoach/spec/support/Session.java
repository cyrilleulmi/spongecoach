package com.spongecoach.spec.support;

import io.quarkiverse.cucumber.ScenarioScope;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;

import java.util.UUID;

/**
 * Which User the scenario's requests act as (ADR-0017). Sets RestAssured's default request
 * specification, so every {@code given()} in every step class carries the User cookie without
 * naming it. {@link Hooks} starts each scenario as the seeded SysAdmin, which is why the scenarios
 * that are not about authorization never mention a User.
 */
@ScenarioScope
public class Session {

    /** The seeded SysAdmin, from {@code V13__app_user.sql}. */
    public static final UUID SEEDED_SYS_ADMIN = UUID.fromString("90000000-0000-0000-0000-000000000001");

    private static final String COOKIE = "spongecoach-user";

    public void actAs(UUID userId) {
        RestAssured.requestSpecification = new RequestSpecBuilder().addCookie(COOKIE, userId.toString()).build();
    }

    public void actAsNobody() {
        RestAssured.requestSpecification = null;
    }
}
