#!/usr/bin/env node
// Checks that every scenario in docs/spec/*.feature is traced to a real automated test, and that
// no test traces a scenario id that no longer exists. See docs/spec/README.md.

import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';

const repoRoot = resolve(import.meta.dirname, '..');
const specDir = join(repoRoot, 'docs', 'spec');
const testRoots = [
  join(repoRoot, 'backend', 'src', 'test'),
  join(repoRoot, 'frontend', 'src'),
  join(repoRoot, 'frontend', 'e2e'),
];

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

const scenarios = new Map(); // id -> { file, unverified, title }
for (const file of walk(specDir, /\.feature$/)) {
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
        scenarios.set(id, { file: relative(repoRoot, file), unverified, title });
        unverified = false;
      }
      continue;
    }
    if (line !== '' && !line.startsWith('#')) unverified = false;
  }
}

const markers = new Map(); // id -> [locations]
for (const root of testRoots) {
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

const uncovered = [...scenarios].filter(([id, s]) => !s.unverified && !markers.has(id));
const unverified = [...scenarios].filter(([, s]) => s.unverified);
const orphaned = [...markers].filter(([id]) => !scenarios.has(id));
const covered = scenarios.size - uncovered.length - unverified.length;

console.log(`spec coverage: ${covered}/${scenarios.size} scenarios traced to a test`);

if (unverified.length) {
  console.log(`\nknown gaps (@unverified, no test expected):`);
  for (const [id, s] of unverified) console.log(`  @spec:${id}  ${s.title}`);
}
if (uncovered.length) {
  console.log(`\nUNCOVERED — a scenario with no test marker:`);
  for (const [id, s] of uncovered) console.log(`  @spec:${id}  (${s.file})  ${s.title}`);
}
if (orphaned.length) {
  console.log(`\nORPHANED — a test marks a scenario id that does not exist:`);
  for (const [id, locations] of orphaned) console.log(`  spec: ${id}  at ${locations.join(', ')}`);
}

process.exit(uncovered.length || orphaned.length ? 1 : 0);
