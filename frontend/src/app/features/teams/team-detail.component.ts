import { Component, inject, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin, of, switchMap, map } from 'rxjs';
import { ClubApiService, Club } from '../../core/api/club.api';
import { TeamApiService, Team, TeamMember } from '../../core/api/team.api';
import { PersonApiService } from '../../core/api/person.api';
import { AuthService } from '../../core/auth/auth.service';

interface ClubMemberOption {
  personId: string;
  firstName: string;
  lastName: string;
}

@Component({
  selector: 'app-team-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './team-detail.component.html',
  styleUrl: './team-detail.component.scss',
})
export class TeamDetailComponent {
  private readonly teamApi = inject(TeamApiService);
  private readonly clubApi = inject(ClubApiService);
  private readonly personApi = inject(PersonApiService);
  private readonly route = inject(ActivatedRoute);
  protected readonly auth = inject(AuthService);

  team = signal<Team | null>(null);
  club = signal<Club | null>(null);
  members = signal<TeamMember[]>([]);
  clubMembers = signal<ClubMemberOption[]>([]);
  error = signal<string | null>(null);
  selectedPersonId = signal<string>('');

  readonly isAdminOfClub = computed(() => {
    const clubId = this.team()?.clubId;
    return clubId ? this.auth.isClubAdmin(clubId) : false;
  });

  readonly addablePersons = computed(() => {
    const memberIds = new Set(this.members().map(m => m.personId));
    return this.clubMembers().filter(p => !memberIds.has(p.personId));
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id')!;
    effect(() => {
      if (!this.auth.currentUser()) return;
      this.loadTeam(id);
    });
  }

  private loadTeam(id: string): void {
    this.teamApi.get(id).subscribe({
      next: team => {
        this.team.set(team);
        this.clubApi.get(team.clubId).subscribe(club => this.club.set(club));
        this.loadMembers(id);
        if (this.auth.isClubAdmin(team.clubId)) {
          this.loadClubMembers(team.clubId);
        }
      },
      error: () => this.error.set('Team not found'),
    });
  }

  private loadMembers(teamId: string): void {
    this.teamApi.listMembers(teamId).subscribe({
      next: members => this.members.set(members),
      error: () => this.error.set('Failed to load players'),
    });
  }

  private loadClubMembers(clubId: string): void {
    this.clubApi.listMembers(clubId).pipe(
      switchMap(memberships => {
        if (memberships.length === 0) return of([]);
        return forkJoin(memberships.map(m =>
          this.personApi.get(m.personId).pipe(
            map(person => ({ personId: m.personId, firstName: person.firstName, lastName: person.lastName }) satisfies ClubMemberOption)
          )
        ));
      })
    ).subscribe({
      next: options => this.clubMembers.set(options),
      error: () => this.error.set('Failed to load club members'),
    });
  }

  addMember(): void {
    const personId = this.selectedPersonId();
    const teamId = this.team()?.id;
    if (!personId || !teamId) return;

    this.teamApi.addMember(teamId, personId).subscribe({
      next: () => {
        this.selectedPersonId.set('');
        this.loadMembers(teamId);
      },
      error: () => this.error.set('Failed to add member'),
    });
  }

  removeMember(personId: string): void {
    const teamId = this.team()?.id;
    if (!teamId) return;
    if (!confirm('Remove this member from the team?')) return;
    this.teamApi.removeMember(teamId, personId).subscribe({
      next: () => this.loadMembers(teamId),
      error: () => this.error.set('Failed to remove member'),
    });
  }
}
