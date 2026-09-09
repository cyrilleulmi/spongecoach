import { test, expect, Page } from '@playwright/test';

const LINE_A = '11111111-1111-1111-1111-111111111111';
const LINE_B = '22222222-2222-2222-2222-222222222222';
const TRAINING_TYPE = '80000000-0000-0000-0000-000000000001';
const MATCH_TYPE = '80000000-0000-0000-0000-000000000002';
const EV_1 = 'aaaaaaaa-0000-0000-0000-000000000001';
const EV_2 = 'aaaaaaaa-0000-0000-0000-000000000002';
const FOCUS_A1 = 'ffffffff-0000-0000-0000-0000000000a1';
const FOCUS_B1 = 'ffffffff-0000-0000-0000-0000000000b1';

async function mockApi(page: Page) {
  // ev-1 (Training 1): only Line A has a focus -> incomplete -> "next".
  let ev1Attachments: { lineId: string; lineName: string; focusId: string; focusName: string }[] = [
    { lineId: LINE_A, lineName: 'Kiwi', focusId: FOCUS_A1, focusName: 'Spielaufbau' },
  ];

  const iterations = () => [
    {
      id: 'it-1',
      name: 'Vorbereitung',
      position: 1,
      events: [
        {
          id: EV_1,
          typeId: TRAINING_TYPE,
          type: 'Training',
          name: null,
          position: 1,
          scheduledOn: null,
          focusAttachments: ev1Attachments,
        },
        {
          id: EV_2,
          typeId: MATCH_TYPE,
          type: 'Match',
          name: 'Testspiel gegen Rotweiss',
          position: 2,
          scheduledOn: '2026-09-12',
          focusAttachments: [],
        },
      ],
    },
  ];

  await page.route('**/api/iterations', (route) => route.fulfill({ json: iterations() }));
  await page.route('**/api/event-types', (route) =>
    route.fulfill({
      json: [
        { id: TRAINING_TYPE, name: 'Training' },
        { id: MATCH_TYPE, name: 'Match' },
      ],
    }),
  );
  await page.route('**/api/lines', (route) =>
    route.fulfill({
      json: [
        { id: LINE_A, name: 'Kiwi', playerCount: 4 },
        { id: LINE_B, name: 'Bäri', playerCount: 4 },
      ],
    }),
  );
  await page.route(`**/api/lines/${LINE_A}`, (route) =>
    route.fulfill({
      json: {
        id: LINE_A,
        name: 'Kiwi',
        players: [],
        skills: [],
        developmentGoals: [],
        focuses: [{ id: FOCUS_A1, name: 'Spielaufbau', goalIds: [] }],
      },
    }),
  );
  await page.route(`**/api/lines/${LINE_B}`, (route) =>
    route.fulfill({
      json: {
        id: LINE_B,
        name: 'Bäri',
        players: [],
        skills: [],
        developmentGoals: [],
        focuses: [{ id: FOCUS_B1, name: 'Cross-Pässe unter Druck', goalIds: [] }],
      },
    }),
  );

  await page.route(`**/api/events/${EV_1}`, async (route) => {
    const body = route.request().postDataJSON();
    ev1Attachments = (body.focusAttachments as { lineId: string; focusId: string }[]).map((a) => ({
      lineId: a.lineId,
      lineName: a.lineId === LINE_A ? 'Kiwi' : 'Bäri',
      focusId: a.focusId,
      focusName: a.focusId === FOCUS_B1 ? 'Cross-Pässe unter Druck' : 'Spielaufbau',
    }));
    await route.fulfill({ json: iterations()[0].events[0] });
  });
}

test.describe('Team overview timeline', () => {
  test('shows one iteration with numbered trainings closing on a named match', async ({ page }) => {
    await mockApi(page);
    await page.goto('/team');

    await expect(page.getByText('Vorbereitung')).toBeVisible();
    await expect(page.getByText('Iteration 1 von 1')).toBeVisible();
    await expect(page.locator('.d-node')).toHaveCount(2);
    await expect(page.locator('.d-node').first()).toContainText('Training 1');
    await expect(page.locator('.d-node').nth(1)).toContainText('Testspiel gegen Rotweiss');
  });

  test('flags the first incomplete event as the next one', async ({ page }) => {
    await mockApi(page);
    await page.goto('/team');

    await expect(page.locator('.d-node.current')).toContainText('NÄCHSTES');
    await expect(page.locator('.d-node.current')).toContainText('Training 1');
  });

  test('lets the coach set a line focus for the selected event', async ({ page }) => {
    await mockApi(page);
    await page.goto('/team');

    // ev-1 is selected by default (it is "next"). Set Bäri's focus.
    const baeriRow = page.locator('.focus-rows li', { hasText: 'Bäri' });
    await baeriRow.locator('select').selectOption({ label: 'Cross-Pässe unter Druck' });

    // After the PUT + reload, Bäri's dropdown keeps the chosen focus.
    await expect(baeriRow.locator('select')).toHaveValue(FOCUS_B1);
  });

  test('navigates to the timeline from the per-line page via the nav', async ({ page }) => {
    await mockApi(page);
    await page.route('**/api/players', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/skills', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/development-goals', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/focuses', (route) => route.fulfill({ json: [] }));

    await page.goto('/lines');
    await page.getByRole('link', { name: 'Team-Übersicht' }).click();

    await expect(page).toHaveURL(/\/team$/);
    await expect(page.getByRole('heading', { name: 'Saison-Timeline' })).toBeVisible();
  });
});
