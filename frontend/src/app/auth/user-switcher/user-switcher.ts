import { Component, computed, inject } from '@angular/core';
import { CurrentUserService } from '../current-user.service';
import { AppUser, Role } from '../user.model';

const GROUPS: { role: Role; label: string }[] = [
  { role: 'SYS_ADMIN', label: 'Admin' },
  { role: 'COACH', label: 'Trainer' },
  { role: 'PLAYER', label: 'Spieler' },
];

/** The header dropdown that picks who the app acts as — in place of a login for now (ADR-0017). */
@Component({
  selector: 'app-user-switcher',
  templateUrl: './user-switcher.html',
  styleUrl: './user-switcher.scss',
})
export class UserSwitcher {
  protected readonly currentUser = inject(CurrentUserService);

  protected readonly groups = computed<{ label: string; users: AppUser[] }[]>(() =>
    GROUPS.map(({ role, label }) => ({
      label,
      users: this.currentUser
        .users()
        .filter((u) => u.role === role)
        .sort((a, b) => a.name.localeCompare(b.name, 'de')),
    })).filter((group) => group.users.length > 0),
  );

  protected onSelect(userId: string): void {
    if (userId !== this.currentUser.me()?.id) {
      this.currentUser.switchTo(userId);
    }
  }
}
