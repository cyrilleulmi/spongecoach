import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CatalogRef } from '../../lines/line.model';
import { LineBadge } from '../../lines/line-badge/line-badge';
import { ChipItem, ManageChips } from '../../lines/manage-chips/manage-chips';
import { RatingBar } from '../../lines/rating-bar/rating-bar';
import { ColorPicker } from '../../theme/color-picker/color-picker';
import { CATALOG_PALETTE } from '../../theme/palette';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { PlayerApiService } from '../player-api.service';
import { PlayerDetail } from '../player.model';
import { PlayerAvatar } from '../player-avatar/player-avatar';

type ManageSection = 'skills' | 'goals';

@Component({
  selector: 'app-player-view',
  imports: [RouterLink, LineBadge, ManageChips, RatingBar, ColorPicker, ThemeToggle, PlayerAvatar],
  templateUrl: './player-view.html',
  styleUrl: './player-view.scss',
})
export class PlayerView implements OnInit {
  private readonly api = inject(PlayerApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly detail = signal<PlayerDetail | null>(null);
  protected readonly notFound = signal(false);
  protected readonly skillCatalog = signal<CatalogRef[]>([]);
  protected readonly goalCatalog = signal<CatalogRef[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly openSection = signal<ManageSection | null>(null);

  protected readonly newSkillName = signal('');
  protected readonly newSkillColor = signal<string>(CATALOG_PALETTE[0]);
  protected readonly newGoalName = signal('');
  protected readonly newGoalColor = signal<string>(CATALOG_PALETTE[2]);

  protected readonly associatedSkillIds = computed(
    () => new Set((this.detail()?.skills ?? []).map((s) => s.skillId)),
  );
  protected readonly associatedGoalIds = computed(
    () => new Set((this.detail()?.developmentGoals ?? []).map((g) => g.id)),
  );
  protected readonly skillChipItems = computed<ChipItem[]>(() => this.skillCatalog());
  protected readonly goalChipItems = computed<ChipItem[]>(() => this.goalCatalog());

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => this.load(params.get('id')!));
  }

  private load(playerId: string): void {
    this.detail.set(null);
    this.notFound.set(false);
    this.openSection.set(null);
    forkJoin({
      player: this.api.getPlayer(playerId),
      skills: this.api.listSkillCatalog(),
      goals: this.api.listGoalCatalog(),
    }).subscribe({
      next: ({ player, skills, goals }) => {
        this.detail.set(player);
        this.skillCatalog.set(skills);
        this.goalCatalog.set(goals);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 404) {
          this.notFound.set(true);
        } else {
          this.errorMessage.set('Spieler konnte nicht geladen werden. Läuft das Backend?');
        }
      },
    });
  }

  protected toggleManage(section: ManageSection): void {
    this.openSection.set(this.openSection() === section ? null : section);
  }

  protected levelOf(rating: number): number {
    return Math.max(1, Math.min(5, Math.round((rating / 100) * 5)));
  }

  /** Optimistic, like the line screen: the bar moves on click and only rolls back on error. */
  protected setRating(skillId: string, rating: number): void {
    const current = this.detail();
    if (!current) {
      return;
    }
    const previousRating = current.skills.find((s) => s.skillId === skillId)?.rating;
    this.patchSkills((skills) => skills.map((s) => (s.skillId === skillId ? { ...s, rating } : s)));

    this.api.setSkillRating(current.id, skillId, rating).subscribe({
      error: () => {
        this.errorMessage.set('Bewertung konnte nicht aktualisiert werden.');
        if (previousRating !== undefined) {
          this.patchSkills((skills) =>
            skills.map((s) => (s.skillId === skillId ? { ...s, rating: previousRating } : s)),
          );
        }
      },
    });
  }

  protected toggleSkill(skillId: string): void {
    const current = this.detail();
    if (!current) {
      return;
    }
    if (this.associatedSkillIds().has(skillId)) {
      const removed = current.skills.find((s) => s.skillId === skillId)!;
      this.patchSkills((skills) => skills.filter((s) => s.skillId !== skillId));
      this.api.removeSkill(current.id, skillId).subscribe({
        error: () => {
          this.errorMessage.set('Skill konnte nicht entfernt werden.');
          this.patchSkills((skills) => [...skills, removed]);
        },
      });
    } else {
      const catalogSkill = this.skillCatalog().find((s) => s.id === skillId);
      this.patchSkills((skills) => [
        ...skills,
        { skillId, name: catalogSkill?.name ?? '', color: catalogSkill?.color ?? '#888888', rating: 50 },
      ]);
      this.api.setSkillRating(current.id, skillId, 50).subscribe({
        error: () => {
          this.errorMessage.set('Skill konnte nicht hinzugefügt werden.');
          this.patchSkills((skills) => skills.filter((s) => s.skillId !== skillId));
        },
      });
    }
  }

  protected toggleGoal(goalId: string): void {
    const current = this.detail();
    if (!current) {
      return;
    }
    const ids = new Set(this.associatedGoalIds());
    if (ids.has(goalId)) {
      ids.delete(goalId);
    } else {
      ids.add(goalId);
    }
    this.api.setDevelopmentGoals(current.id, [...ids]).subscribe({
      next: (updated) => this.detail.set(updated),
      error: () => this.errorMessage.set('Ziele konnten nicht aktualisiert werden.'),
    });
  }

  protected createSkill(): void {
    const name = this.newSkillName().trim();
    if (!name) {
      return;
    }
    this.api.createSkill(name, this.newSkillColor()).subscribe({
      next: (created) => {
        this.skillCatalog.set([...this.skillCatalog(), created]);
        this.newSkillName.set('');
        this.toggleSkill(created.id);
      },
      error: () => this.errorMessage.set('Skill konnte nicht angelegt werden.'),
    });
  }

  protected createGoal(): void {
    const name = this.newGoalName().trim();
    if (!name) {
      return;
    }
    this.api.createGoal(name, this.newGoalColor()).subscribe({
      next: (created) => {
        this.goalCatalog.set([...this.goalCatalog(), created]);
        this.newGoalName.set('');
        this.toggleGoal(created.id);
      },
      error: () => this.errorMessage.set('Ziel konnte nicht angelegt werden.'),
    });
  }

  /** Reads `detail` at call time, so a rollback lands on top of any later optimistic edits. */
  private patchSkills(change: (skills: PlayerDetail['skills']) => PlayerDetail['skills']): void {
    const current = this.detail();
    if (current) {
      this.detail.set({ ...current, skills: change(current.skills) });
    }
  }
}
