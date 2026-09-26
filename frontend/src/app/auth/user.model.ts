/** What a User may do (ADR-0017). */
export type Role = 'SYS_ADMIN' | 'COACH' | 'PLAYER';

/** A seeded User, as the user dropdown lists them. */
export interface AppUser {
  id: string;
  name: string;
  role: Role;
  playerId: string | null;
}

/** The User the app acts as, with the active Lines their Player is on. */
export interface CurrentUser {
  id: string;
  name: string;
  role: Role;
  teamId: string | null;
  playerId: string | null;
  lineIds: string[];
}
