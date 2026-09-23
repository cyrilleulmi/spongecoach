package com.spongecoach.spec;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;

/**
 * Runs the behavioural specification in {@code docs/spec} against the real HTTP API.
 *
 * <p>The feature files stay in {@code docs/spec} rather than under {@code src/test/resources}
 * because they are documentation first (see {@code docs/spec/README.md}); paths here are relative
 * to the backend project directory, which is the test task's working directory.
 *
 * <p>Features are listed one by one on purpose: {@code ui.feature} lives in the same folder but is
 * proven by the frontend suites, and has no step definitions here.
 *
 * <p>{@code @unverified} scenarios describe behaviour v1 does not implement yet. They are kept as
 * documentation and skipped, and the spec-coverage checker reports them as known gaps.
 */
@CucumberOptions(
        features = {
                "../docs/spec/catalogs.feature",
                "../docs/spec/lines.feature",
                "../docs/spec/timeline.feature",
                "../docs/spec/focus-planning.feature",
                "../docs/spec/attendance.feature",
                "../docs/spec/players.feature",
        },
        glue = "com.spongecoach.spec",
        tags = "not @unverified",
        plugin = {"json:build/cucumber-report.json"})
public class SpecTest extends CucumberQuarkusTest {
}
