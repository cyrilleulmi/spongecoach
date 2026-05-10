import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ClubApiService, Club } from '../../core/api/club.api';
import { TeamApiService, Team } from '../../core/api/team.api';

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
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  club = signal<Club | null>(null);
  teams = signal<Team[]>([]);
  error = signal<string | null>(null);
  showTeamForm = signal(false);
  editingTeamId = signal<string | null>(null);

  teamForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
  });

  constructor() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.clubApi.get(id).subscribe({
      next: club => this.club.set(club),
      error: () => this.error.set('Club not found'),
    });
    this.teamApi.list(id).subscribe({
      next: teams => this.teams.set(teams),
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
    const clubId = this.club()!.id;

    const request$ = id
      ? this.teamApi.update(id, name)
      : this.teamApi.create({ name, clubId });

    request$.subscribe({
      next: () => {
        this.cancelTeamForm();
        this.teamApi.list(clubId).subscribe(teams => this.teams.set(teams));
      },
      error: () => this.error.set('Failed to save team'),
    });
  }

  deleteTeam(id: string): void {
    if (!confirm('Delete this team?')) return;
    this.teamApi.delete(id).subscribe({
      next: () => this.teamApi.list(this.club()!.id).subscribe(teams => this.teams.set(teams)),
      error: () => this.error.set('Failed to delete team'),
    });
  }
}
