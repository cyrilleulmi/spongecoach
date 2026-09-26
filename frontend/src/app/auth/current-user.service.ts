import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { AppUser, CurrentUser } from './user.model';

/** The backend reads the User from this cookie (ADR-0017); a cookie so `<img>` requests carry it. */
export const USER_COOKIE = 'spongecoach-user';

function readCookie(): string | null {
  try {
    const match = document.cookie.split('; ').find((c) => c.startsWith(`${USER_COOKIE}=`));
    return match ? decodeURIComponent(match.slice(USER_COOKIE.length + 1)) : null;
  } catch {
    return null;
  }
}

function writeCookie(userId: string): void {
  try {
    document.cookie = `${USER_COOKIE}=${encodeURIComponent(userId)}; path=/; max-age=31536000; SameSite=Lax`;
  } catch {
    /* no document (non-browser test host) */
  }
}

/**
 * Who the app acts as — a stand-in for authentication until it exists (ADR-0017). The User is
 * picked in the header dropdown and kept in a cookie; the first visit picks the SysAdmin.
 *
 * Also answers the UI's "may I edit this?" questions, mirroring the backend's `Access`. The backend
 * is what enforces them; the UI only hides what would be refused. Until a User is loaded every
 * answer is yes — the app shell renders no screen before then, so this only matters for a
 * component tested on its own.
 */
@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly http = inject(HttpClient);

  readonly users = signal<AppUser[]>([]);
  readonly me = signal<CurrentUser | null>(null);
  readonly loadFailed = signal(false);

  /** SysAdmin or Coach: may manage Lines, Iterations and Events, and edit every Player. */
  readonly isCoach = computed(() => {
    const me = this.me();
    return !me || me.role === 'SYS_ADMIN' || me.role === 'COACH';
  });

  /** Lists the Users, settles on one (the stored one, else the SysAdmin), then loads it. */
  init(): void {
    this.http.get<AppUser[]>('/api/users').subscribe({
      next: (users) => {
        this.users.set(users);
        const stored = readCookie();
        const chosen =
          users.find((u) => u.id === stored) ?? users.find((u) => u.role === 'SYS_ADMIN') ?? users[0];
        if (!chosen) {
          this.loadFailed.set(true);
          return;
        }
        if (chosen.id !== stored) {
          writeCookie(chosen.id);
        }
        this.refresh();
      },
      error: () => this.loadFailed.set(true),
    });
  }

  /** Re-reads the current User — their Lines change when a roster they are on is edited. */
  refresh(): void {
    this.http.get<CurrentUser>('/api/me').subscribe({
      next: (me) => this.me.set(me),
      error: () => this.loadFailed.set(true),
    });
  }

  /** Acts as another User from now on. Reloads, so no screen keeps data or controls from before. */
  switchTo(userId: string): void {
    writeCookie(userId);
    this.reloadPage();
  }

  canEditLine(lineId: string | null | undefined): boolean {
    return this.isCoach() || (!!lineId && !!this.me()?.lineIds.includes(lineId));
  }

  /** Editing a Player's profile, and answering attendance for them. */
  canEditPlayer(playerId: string | null | undefined): boolean {
    return this.isCoach() || (!!playerId && this.me()?.playerId === playerId);
  }

  protected reloadPage(): void {
    location.reload();
  }
}
