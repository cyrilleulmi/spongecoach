import { Page } from '@playwright/test';

/** The seeded Users, as far as the e2e suites need them (ADR-0017). */
export const USERS = [
  { id: 'u-admin', name: 'Admin', role: 'SYS_ADMIN', playerId: null },
  { id: 'u-cyrille', name: 'Cyrille', role: 'COACH', playerId: null },
  { id: 'u-carmela', name: 'Carmela', role: 'PLAYER', playerId: 'p-carmela' },
];

/**
 * Stubs the User list and `/api/me`. `/api/me` answers as whoever the `spongecoach-user` cookie
 * names, like the backend does, so switching User in the dropdown really switches.
 */
export async function stubUsers(page: Page, lineIdsByUser: Record<string, string[]> = {}) {
  await page.route('**/api/users', (route) => route.fulfill({ json: USERS }));
  await page.route('**/api/me', (route) => {
    const cookie = route.request().headers()['cookie'] ?? '';
    const id = /spongecoach-user=([^;]+)/.exec(cookie)?.[1];
    const user = USERS.find((u) => u.id === id) ?? USERS[0];
    return route.fulfill({
      json: {
        ...user,
        teamId: user.role === 'SYS_ADMIN' ? null : 'team',
        lineIds: lineIdsByUser[user.id] ?? [],
      },
    });
  });
}
