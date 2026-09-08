import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ChipItem, ManageChips } from '../manage-chips/manage-chips';
import { RatingBar } from '../rating-bar/rating-bar';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { ColorPicker } from '../../theme/color-picker/color-picker';
import { CATALOG_PALETTE } from '../../theme/palette';
import { LineApiService } from '../line-api.service';
import { CatalogRef, FocusRef, LineDetail, LineSummary, Player } from '../line.model';

/** Neutral chip color for players, which — unlike Skills/Goals — carry no color of their own. */
const PLAYER_CHIP_COLOR = '#7a828c';

type ManageSection = 'skills' | 'goals' | 'focuses';

@Component({
  selector: 'app-line-overview',
  imports: [RatingBar, ManageChips, ThemeToggle, ColorPicker],
  templateUrl: './line-overview.html',
  styleUrl: './line-overview.scss',
})
export class LineOverview implements OnInit {
  private readonly api = inject(LineApiService);

  protected readonly lines = signal<LineSummary[]>([]);
  protected readonly selectedLineId = signal<string | null>(null);
  protected readonly detail = signal<LineDetail | null>(null);
  protected readonly playerCatalog = signal<Player[]>([]);
  protected readonly skillCatalog = signal<CatalogRef[]>([]);
  protected readonly goalCatalog = signal<CatalogRef[]>([]);
  protected readonly focusCatalog = signal<FocusRef[]>([]);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly openSection = signal<ManageSection | null>(null);

  /** Skill or goal id whose color swatches are currently open (their UUIDs never collide). */
  protected readonly editingColorId = signal<string | null>(null);
  protected readonly palette = CATALOG_PALETTE;

  protected readonly newSkillName = signal('');
  protected readonly newSkillColor = signal<string>(CATALOG_PALETTE[0]);
  protected readonly newGoalName = signal('');
  protected readonly newGoalColor = signal<string>(CATALOG_PALETTE[2]);
  protected readonly newFocusName = signal('');
  protected readonly newFocusGoalIds = signal<string[]>([]);
  protected readonly newFocusGoalIdSet = computed(() => new Set(this.newFocusGoalIds()));

  private readonly rosterDialog = viewChild<ElementRef<HTMLDialogElement>>('rosterDialog');

  protected readonly associatedPlayerIds = computed(
    () => new Set((this.detail()?.players ?? []).map((p) => p.id)),
  );
  protected readonly associatedSkillIds = computed(
    () => new Set((this.detail()?.skills ?? []).map((s) => s.skillId)),
  );
  protected readonly associatedGoalIds = computed(
    () => new Set((this.detail()?.developmentGoals ?? []).map((g) => g.id)),
  );
  protected readonly associatedFocusIds = computed(
    () => new Set((this.detail()?.focuses ?? []).map((f) => f.id)),
  );

  protected readonly playerChipItems = computed<ChipItem[]>(() =>
    this.playerCatalog().map((p) => ({ id: p.id, name: p.name, color: PLAYER_CHIP_COLOR })),
  );
  protected readonly skillChipItems = computed<ChipItem[]>(() =>
    this.skillCatalog().map((s) => ({ id: s.id, name: s.name, color: s.color })),
  );
  protected readonly goalChipItems = computed<ChipItem[]>(() =>
    this.goalCatalog().map((g) => ({ id: g.id, name: g.name, color: g.color })),
  );
  protected readonly focusChipItems = computed<ChipItem[]>(() =>
    this.focusCatalog().map((f) => ({
      id: f.id,
      name: f.name,
      color: this.goalColorFor(f) ?? '#888888',
      tooltip: this.goalNameFor(f) ?? undefined,
    })),
  );

  ngOnInit(): void {
    forkJoin({
      lines: this.api.listLines(),
      players: this.api.listPlayerCatalog(),
      skills: this.api.listSkillCatalog(),
      goals: this.api.listGoalCatalog(),
      focuses: this.api.listFocusCatalog(),
    }).subscribe({
      next: ({ lines, players, skills, goals, focuses }) => {
        this.lines.set(lines);
        this.playerCatalog.set(players);
        this.skillCatalog.set(skills);
        this.goalCatalog.set(goals);
        this.focusCatalog.set(focuses);
        if (lines.length > 0) {
          this.selectLine(lines[0].id);
        }
      },
      error: () => this.errorMessage.set('Blöcke konnten nicht geladen werden. Läuft das Backend?'),
    });
  }

  protected selectLine(lineId: string): void {
    // Only collapse the open panels when actually switching line — not on the
    // refetch that follows every association/creation on the current line.
    if (this.selectedLineId() !== lineId) {
      this.openSection.set(null);
      this.editingColorId.set(null);
    }
    this.selectedLineId.set(lineId);
    this.api.getLine(lineId).subscribe({
      next: (detail) => this.detail.set(detail),
      error: () => this.errorMessage.set('Dieser Block konnte nicht geladen werden.'),
    });
  }

  protected toggleManage(section: ManageSection): void {
    this.openSection.set(this.openSection() === section ? null : section);
  }

  protected levelOf(rating: number): number {
    return Math.max(1, Math.min(5, Math.round((rating / 100) * 5)));
  }

  protected initials(name: string): string {
    return name
      .split(' ')
      .filter(Boolean)
      .map((part) => part[0])
      .join('');
  }

  /** Every catalog goal this focus is derived from, in catalog order. */
  protected goalTagsFor(focus: FocusRef): CatalogRef[] {
    return this.goalCatalog().filter((g) => focus.goalIds.includes(g.id));
  }

  protected goalNameFor(focus: FocusRef): string | null {
    return this.goalTagsFor(focus).map((g) => g.name).join(', ') || null;
  }

  protected goalColorFor(focus: FocusRef): string | null {
    return this.goalTagsFor(focus)[0]?.color ?? null;
  }

  protected openRosterDialog(): void {
    this.rosterDialog()?.nativeElement.showModal();
  }

  protected closeRosterDialog(): void {
    this.rosterDialog()?.nativeElement.close();
  }

  protected togglePlayer(playerId: string): void {
    const lineId = this.selectedLineId();
    if (!lineId) {
      return;
    }
    const ids = this.toggledIds(this.associatedPlayerIds(), playerId);
    this.api.updateLineAssociations(lineId, { playerIds: ids }).subscribe({
      next: () => this.selectLine(lineId),
      error: () => this.errorMessage.set('Kader konnte nicht aktualisiert werden.'),
    });
  }

  protected setRating(skillId: string, rating: number): void {
    const lineId = this.selectedLineId();
    if (!lineId) {
      return;
    }
    this.api.setSkillRating(lineId, skillId, rating).subscribe({
      next: () => this.selectLine(lineId),
      error: () => this.errorMessage.set('Bewertung konnte nicht aktualisiert werden.'),
    });
  }

  protected toggleSkill(skillId: string): void {
    const lineId = this.selectedLineId();
    if (!lineId) {
      return;
    }
    if (this.associatedSkillIds().has(skillId)) {
      this.api.removeSkill(lineId, skillId).subscribe({
        next: () => this.selectLine(lineId),
        error: () => this.errorMessage.set('Skill konnte nicht entfernt werden.'),
      });
    } else {
      this.api.setSkillRating(lineId, skillId, 50).subscribe({
        next: () => this.selectLine(lineId),
        error: () => this.errorMessage.set('Skill konnte nicht hinzugefügt werden.'),
      });
    }
  }

  protected toggleGoal(goalId: string): void {
    const lineId = this.selectedLineId();
    if (!lineId) {
      return;
    }
    const ids = this.toggledIds(this.associatedGoalIds(), goalId);
    this.api.updateLineAssociations(lineId, { developmentGoalIds: ids }).subscribe({
      next: () => this.selectLine(lineId),
      error: () => this.errorMessage.set('Ziele konnten nicht aktualisiert werden.'),
    });
  }

  protected toggleFocus(focusId: string): void {
    const lineId = this.selectedLineId();
    if (!lineId) {
      return;
    }
    const ids = this.toggledIds(this.associatedFocusIds(), focusId);
    this.api.updateLineAssociations(lineId, { focusIds: ids }).subscribe({
      next: () => this.selectLine(lineId),
      error: () => this.errorMessage.set('Fokusse konnten nicht aktualisiert werden.'),
    });
  }

  protected toggleColorEdit(id: string): void {
    this.editingColorId.set(this.editingColorId() === id ? null : id);
  }

  protected changeSkillColor(skillId: string, color: string): void {
    this.editingColorId.set(null);
    this.api.setSkillColor(skillId, color).subscribe({
      next: (updated) => {
        this.skillCatalog.set(this.skillCatalog().map((s) => (s.id === skillId ? updated : s)));
        this.selectLine(this.selectedLineId()!);
      },
      error: () => this.errorMessage.set('Farbe konnte nicht aktualisiert werden.'),
    });
  }

  protected changeGoalColor(goalId: string, color: string): void {
    this.editingColorId.set(null);
    this.api.setGoalColor(goalId, color).subscribe({
      next: (updated) => {
        this.goalCatalog.set(this.goalCatalog().map((g) => (g.id === goalId ? updated : g)));
        this.selectLine(this.selectedLineId()!);
      },
      error: () => this.errorMessage.set('Farbe konnte nicht aktualisiert werden.'),
    });
  }

  protected createSkill(): void {
    const lineId = this.selectedLineId();
    const name = this.newSkillName().trim();
    if (!lineId || !name) {
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
    const lineId = this.selectedLineId();
    const name = this.newGoalName().trim();
    if (!lineId || !name) {
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

  protected toggleNewFocusGoal(goalId: string): void {
    this.newFocusGoalIds.set(this.toggledIds(this.newFocusGoalIdSet(), goalId));
  }

  protected createFocus(): void {
    const lineId = this.selectedLineId();
    const name = this.newFocusName().trim();
    const goalIds = this.newFocusGoalIds();
    if (!lineId || !name || goalIds.length === 0) {
      return;
    }
    this.api.createFocus(name, goalIds).subscribe({
      next: (created) => {
        this.focusCatalog.set([...this.focusCatalog(), created]);
        this.newFocusName.set('');
        this.newFocusGoalIds.set([]);
        this.toggleFocus(created.id);
      },
      error: () => this.errorMessage.set('Fokus konnte nicht angelegt werden.'),
    });
  }

  private toggledIds(current: ReadonlySet<string>, id: string): string[] {
    const next = new Set(current);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    return [...next];
  }
}
