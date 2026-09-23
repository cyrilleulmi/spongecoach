import { test, expect, Page } from '@playwright/test';

const KIWI = { id: '11111111-1111-1111-1111-111111111111', name: 'Kiwi', color: '#4c8c3d' };
const BAERI = { id: '12121212-1212-1212-1212-121212121212', name: 'Bäri', color: '#8b5e34' };
const CARMELA_ID = '55555555-5555-5555-5555-555555555555';
const STOCKI_ID = '77777777-7777-7777-7777-777777777777';
const SKILL = { id: '88888888-8888-8888-8888-888888888888', name: 'Schusstechnik', color: '#b1467a' };
const GOAL = { id: '99999999-9999-9999-9999-999999999999', name: 'Mehr Abschlüsse suchen', color: '#b1467a' };

async function mockApi(page: Page) {
  let rating = 60;
  let avatar: { version: number; png: Buffer } | null = null;

  await page.route('**/api/players', (route) =>
    route.fulfill({
      json: [
        { id: CARMELA_ID, name: 'Carmela', lines: [BAERI], avatarVersion: avatar?.version ?? null },
        { id: STOCKI_ID, name: 'Stocki', lines: [], avatarVersion: null },
      ],
    }),
  );
  const carmela = () => ({
    id: CARMELA_ID,
    name: 'Carmela',
    lines: [BAERI],
    skills: [{ skillId: SKILL.id, name: SKILL.name, color: SKILL.color, rating }],
    developmentGoals: [GOAL],
    avatarVersion: avatar?.version ?? null,
  });
  await page.route(`**/api/players/${CARMELA_ID}`, (route) => route.fulfill({ json: carmela() }));
  // The Avatar round-trips like the backend: PUT stores the PNG under a new version, GET serves it.
  await page.route(`**/api/players/${CARMELA_ID}/avatar*`, (route) => {
    const request = route.request();
    if (request.method() === 'PUT') {
      avatar = { version: (avatar?.version ?? 0) + 1, png: request.postDataBuffer()! };
      return route.fulfill({ json: carmela() });
    }
    if (request.method() === 'DELETE') {
      avatar = null;
      return route.fulfill({ status: 204 });
    }
    return avatar
      ? route.fulfill({ contentType: 'image/png', body: avatar.png })
      : route.fulfill({ status: 404, json: { error: 'not_found' } });
  });
  await page.route(`**/api/players/${CARMELA_ID}/skills/${SKILL.id}`, (route) => {
    rating = route.request().postDataJSON().rating;
    return route.fulfill({ json: { skillId: SKILL.id, name: SKILL.name, color: SKILL.color, rating } });
  });
  await page.route('**/api/player-skills', (route) => route.fulfill({ json: [SKILL] }));
  await page.route('**/api/player-development-goals', (route) => route.fulfill({ json: [GOAL] }));

  // The line screen, reached through a Line badge.
  await page.route('**/api/lines', (route) =>
    route.fulfill({ json: [KIWI, BAERI].map((l) => ({ ...l, playerCount: 1 })) }),
  );
  await page.route('**/api/skills', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/development-goals', (route) => route.fulfill({ json: [] }));
  for (const line of [KIWI, BAERI]) {
    await page.route(`**/api/lines/${line.id}`, (route) =>
      route.fulfill({
        json: { id: line.id, name: line.name, players: [{ id: CARMELA_ID, name: 'Carmela' }], skills: [], developmentGoals: [] },
      }),
    );
  }
}

test.describe('Player screens', () => {
  test('goes from the Player list to a Player and rates a Skill', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await page.getByRole('link', { name: 'Spieler', exact: true }).click();

    await expect(page.locator('.player-row')).toHaveCount(2);
    await page.locator('.player-link', { hasText: 'Carmela' }).click();

    await expect(page).toHaveURL(new RegExp(`/players/${CARMELA_ID}$`));
    await expect(page.getByRole('heading', { name: 'Carmela' })).toBeVisible();
    await expect(page.getByText('Mehr Abschlüsse suchen')).toBeVisible();
    await expect(page.getByText('3/5')).toBeVisible();

    await page.getByRole('button', { name: 'Bewertung auf 5 von 5 setzen' }).click();
    await expect(page.getByText('5/5')).toBeVisible();
  });

  // spec: ui.avatar-painter-opens
  // spec: ui.avatar-save
  // spec: ui.avatar-painted-or-initials
  test('paints an Avatar and shows it on the player screen and in the list', async ({ page }) => {
    await mockApi(page);
    await page.goto(`/players/${CARMELA_ID}`);
    await expect(page.locator('.page-head app-player-avatar')).toHaveText('C');

    await page.getByRole('button', { name: 'Profilbild malen' }).click();
    const painter = page.getByRole('dialog', { name: 'Profilbild malen' });
    await expect(painter).toBeVisible();

    await painter.getByRole('button', { name: 'Farbe #d93b3b' }).click();
    await painter.getByRole('button', { name: 'Grösse L' }).click();
    const canvas = painter.getByLabel('Malfläche');
    const box = (await canvas.boundingBox())!;
    const cx = box.x + box.width / 2;
    const cy = box.y + box.height / 2;
    await page.mouse.move(cx - 40, cy);
    await page.mouse.down();
    await page.mouse.move(cx, cy, { steps: 8 });
    await page.mouse.move(cx + 40, cy, { steps: 8 });
    await page.mouse.up();

    const centerPixel = await canvas.evaluate((el: HTMLCanvasElement) =>
      Array.from(el.getContext('2d')!.getImageData(256, 256, 1, 1).data),
    );
    expect(centerPixel).toEqual([0xd9, 0x3b, 0x3b, 255]);

    const upload = page.waitForRequest((r) => r.url().endsWith('/avatar') && r.method() === 'PUT');
    await painter.getByRole('button', { name: 'Speichern' }).click();
    const body = (await upload).postDataBuffer()!;
    expect(body.subarray(1, 4).toString('ascii')).toBe('PNG');

    await expect(painter).toBeHidden();
    const headAvatar = page.locator('.page-head img.avatar');
    await expect(headAvatar).toHaveAttribute('src', `/api/players/${CARMELA_ID}/avatar?v=1`);
    await expect.poll(() => headAvatar.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBe(512);

    await page.getByRole('link', { name: '← Spieler' }).click();
    await expect(page.locator('.player-row', { hasText: 'Carmela' }).locator('img.avatar')).toBeVisible();
    await expect(page.locator('.player-row', { hasText: 'Stocki' }).locator('app-player-avatar')).toHaveText('S');
  });

  // spec: ui.avatar-eraser
  test('the eraser paints the blank background back over a stroke, edges included', async ({ page }) => {
    await mockApi(page);
    await page.goto(`/players/${CARMELA_ID}`);
    await page.getByRole('button', { name: 'Profilbild malen' }).click();
    const painter = page.getByRole('dialog', { name: 'Profilbild malen' });
    const canvas = painter.getByLabel('Malfläche');
    const box = (await canvas.boundingBox())!;
    const cx = box.x + box.width / 2;
    const cy = box.y + box.height / 2;
    const swipe = async () => {
      await page.mouse.move(cx - 30, cy);
      await page.mouse.down();
      await page.mouse.move(cx + 30, cy, { steps: 10 });
      await page.mouse.up();
    };
    // Canvas pixels along a vertical line through the stroke, edges included.
    const column = () =>
      canvas.evaluate((el: HTMLCanvasElement) => {
        const data = el.getContext('2d')!.getImageData(256, 216, 1, 80).data;
        return Array.from({ length: 80 }, (_, i) => Array.from(data.slice(i * 4, i * 4 + 4)));
      });
    // Anti-aliasing may round a channel by a step or two; a leftover edge is off by far more.
    const isBlank = (pixel: number[]) => [226, 239, 233, 255].every((channel, i) => Math.abs(pixel[i] - channel) <= 3);

    await painter.getByRole('button', { name: 'Farbe #d93b3b' }).click();
    await painter.getByRole('button', { name: 'Grösse L' }).click();
    await swipe();
    expect(await column()).toContainEqual([217, 59, 59, 255]);

    await painter.getByRole('button', { name: 'Radierer' }).click();
    await swipe();

    expect((await column()).filter((pixel) => !isBlank(pixel))).toEqual([]);
  });

  // spec: ui.avatar-painter-edit-or-new
  // spec: ui.avatar-painter-shows-crop
  test('reopening the painter starts on the saved Avatar, with the circle guide shown', async ({ page }) => {
    await mockApi(page);
    await page.goto(`/players/${CARMELA_ID}`);
    await page.getByRole('button', { name: 'Profilbild malen' }).click();
    const painter = page.getByRole('dialog', { name: 'Profilbild malen' });
    await painter.getByRole('button', { name: 'Füllen' }).click();
    await painter.getByRole('button', { name: 'Farbe #6ec3f0' }).click();
    await painter.getByLabel('Malfläche').click({ position: { x: 10, y: 10 } });
    await painter.getByRole('button', { name: 'Speichern' }).click();
    await expect(painter).toBeHidden();

    await page.getByRole('button', { name: 'Profilbild malen' }).click();
    await expect(painter.locator('.crop-guide')).toBeVisible();
    await expect(painter.getByRole('button', { name: 'Bearbeiten' })).toHaveAttribute('aria-pressed', 'true');
    const corner = () =>
      painter
        .getByLabel('Malfläche')
        .evaluate((el: HTMLCanvasElement) => Array.from(el.getContext('2d')!.getImageData(5, 5, 1, 1).data));
    await expect.poll(corner).toEqual([0x6e, 0xc3, 0xf0, 255]);

    await painter.getByRole('button', { name: 'Neu' }).click();
    await expect.poll(corner).not.toEqual([0x6e, 0xc3, 0xf0, 255]);
    await painter.getByRole('button', { name: 'Rückgängig' }).click();
    await expect.poll(corner).toEqual([0x6e, 0xc3, 0xf0, 255]);
  });

  test('a Line badge opens that Line on the line screen', async ({ page }) => {
    await mockApi(page);
    await page.goto(`/players/${CARMELA_ID}`);

    await page.getByRole('link', { name: 'Block Bäri öffnen' }).click();

    await expect(page).toHaveURL(new RegExp(`/lines\\?line=${BAERI.id}$`));
    await expect(page.getByRole('heading', { name: 'Bäri', exact: true })).toBeVisible();
  });

  test('a roster row on the line screen opens the Player', async ({ page }) => {
    await mockApi(page);
    await page.goto('/lines');

    await page.locator('.roster-row .player-link', { hasText: 'Carmela' }).click();

    await expect(page.getByRole('heading', { name: 'Carmela' })).toBeVisible();
  });

  test('an unknown Player shows a not-found page', async ({ page }) => {
    await mockApi(page);
    await page.route('**/api/players/00000000-0000-0000-0000-000000000000', (route) =>
      route.fulfill({ status: 404, json: { error: 'not_found', message: 'nope' } }),
    );
    await page.goto('/players/00000000-0000-0000-0000-000000000000');

    await expect(page.getByText('Spieler nicht gefunden')).toBeVisible();
  });
});
