#!/usr/bin/env node
// Checks that every scenario in docs/spec/*.feature is actually verified. See docs/spec/README.md.
//
// Two mechanisms, because the two layers are proven differently:
//
//   - Backend features are RUN by Cucumber (backend/src/test/.../SpecTest.java). An undefined step
//     already fails that suite, so traceability is structural. What this checker adds is the one
//     thing the runtime will not tell you: that a scenario was actually executed rather than
//     silently filtered out by a tag or dropped from SpecTest's feature list. It reads the run's
//     report to confirm that.
//   - ui.feature is proven by the frontend suites, which are not Cucumber. It keeps the original
//     `// spec: <id>` marker protocol, scanned here.

import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';

const repoRoot = resolve(import.meta.dirname, '..');
const specDir = join(repoRoot, 'docs', 'spec');
const cucumberReport = join(repoRoot, 'backend', 'build', 'cucumber-report.json');

/** Features run by Cucumber. Must match SpecTest's `features` list. */
const CUCUMBER_FEATURES = new Set([
  'attendance.feature',
  'catalogs.feature',
  'focus-planning.feature',
  'lines.feature',
  'timeline.feature',
]);

/** Where the marker-traced (frontend) layer's tests live. */
const MARKER_TEST_ROOTS = [join(repoRoot, 'frontend', 'src'), join(repoRoot, 'frontend', 'e2e')];

function walk(dir, match) {
  const found = [];
  let entries;
  try {
    entries = readdirSync(dir);
  } catch {
    return found;
  }
  for (const entry of entries) {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) {
      found.push(...walk(path, match));
    } else if (match.test(entry)) {
      found.push(path);
    }
  }
  return found;
}

// --- what the spec claims ----------------------------------------------------

const scenarios = new Map(); // id -> { file, basename, unverified, title, runByCucumber }
for (const file of walk(specDir, /\.feature$/)) {
  const basename = relative(specDir, file);
  const lines = readFileSync(file, 'utf8').split(/\r?\n/);
  let unverified = false;
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith('@')) {
      if (line.includes('@unverified')) unverified = true;
      const id = line.match(/@spec:([\w.-]+)/)?.[1];
      if (id) {
        const title = lines.slice(i + 1).find((l) => l.trim().startsWith('Scenario'))?.trim() ?? '';
        if (scenarios.has(id)) {
          console.error(`duplicate scenario id @spec:${id}`);
          process.exit(2);
        }
        scenarios.set(id, {
          file: relative(repoRoot, file),
          basename,
          unverified,
          title,
          runByCucumber: CUCUMBER_FEATURES.has(basename),
        });
        unverified = false;
      }
      continue;
    }
    if (line !== '' && !line.startsWith('#')) unverified = false;
  }
}

// A feature file that nothing runs and nothing scans would pass silently. Catch that here.
const knownFeatures = new Set([...CUCUMBER_FEATURES, 'ui.feature']);
const strayFeatures = [...new Set([...scenarios.values()].map((s) => s.basename))].filter(
  (basename) => !knownFeatures.has(basename),
);

// --- what the Cucumber run actually executed ---------------------------------

const executed = new Map(); // id -> status
let reportMissing = false;
if (existsSync(cucumberReport)) {
  const report = JSON.parse(readFileSync(cucumberReport, 'utf8'));
  for (const feature of report) {
    for (const element of feature.elements ?? []) {
      const ids = (element.tags ?? [])
        .map((tag) => tag.name.match(/^@spec:([\w.-]+)$/)?.[1])
        .filter(Boolean);
      const failed = (element.steps ?? []).some((step) => step.result?.status !== 'passed');
      for (const id of ids) {
        // A Scenario Outline appears once per example; one failing example fails the scenario.
        executed.set(id, failed || executed.get(id) === 'failed' ? 'failed' : 'passed');
      }
    }
  }
} else {
  reportMissing = true;
}

// --- what the marker-traced tests claim --------------------------------------

const markers = new Map(); // id -> [locations]
for (const root of MARKER_TEST_ROOTS) {
  for (const file of walk(root, /\.(java|ts)$/)) {
    readFileSync(file, 'utf8')
      .split(/\r?\n/)
      .forEach((line, index) => {
        const marker = line.match(/(?:\/\/|\*)\s*spec:\s*([\w.,\s-]+)/);
        if (!marker) return;
        for (const id of marker[1].split(',').map((s) => s.trim()).filter(Boolean)) {
          if (!markers.has(id)) markers.set(id, []);
          markers.get(id).push(`${relative(repoRoot, file)}:${index + 1}`);
        }
      });
  }
}

// --- verdict -----------------------------------------------------------------

const all = [...scenarios];
const unverified = all.filter(([, s]) => s.unverified);
const cucumberScenarios = all.filter(([, s]) => s.runByCucumber && !s.unverified);
const markerScenarios = all.filter(([, s]) => !s.runByCucumber && !s.unverified);

const notExecuted = reportMissing ? [] : cucumberScenarios.filter(([id]) => !executed.has(id));
const failed = cucumberScenarios.filter(([id]) => executed.get(id) === 'failed');
const uncovered = markerScenarios.filter(([id]) => !markers.has(id));
const orphaned = [...markers].filter(([id]) => !scenarios.has(id));

const cucumberCovered = reportMissing ? 0 : cucumberScenarios.length - notExecuted.length;
const markerCovered = markerScenarios.length - uncovered.length;

if (reportMissing) {
  console.log(
    `spec coverage: ${markerCovered}/${markerScenarios.length} marker-traced scenarios traced to a test`,
  );
  console.log(
    `  backend: SKIPPED — no Cucumber report at ${relative(repoRoot, cucumberReport)}; run the backend tests first`,
  );
} else {
  console.log(`spec coverage: ${cucumberCovered}/${cucumberScenarios.length} scenarios executed by Cucumber, ` +
    `${markerCovered}/${markerScenarios.length} traced to a test by marker`);
}

if (unverified.length) {
  console.log(`\nknown gaps (@unverified, no test expected):`);
  for (const [id, s] of unverified) console.log(`  @spec:${id}  ${s.title}`);
}
if (strayFeatures.length) {
  console.log(`\nUNCHECKED FEATURE FILE — neither run by Cucumber nor scanned for markers:`);
  for (const basename of strayFeatures) {
    console.log(`  docs/spec/${basename}  (add it to SpecTest and to CUCUMBER_FEATURES here)`);
  }
}
if (notExecuted.length) {
  console.log(`\nNOT EXECUTED — a scenario Cucumber never ran (missing from SpecTest, or filtered out):`);
  for (const [id, s] of notExecuted) console.log(`  @spec:${id}  (${s.file})  ${s.title}`);
}
if (failed.length) {
  console.log(`\nFAILED — a scenario Cucumber ran and did not pass:`);
  for (const [id, s] of failed) console.log(`  @spec:${id}  (${s.file})  ${s.title}`);
}
if (uncovered.length) {
  console.log(`\nUNCOVERED — a scenario with no test marker:`);
  for (const [id, s] of uncovered) console.log(`  @spec:${id}  (${s.file})  ${s.title}`);
}
if (orphaned.length) {
  console.log(`\nORPHANED — a test marks a scenario id that does not exist:`);
  for (const [id, locations] of orphaned) console.log(`  spec: ${id}  at ${locations.join(', ')}`);
}

const failures =
  strayFeatures.length + notExecuted.length + failed.length + uncovered.length + orphaned.length;
process.exit(failures ? 1 : 0);
