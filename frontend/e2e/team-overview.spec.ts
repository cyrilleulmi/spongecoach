import { test, expect, Page } from '@playwright/test';

const LINE_A = '11111111-1111-1111-1111-111111111111';
const LINE_B = '22222222-2222-2222-2222-222222222222';
const TRAINING_TYPE = '80000000-0000-0000-0000-000000000001';
const MATCH_TYPE = '80000000-0000-0000-0000-000000000002';
const EV_1 = 'aaaaaaaa-0000-0000-0000-000000000001';
const EV_2 = 'aaaaaaaa-0000-0000-0000-000000000002';
const PLAYER_1 = 'cccccccc-0000-0000-0000-000000000001';
const PLAYER_2 = 'cccccccc-0000-0000-0000-000000000002';

// Far enough out that both events stay upcoming — and so editable (ADR-0012).
const EV_1_AT = '2099-03-10T18:00:00';
const EV_2_AT = '2099-03-14T19:30:00';

const ATTENDING_LINES = [
  { id: LINE_A, name: 'Kiwi', color: '#4c8c3d' },
  { id: LINE_B, name: 'Bäri', color: '#8b5e34' },
];

const ATTENDANCE = [
  { playerId: PLAYER_1, playerName: 'Carmela', lineIds: [LINE_A], status: 'PENDING', declineMessage: null },
  { playerId: PLAYER_2, playerName: 'Rahel', lineIds: [LINE_B], status: 'ATTENDING', declineMessage: null },
];

async function mockApi(page: Page) {
  let ev1Attachments: { lineId: string; lineName: string; focus: string }[] = [
    { lineId: LINE_A, lineName: 'Kiwi', focus: 'Spielaufbau' },
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
          scheduledOn: EV_1_AT,
          focusAttachments: ev1Attachments,
          lines: ATTENDING_LINES,
          attendance: ATTENDANCE,
        },
        {
          id: EV_2,
          typeId: MATCH_TYPE,
          type: 'Match',
          name: 'Testspiel gegen Rotweiss',
          scheduledOn: EV_2_AT,
          focusAttachments: [],
          lines: ATTENDING_LINES,
          attendance: ATTENDANCE,
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
        { id: LINE_A, name: 'Kiwi', playerCount: 4, color: '#4c8c3d' },
        { id: LINE_B, name: 'Bäri', playerCount: 4, color: '#8b5e34' },
      ],
    }),
  );
  await page.route(`**/api/lines/${LINE_A}`, (route) =>
    route.fulfill({
      json: { id: LINE_A, name: 'Kiwi', players: [], skills: [], developmentGoals: [] },
    }),
  );
  await page.route(`**/api/lines/${LINE_B}`, (route) =>
    route.fulfill({
      json: { id: LINE_B, name: 'Bäri', players: [], skills: [], developmentGoals: [] },
    }),
  );

  await page.route(`**/api/events/${EV_1}`, async (route) => {
    const body = route.request().postDataJSON();
    ev1Attachments = (body.focusAttachments as { lineId: string; focus: string }[]).map((a) => ({
      lineId: a.lineId,
      lineName: a.lineId === LINE_A ? 'Kiwi' : 'Bäri',
      focus: a.focus,
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

  test('flags the first not-yet-past event as the next one', async ({ page }) => {
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
    await baeriRow.locator('.focus-input').fill('Cross-Pässe unter Druck');
    await baeriRow.locator('.focus-input').press('Tab');

    // After the PUT + reload, Bäri's field keeps the typed focus text.
    await expect(baeriRow.locator('.focus-input')).toHaveValue('Cross-Pässe unter Druck');
  });

  test('navigates to the timeline from the per-line page via the nav', async ({ page }) => {
    await mockApi(page);
    await page.route('**/api/players', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/skills', (route) => route.fulfill({ json: [] }));
    await page.route('**/api/development-goals', (route) => route.fulfill({ json: [] }));

    await page.goto('/lines');
    await page.getByRole('link', { name: 'Team-Übersicht' }).click();

    await expect(page).toHaveURL(/\/team$/);
    await expect(page.getByRole('heading', { name: 'Saison-Timeline' })).toBeVisible();
  });
});
