import { test, expect } from '@playwright/test';

test('the app shell loads', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle(/Frontend/i);
});
