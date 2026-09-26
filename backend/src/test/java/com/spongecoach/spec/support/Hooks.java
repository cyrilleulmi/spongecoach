package com.spongecoach.spec.support;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.quarkiverse.cucumber.ScenarioScope;
import jakarta.inject.Inject;

/**
 * Undoes what a scenario created. Every scenario in the run shares one Quarkus boot and one
 * database, so this has to be thorough — a leaked Line will surface later as a puzzling failure in
 * an unrelated listing scenario.
 */
@ScenarioScope
public class Hooks {

    @Inject
    ScenarioWorld world;

    @Inject
    Session session;

    @Before
    public void actAsSysAdmin() {
        session.actAs(Session.SEEDED_SYS_ADMIN);
    }

    @After
    public void tearDown() {
        // Cleanup goes through TestData, not HTTP, but no later scenario should inherit this User.
        session.actAsNobody();
        world.runCleanups();
    }
}
