import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrentUserService } from '../../auth/current-user.service';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { DrillApiService } from '../drill-api.service';
import { DrillStatus, DrillSummary, DrillTag } from '../drill.model';

export const STATUS_LABELS: Record<DrillStatus, string> = {
  PENDING: 'Wird gelesen …',
  NEEDS_INPUT: 'Rückfrage',
  READY: 'Bereit',
  FAILED: 'Fehler',
};

/** Every Drill of the Team, most recently changed first, filterable by tag (ADR-0018). */
@Component({
  selector: 'app-drill-list',
  imports: [RouterLink, ThemeToggle],
  templateUrl: './drill-list.html',
  styleUrl: './drill-list.scss',
})
export class DrillList implements OnInit {
  private readonly api = inject(DrillApiService);
  protected readonly currentUser = inject(CurrentUserService);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly drills = signal<DrillSummary[]>([]);
  protected readonly tags = signal<DrillTag[]>([]);
  protected readonly selectedTags = signal<ReadonlySet<string>>(new Set());
  protected readonly loaded = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  /** Drills carrying every selected tag. */
  protected readonly visible = computed(() => {
    const selected = this.selectedTags();
    return this.drills().filter((drill) => [...selected].every((id) => drill.tags.some((tag) => tag.id === id)));
  });

  ngOnInit(): void {
    this.api.listDrills().subscribe({
      next: (drills) => {
        this.drills.set(drills);
        this.loaded.set(true);
      },
      error: () => this.errorMessage.set('Übungen konnten nicht geladen werden. Läuft das Backend?'),
    });
    this.api.listTags().subscribe({ next: (tags) => this.tags.set(tags), error: () => undefined });
  }

  protected toggleTag(tagId: string): void {
    const next = new Set(this.selectedTags());
    if (next.has(tagId)) next.delete(tagId);
    else next.add(tagId);
    this.selectedTags.set(next);
  }
}
