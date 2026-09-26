import { test, expect } from '@playwright/test';
import { stubUsers } from './users';

test('the app shell loads', async ({ page }) => {
  await stubUsers(page);
  await page.route('**/api/iterations', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/event-types', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/lines', (route) => route.fulfill({ json: [] }));
  await page.goto('/');

  await expect(page).toHaveTitle(/SpongeCoach/i);
});

// spec: ui.user-switcher
test('switching User in the header reloads the app as that User', async ({ page }) => {
  await stubUsers(page);
  await page.route('**/api/iterations', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/event-types', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/lines', (route) => route.fulfill({ json: [] }));
  await page.goto('/team');

  const switcher = page.getByLabel('Benutzer wechseln');
  // First visit: the SysAdmin is chosen, and planning the timeline is offered.
  await expect(switcher).toHaveValue('u-admin');
  await expect(page.getByRole('button', { name: 'Iteration anlegen' })).toBeVisible();

  await switcher.selectOption('u-carmela');

  await expect(switcher).toHaveValue('u-carmela');
  await expect(page.getByRole('button', { name: 'Iteration anlegen' })).toHaveCount(0);
});
