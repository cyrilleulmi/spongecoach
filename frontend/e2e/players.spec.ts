import { test, expect, Page } from '@playwright/test';

const KIWI = { id: '11111111-1111-1111-1111-111111111111', name: 'Kiwi', color: '#4c8c3d' };
const BAERI = { id: '12121212-1212-1212-1212-121212121212', name: 'Bäri', color: '#8b5e34' };
const CARMELA_ID = '55555555-5555-5555-5555-555555555555';
const STOCKI_ID = '77777777-7777-7777-7777-777777777777';
const SKILL = { id: '88888888-8888-8888-8888-888888888888', name: 'Schusstechnik', color: '#b1467a' };
const GOAL = { id: '99999999-9999-9999-9999-999999999999', name: 'Mehr Abschlüsse suchen', color: '#b1467a' };

async function mockApi(page: Page) {
  let rating = 60;

  await page.route('**/api/players', (route) =>
    route.fulfill({
      json: [
        { id: CARMELA_ID, name: 'Carmela', lines: [BAERI] },
        { id: STOCKI_ID, name: 'Stocki', lines: [] },
      ],
    }),
  );
  await page.route(`**/api/players/${CARMELA_ID}`, (route) =>
    route.fulfill({
      json: {
        id: CARMELA_ID,
        name: 'Carmela',
        lines: [BAERI],
        skills: [{ skillId: SKILL.id, name: SKILL.name, color: SKILL.color, rating }],
        developmentGoals: [GOAL],
      },
    }),
  );
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
