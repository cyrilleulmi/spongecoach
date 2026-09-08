import { test, expect, Page } from '@playwright/test';

const LINE_ID = '11111111-1111-1111-1111-111111111111';
const SKILL_ID = '22222222-2222-2222-2222-222222222222';
const GOAL_ID = '33333333-3333-3333-3333-333333333333';
const FOCUS_ID = '44444444-4444-4444-4444-444444444444';
const CARMELA_ID = '55555555-5555-5555-5555-555555555555';
const DEBI_ID = '66666666-6666-6666-6666-666666666666';

const TEAM_PLAYERS = [
  { id: CARMELA_ID, name: 'Carmela' },
  { id: DEBI_ID, name: 'Debi' },
];

async function mockApi(page: Page) {
  let rating = 60;
  let rosterIds: string[] = [CARMELA_ID];
  const skills = [{ id: SKILL_ID, name: 'Passgenauigkeit', color: '#2c7a68' }];

  await page.route('**/api/lines', (route) =>
    route.fulfill({ json: [{ id: LINE_ID, name: 'Kiwi', playerCount: rosterIds.length }] }),
  );
  await page.route('**/api/players', (route) => route.fulfill({ json: TEAM_PLAYERS }));

  await page.route('**/api/skills', async (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      const created = { id: 'skill-new-0001', name: body.name, color: body.color };
      skills.push(created);
      return route.fulfill({ status: 201, json: created });
    }
    return route.fulfill({ json: skills });
  });
  await page.route('**/api/development-goals', (route) =>
    route.fulfill({ json: [{ id: GOAL_ID, name: 'Ballverluste im eigenen Drittel reduzieren', color: '#c8722e' }] }),
  );
  await page.route('**/api/focuses', (route) =>
    route.fulfill({ json: [{ id: FOCUS_ID, name: 'Cross-Pässe unter Druck', goalIds: [GOAL_ID] }] }),
  );

  const lineDetail = () => ({
    id: LINE_ID,
    name: 'Kiwi',
    players: rosterIds.map((id) => TEAM_PLAYERS.find((p) => p.id === id)!),
    skills: [{ skillId: SKILL_ID, name: 'Passgenauigkeit', color: '#2c7a68', rating }],
    developmentGoals: [{ id: GOAL_ID, name: 'Ballverluste im eigenen Drittel reduzieren', color: '#c8722e' }],
    focuses: [{ id: FOCUS_ID, name: 'Cross-Pässe unter Druck', goalIds: [GOAL_ID] }],
  });

  await page.route(`**/api/lines/${LINE_ID}`, async (route) => {
    if (route.request().method() !== 'GET') {
      const body = route.request().postDataJSON();
      if (Array.isArray(body?.playerIds)) {
        rosterIds = body.playerIds;
      }
    }
    await route.fulfill({ json: lineDetail() });
  });

  // Generic skill-association route (e.g. auto-associating a freshly created skill).
  await page.route(`**/api/lines/${LINE_ID}/skills/*`, async (route) => {
    const body = route.request().postDataJSON();
    await route.fulfill({ json: { skillId: 'skill-new-0001', name: 'x', color: '#2c7a68', rating: body?.rating ?? 50 } });
  });
  // Specific route for SKILL_ID wins (registered last) — tracks the rating for that skill.
  await page.route(`**/api/lines/${LINE_ID}/skills/${SKILL_ID}`, async (route) => {
    const body = route.request().postDataJSON();
    rating = body.rating;
    await route.fulfill({ json: { skillId: SKILL_ID, name: 'Passgenauigkeit', color: '#2c7a68', rating } });
  });
}

test.describe('Per-line overview page', () => {
  test('shows the roster, skills, goals, and focuses for the selected line', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Kiwi', exact: true })).toBeVisible();
    await expect(page.locator('.roster-row').getByText('Carmela')).toBeVisible();
    await expect(page.getByText('Passgenauigkeit')).toBeVisible();
    await expect(page.getByText('Ballverluste im eigenen Drittel reduzieren').first()).toBeVisible();
    await expect(page.getByText('Cross-Pässe unter Druck')).toBeVisible();
  });

  test('lets the coach rate a skill by clicking a segment of the bar', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    await expect(page.getByText('3/5')).toBeVisible();

    await page.getByRole('button', { name: 'Bewertung auf 5 von 5 setzen' }).click();

    await expect(page.getByText('5/5')).toBeVisible();
  });

  test('lets the coach add an existing team player to the roster via the dialog', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    await expect(page.locator('.roster-row')).toHaveCount(1);

    // "Verwalten" on the Kader card is the first one on the page.
    await page.getByRole('button', { name: 'Verwalten' }).first().click();
    await page.getByRole('button', { name: 'Debi hinzufügen' }).click();

    await expect(page.locator('.roster-row').getByText('Debi')).toBeVisible();
    await expect(page.locator('.roster-row')).toHaveCount(2);
  });

  test('opens the goal manage panel to show catalog chips', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    // Buttons in order: Kader, Skills, Ziele, Fokusse.
    await page.getByRole('button', { name: 'Verwalten' }).nth(2).click();

    await expect(page.getByText('Ballverluste im eigenen Drittel reduzieren').first()).toBeVisible();
  });

  test('lets the coach create a new skill from the manage panel', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');

    // Open the Skills manage panel (2nd "Verwalten": Kader, Skills, …).
    await page.getByRole('button', { name: 'Verwalten' }).nth(1).click();

    const createRow = page.locator('.card.skills .create-row');
    await createRow.getByPlaceholder('Neuer Skill').fill('Bully-Kontrolle');
    // Pick a color from the palette, then create.
    await createRow.locator('.swatch').nth(3).click();
    await createRow.getByRole('button', { name: 'Anlegen' }).click();

    await expect(page.locator('.card.skills .chip', { hasText: 'Bully-Kontrolle' })).toBeVisible();
  });
});
