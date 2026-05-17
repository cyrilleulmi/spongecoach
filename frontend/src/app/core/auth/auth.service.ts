import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface ClubMembership {
  clubId: string;
  personId: string;
  role: 'ADMIN' | 'MEMBER';
}

export interface MockUser {
  personId: string;
  email: string;
  role: 'ADMIN' | 'USER';
  clubMemberships: ClubMembership[];
}

const STORAGE_KEY = 'mockUserId';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly availableUsers = signal<MockUser[]>([]);
  readonly currentUser = signal<MockUser | null>(this.loadFromStorage());

  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  isClubAdmin(clubId: string): boolean {
    return this.currentUser()?.clubMemberships?.some(
      m => m.clubId === clubId && m.role === 'ADMIN'
    ) ?? false;
  }

  loadMockUsers(): void {
    this.http.get<MockUser[]>('/api/auth/mock/users').subscribe(users => {
      this.availableUsers.set(users);
      if (!this.currentUser() && users.length > 0) {
        this.selectUser(users[0]);
      } else if (this.currentUser()) {
        // Refresh current user with full data (including clubMemberships)
        const refreshed = users.find(u => u.email === this.currentUser()!.email);
        if (refreshed) this.currentUser.set(refreshed);
      }
    });
  }

  selectUser(user: MockUser): void {
    this.currentUser.set(user);
    localStorage.setItem(STORAGE_KEY, user.email);
  }

  private loadFromStorage(): MockUser | null {
    const email = localStorage.getItem(STORAGE_KEY);
    return email ? { personId: '', email, role: 'USER', clubMemberships: [] } : null;
  }
}
