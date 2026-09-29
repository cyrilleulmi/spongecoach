import { test, expect, Page } from '@playwright/test';
import { join } from 'node:path';
import { stubUsers } from './users';

const DRILL_ID = 'dddddddd-dddd-dddd-dddd-dddddddddddd';
/** A real photo of the board: 4000×3000 landscape pixels with EXIF orientation 6 (portrait). */
const PHOTO = join(__dirname, '..', '..', 'docs', 'drills', 'examples', 'halbkreis', '1.jpg');

function stage(name: string) {
  return {
    id: 's1',
    name,
    sketches: [1],
    area: 'FULL',
    actors: [
      { id: 'a1', kind: 'PLAYER', side: 'A', label: 'A1', start: { x: -5, y: 0 }, hasBall: true },
      { id: 'a2', kind: 'PLAYER', side: 'A', label: 'A2', start: { x: 5, y: 0 }, hasBall: false },
    ],
    parts: [
      { id: 'p1', name: 'Passgeberin', actorId: 'a1', nextPartId: '' },
      { id: 'p2', name: 'Schützin', actorId: 'a2', nextPartId: '' },
    ],
    props: [],
    steps: [
      { id: 'pass', partId: 'p1', type: 'PASS', path: [{ x: 5, y: 0 }], targetPartId: 'p2', after: '', afterEdge: 'END', delay: 0, speed: 10, duration: 0, sketch: 1, label: '1' },
      { id: 'shot', partId: 'p2', type: 'SHOT', path: [{ x: 0, y: 9.65 }], targetPartId: '', after: 'pass', afterEdge: 'END', delay: 0.5, speed: 25, duration: 0, sketch: 1, label: '2' },
    ],
    repetition: { mode: 'REPLAY', mirrored: false },
  };
}

function drill(overrides: Record<string, unknown> = {}) {
  return {
    id: DRILL_ID,
    name: 'Halbkreis',
    status: 'READY',
    error: null,
    sketchRelation: 'MIXED',
    tags: [],
    sketches: [{ position: 1, note: null, reading: null }],
    currentVersion: 1,
    script: { stages: [stage('Schiessen')], assumptions: [] },
    versions: [{ version: 1, source: 'AI', changeSummary: null, createdAt: '2026-09-27T10:00:00Z' }],
    messages: [{ position: 1, author: 'INTERPRETER', content: 'Skript erstellt.', questions: null, answers: null, scriptVersion: 1, createdAt: '' }],
    openQuestions: [],
    ...overrides,
  };
}

/**
 * The drill endpoints, answering like the backend: a request that starts a job answers PENDING,
 * the first read after it is still PENDING, and the one after that finds the job done.
 */
async function mockApi(page: Page) {
  await stubUsers(page);
  let current: ReturnType<typeof drill> = drill();
  let done = current;
  let pendingReads = 0;
  const startJob = (result: ReturnType<typeof drill>) => {
    done = result;
    current = { ...result, status: 'PENDING' };
    pendingReads = 1;
    return current;
  };

  await page.route('**/api/drill-tags', (route) => route.fulfill({ json: [{ id: 't-shoot', name: 'Schiessen' }] }));
  await page.route('**/api/drills', (route) => {
    if (route.request().method() === 'POST') {
      startJob(drill());
      return route.fulfill({ status: 202, json: { ...current, script: null, currentVersion: null, versions: [], messages: [] } });
    }
    return route.fulfill({ json: [{ id: DRILL_ID, name: current.name, status: current.status, tags: [], sketchCount: 1, updatedAt: '' }] });
  });
  await page.route(`**/api/drills/${DRILL_ID}`, (route) => {
    if (current.status === 'PENDING' && pendingReads-- <= 0) current = done;
    return route.fulfill({ json: current });
  });
  await page.route(`**/api/drills/${DRILL_ID}/sketches/1`, (route) => route.fulfill({ path: PHOTO, contentType: 'image/jpeg' }));
  await page.route(`**/api/drills/${DRILL_ID}/chat`, (route) => {
    const message = route.request().postDataJSON().message;
    const corrected = drill({
      currentVersion: 2,
      script: { stages: [stage('Schiessen, schneller')], assumptions: [] },
      versions: [
        { version: 2, source: 'AI', changeSummary: 'Schneller gepasst', createdAt: '' },
        { version: 1, source: 'AI', changeSummary: null, createdAt: '' },
      ],
      messages: [
        ...drill().messages,
        { position: 2, author: 'COACH', content: message, questions: null, answers: null, scriptVersion: null, createdAt: '' },
        { position: 3, author: 'INTERPRETER', content: 'Angepasst.', questions: null, answers: null, scriptVersion: 2, createdAt: '' },
      ],
    });
    return route.fulfill({ status: 202, json: startJob(corrected) });
  });
}

/** Width and height from a JPEG's baseline frame header. */
function jpegSize(bytes: Buffer): { width: number; height: number } {
  for (let i = 2; i < bytes.length - 9; i++) {
    if (bytes[i] === 0xff && bytes[i + 1] === 0xc0) {
      return { height: bytes.readUInt16BE(i + 5), width: bytes.readUInt16BE(i + 7) };
    }
  }
  throw new Error('no JPEG frame header in the upload');
}

test.describe('Drills', () => {
  // spec: ui.drill-upload
  // spec: ui.drill-waits-while-interpreting
  test('uploads a board photo upright and downscaled, waits for Claude, then plays the Drill', async ({ page }) => {
    await mockApi(page);
    await page.goto('/uebungen');
    await page.getByRole('link', { name: 'Neue Übung' }).click();

    await page.getByPlaceholder('z. B. Bresil').fill('Halbkreis');
    await page.locator('.file-input').setInputFiles(PHOTO);
    await expect(page.locator('.sketch-item')).toHaveCount(1);
    await page.getByRole('button', { name: 'Schiessen' }).click();

    const upload = page.waitForRequest((r) => r.url().endsWith('/api/drills') && r.method() === 'POST');
    await page.getByRole('button', { name: 'Hochladen und animieren' }).click();
    const body = (await upload).postDataBuffer()!;
    // The phone's EXIF rotation is applied, and the long edge is cut to 1568 px.
    expect(jpegSize(body)).toEqual({ width: 1176, height: 1568 });
    expect(body.toString('latin1')).toContain('name="tagIds"');

    await expect(page).toHaveURL(new RegExp(`/uebungen/${DRILL_ID}$`));
    await expect(page.getByRole('status')).toContainText('Claude liest die Skizzen');
    await expect(page.locator('app-rink-player .actor')).toHaveCount(2, { timeout: 10_000 });
    await expect(page.getByRole('status')).toHaveCount(0);
  });

  // spec: ui.drill-plays
  // spec: ui.drill-chat-correction
  test('plays the animation and takes a correction by chat', async ({ page }) => {
    await mockApi(page);
    await page.goto(`/uebungen/${DRILL_ID}`);

    const ball = page.locator('app-rink-player .ball');
    await expect(ball).toHaveCount(1);
    const start = Number(await ball.getAttribute('cx'));
    await expect.poll(async () => Number(await ball.getAttribute('cx')), { timeout: 5_000 }).not.toBe(start);

    await page.getByPlaceholder(/Was stimmt nicht/).fill('Der Pass soll schneller sein');
    await page.getByRole('button', { name: 'Senden' }).click();

    await expect(page.locator('.chat-message').nth(1)).toContainText('Der Pass soll schneller sein');
    await expect(page.locator('.chat-message').nth(2)).toContainText('Version 2', { timeout: 10_000 });
    await expect(page.locator('.stage-name')).toHaveText('Schiessen, schneller');
  });

  // spec: ui.drill-player-read-only
  test('lets a Player watch a Drill without changing it', async ({ page, context }) => {
    await mockApi(page);
    await context.addCookies([{ name: 'spongecoach-user', value: 'u-carmela', url: 'http://localhost:4200' }]);
    await page.goto('/');
    await page.getByRole('link', { name: 'Übungen', exact: true }).click();

    await expect(page.getByRole('link', { name: 'Neue Übung' })).toHaveCount(0);
    await page.locator('.drill-link').click();
    await expect(page.locator('app-rink-player .actor')).toHaveCount(2);
    await expect(page.getByPlaceholder(/Was stimmt nicht/)).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Von Hand bearbeiten' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Löschen' })).toHaveCount(0);
  });
});
