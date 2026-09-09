import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
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
  protected readonly newLineName = signal('');
  protected readonly menuOpen = signal(false);
  protected readonly deletedLines = signal<LineSummary[]>([]);

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
  private readonly createLineDialog = viewChild<ElementRef<HTMLDialogElement>>('createLineDialog');
  private readonly deleteLineDialog = viewChild<ElementRef<HTMLDialogElement>>('deleteLineDialog');
  private readonly restoreLineDialog = viewChild<ElementRef<HTMLDialogElement>>('restoreLineDialog');

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

  @HostListener('document:click')
  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected toggleMenu(event: Event): void {
    event.stopPropagation();
    this.menuOpen.set(!this.menuOpen());
  }

  protected openCreateLineDialog(): void {
    this.menuOpen.set(false);
    this.newLineName.set('');
    this.createLineDialog()?.nativeElement.showModal();
  }

  protected closeCreateLineDialog(): void {
    this.createLineDialog()?.nativeElement.close();
  }

  protected addLine(): void {
    const name = this.newLineName().trim();
    if (!name) {
      return;
    }
    this.api.createLine(name).subscribe({
      next: (created) => {
        this.lines.set([...this.lines(), created]);
        this.newLineName.set('');
        this.closeCreateLineDialog();
        this.selectLine(created.id);
      },
      error: () => this.errorMessage.set('Block konnte nicht angelegt werden.'),
    });
  }

  protected openDeleteLineDialog(): void {
    if (!this.selectedLineId()) {
      return;
    }
    this.menuOpen.set(false);
    this.deleteLineDialog()?.nativeElement.showModal();
  }

  protected closeDeleteLineDialog(): void {
    this.deleteLineDialog()?.nativeElement.close();
  }

  protected confirmDeleteLine(): void {
    const lineId = this.selectedLineId();
    if (!lineId) {
      return;
    }
    this.api.deleteLine(lineId).subscribe({
      next: () => {
        this.closeDeleteLineDialog();
        const remaining = this.lines().filter((l) => l.id !== lineId);
        this.lines.set(remaining);
        this.detail.set(null);
        this.selectedLineId.set(null);
        if (remaining.length > 0) {
          this.selectLine(remaining[0].id);
        }
      },
      error: () => this.errorMessage.set('Block konnte nicht gelöscht werden.'),
    });
  }

  protected openRestoreLineDialog(): void {
    this.menuOpen.set(false);
    this.api.listDeletedLines().subscribe({
      next: (lines) => {
        this.deletedLines.set(lines);
        this.restoreLineDialog()?.nativeElement.showModal();
      },
      error: () => this.errorMessage.set('Gelöschte Blöcke konnten nicht geladen werden.'),
    });
  }

  protected closeRestoreLineDialog(): void {
    this.restoreLineDialog()?.nativeElement.close();
  }

  protected restoreLine(lineId: string): void {
    this.api.restoreLine(lineId).subscribe({
      next: (restored) => {
        this.deletedLines.set(this.deletedLines().filter((l) => l.id !== lineId));
        this.lines.set([...this.lines(), restored]);
        this.selectLine(restored.id);
      },
      error: () => this.errorMessage.set('Block konnte nicht wiederhergestellt werden.'),
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

  /**
   * Optimistic: applies the rating to `detail` immediately so the bar reacts on click rather than
   * after a round trip, then fires the PUT in the background and only rolls back on error — no
   * `selectLine()` refetch on the happy path.
   */
  protected setRating(skillId: string, rating: number): void {
    const lineId = this.selectedLineId();
    const current = this.detail();
    if (!lineId || !current) {
      return;
    }
    const previousRating = current.skills.find((s) => s.skillId === skillId)?.rating;
    this.patchDetailSkills(current.skills.map((s) => (s.skillId === skillId ? { ...s, rating } : s)));

    this.api.setSkillRating(lineId, skillId, rating).subscribe({
      error: () => {
        this.errorMessage.set('Bewertung konnte nicht aktualisiert werden.');
        if (previousRating !== undefined) {
          this.revertDetailSkills((skills) =>
            skills.map((s) => (s.skillId === skillId ? { ...s, rating: previousRating } : s)),
          );
        }
      },
    });
  }

  protected toggleSkill(skillId: string): void {
    const lineId = this.selectedLineId();
    const current = this.detail();
    if (!lineId || !current) {
      return;
    }
    if (this.associatedSkillIds().has(skillId)) {
      const removed = current.skills.find((s) => s.skillId === skillId);
      this.patchDetailSkills(current.skills.filter((s) => s.skillId !== skillId));

      this.api.removeSkill(lineId, skillId).subscribe({
        error: () => {
          this.errorMessage.set('Skill konnte nicht entfernt werden.');
          if (removed) {
            this.revertDetailSkills((skills) => [...skills, removed]);
          }
        },
      });
    } else {
      const catalogSkill = this.skillCatalog().find((s) => s.id === skillId);
      this.patchDetailSkills([
        ...current.skills,
        { skillId, name: catalogSkill?.name ?? '', color: catalogSkill?.color ?? '#888888', rating: 50 },
      ]);

      this.api.setSkillRating(lineId, skillId, 50).subscribe({
        error: () => {
          this.errorMessage.set('Skill konnte nicht hinzugefügt werden.');
          this.revertDetailSkills((skills) => skills.filter((s) => s.skillId !== skillId));
        },
      });
    }
  }

  private patchDetailSkills(skills: LineDetail['skills']): void {
    const current = this.detail();
    if (current) {
      this.detail.set({ ...current, skills });
    }
  }

  /** Re-reads `detail` at rollback time, since further optimistic edits may have landed since. */
  private revertDetailSkills(rollback: (skills: LineDetail['skills']) => LineDetail['skills']): void {
    const current = this.detail();
    if (current) {
      this.detail.set({ ...current, skills: rollback(current.skills) });
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
