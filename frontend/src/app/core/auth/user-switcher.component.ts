import { Component, OnInit, inject, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService, MockUser } from './auth.service';

@Component({
  selector: 'app-user-switcher',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="user-switcher">
      <button class="user-btn" (click)="toggle()">
        <span class="user-name">{{ currentUser()?.email ?? 'Select user' }}</span>
        <span class="role-badge" [class]="'role-' + (currentUser()?.role?.toLowerCase() ?? '')">
          {{ currentUser()?.role ?? '—' }}
        </span>
        <span class="chevron">▾</span>
      </button>

      @if (open()) {
        <div class="dropdown">
          @for (user of availableUsers(); track user.email) {
            <button
              class="dropdown-item"
              [class.active]="user.email === currentUser()?.email"
              (click)="select(user)">
              <span class="item-email">{{ user.email }}</span>
              <span class="role-badge" [class]="'role-' + (user.role?.toLowerCase() ?? 'user')">{{ user.role }}</span>
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .user-switcher { position: relative; margin-left: auto; }

    .user-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px 12px;
      background: none;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      cursor: pointer;
      font-size: 14px;
      color: var(--color-text);
    }
    .user-btn:hover { background: var(--color-surface-hover, #f3f4f6); }

    .role-badge {
      font-size: 11px;
      font-weight: 600;
      padding: 2px 6px;
      border-radius: 9999px;
      text-transform: uppercase;
    }
    .role-admin { background: #fef3c7; color: #92400e; }
    .role-user  { background: #dbeafe; color: #1e40af; }

    .chevron { font-size: 10px; color: var(--color-text-muted); }

    .dropdown {
      position: absolute;
      right: 0;
      top: calc(100% + 6px);
      min-width: 280px;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md, 6px);
      box-shadow: 0 4px 12px rgba(0,0,0,.12);
      z-index: 100;
      overflow: hidden;
    }

    .dropdown-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      padding: 10px 16px;
      background: none;
      border: none;
      cursor: pointer;
      text-align: left;
      font-size: 13px;
      color: var(--color-text);
      gap: 8px;
    }
    .dropdown-item:hover { background: var(--color-surface-hover, #f3f4f6); }
    .dropdown-item.active { background: #eff6ff; }

    .item-email { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `]
})
export class UserSwitcherComponent implements OnInit {
  private readonly auth = inject(AuthService);
  protected readonly open = signal(false);
  protected readonly currentUser = this.auth.currentUser;
  protected readonly availableUsers = this.auth.availableUsers;

  ngOnInit(): void {
    this.auth.loadMockUsers();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('app-user-switcher')) {
      this.open.set(false);
    }
  }

  protected toggle(): void {
    this.open.update(v => !v);
  }

  protected select(user: MockUser): void {
    this.auth.selectUser(user);
    this.open.set(false);
  }
}
