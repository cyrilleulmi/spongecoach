import { Component, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { forkJoin, of, switchMap, map } from 'rxjs';
import { ClubApiService, Club } from '../../core/api/club.api';
import { TeamApiService, Team } from '../../core/api/team.api';
import { PersonApiService } from '../../core/api/person.api';
import { AuthService } from '../../core/auth/auth.service';

export interface ClubMemberRow {
  clubId: string;
  personId: string;
  role: 'ADMIN' | 'MEMBER';
  firstName: string;
  lastName: string;
  email: string;
}

@Component({
  selector: 'app-club-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './club-detail.component.html',
  styleUrl: './clubs.component.scss',
})
export class ClubDetailComponent {
  private readonly clubApi = inject(ClubApiService);
  private readonly teamApi = inject(TeamApiService);
  private readonly personApi = inject(PersonApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  club = signal<Club | null>(null);
  teams = signal<Team[]>([]);
  members = signal<ClubMemberRow[]>([]);
  error = signal<string | null>(null);
  activeTab = signal<'teams' | 'members'>('teams');
  showTeamForm = signal(false);
  editingTeamId = signal<string | null>(null);
  membersLoading = signal(false);

  readonly isAdminOfClub = computed(() => {
    const clubId = this.club()?.id;
    return clubId ? this.auth.isClubAdmin(clubId) : false;
  });

  readonly clubRoleLabel = computed(() => {
    const clubId = this.club()?.id;
    if (!clubId) return null;
    return this.auth.isClubAdmin(clubId) ? 'ADMIN' : 'MEMBER';
  });

  readonly currentPersonId = computed(() => this.auth.currentUser()?.personId ?? '');

  teamForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
  });

  private readonly clubId = this.route.snapshot.paramMap.get('id')!;

  constructor() {
    effect(() => {
      if (!this.auth.currentUser()) return;
      this.clubApi.get(this.clubId).subscribe({
        next: club => this.club.set(club),
        error: () => this.error.set('Club not found'),
      });
      this.teamApi.listByClub(this.clubId).subscribe({
        next: teams => this.teams.set(teams),
      });
    });
  }

  switchTab(tab: 'teams' | 'members'): void {
    this.activeTab.set(tab);
    if (tab === 'members' && this.members().length === 0) {
      this.loadMembers();
    }
  }

  loadMembers(): void {
    this.membersLoading.set(true);
    this.clubApi.listMembers(this.clubId).pipe(
      switchMap(memberships => {
        if (memberships.length === 0) return of([]);
        return forkJoin(memberships.map(m =>
          this.personApi.get(m.personId).pipe(
            map(person => ({
              clubId: m.clubId,
              personId: m.personId,
              role: m.role,
              firstName: person.firstName,
              lastName: person.lastName,
              email: person.email,
            }) satisfies ClubMemberRow)
          )
        ));
      })
    ).subscribe({
      next: rows => { this.members.set(rows); this.membersLoading.set(false); },
      error: () => { this.error.set('Failed to load members'); this.membersLoading.set(false); },
    });
  }

  toggleRole(member: ClubMemberRow): void {
    const newRole = member.role === 'ADMIN' ? 'MEMBER' : 'ADMIN';
    const label = `${member.firstName} ${member.lastName}`;
    if (!confirm(`Change ${label}'s role to ${newRole}?`)) return;
    this.clubApi.updateMemberRole(this.clubId, member.personId, newRole).subscribe({
      next: () => this.loadMembers(),
      error: () => this.error.set('Failed to update role'),
    });
  }

  removeMember(member: ClubMemberRow): void {
    const label = `${member.firstName} ${member.lastName}`;
    const teamWarning = this.teams().length > 0
      ? '\n\nThey will also be removed from all teams in this club.'
      : '';
    if (!confirm(`Remove ${label} from the club?${teamWarning}`)) return;
    this.clubApi.removeMember(this.clubId, member.personId).subscribe({
      next: () => this.loadMembers(),
      error: () => this.error.set('Failed to remove member'),
    });
  }

  openCreateTeam(): void {
    this.editingTeamId.set(null);
    this.teamForm.reset();
    this.showTeamForm.set(true);
  }

  openEditTeam(team: Team): void {
    this.editingTeamId.set(team.id);
    this.teamForm.setValue({ name: team.name });
    this.showTeamForm.set(true);
  }

  cancelTeamForm(): void {
    this.showTeamForm.set(false);
    this.editingTeamId.set(null);
    this.teamForm.reset();
  }

  submitTeam(): void {
    if (this.teamForm.invalid || !this.club()) return;
    const { name } = this.teamForm.getRawValue();
    const id = this.editingTeamId();

    const request$ = id
      ? this.teamApi.update(id, name)
      : this.teamApi.create({ name, clubId: this.clubId });

    request$.subscribe({
      next: () => {
        this.cancelTeamForm();
        this.teamApi.listByClub(this.clubId).subscribe(teams => this.teams.set(teams));
      },
      error: () => this.error.set('Failed to save team'),
    });
  }

  deleteTeam(id: string): void {
    const team = this.teams().find(t => t.id === id);
    if (!confirm(`Delete team "${team?.name ?? ''}"? This cannot be undone.`)) return;
    this.teamApi.delete(id).subscribe({
      next: () => this.teamApi.listByClub(this.clubId).subscribe(teams => this.teams.set(teams)),
      error: () => this.error.set('Failed to delete team'),
    });
  }
}
