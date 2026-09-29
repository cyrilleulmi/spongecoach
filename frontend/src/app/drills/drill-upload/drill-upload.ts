import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CurrentUserService } from '../../auth/current-user.service';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { DrillApiService } from '../drill-api.service';
import { DrillTag, SketchRelation } from '../drill.model';
import { normaliseTurns, prepareSketch } from '../image-prep';

/** Up to this many photos per Drill (ADR-0018); the backend refuses more. */
export const MAX_SKETCHES = 12;

interface PendingSketch {
  key: number;
  file: File;
  previewUrl: string;
  /** Quarter turns clockwise the coach added on top of the photo's own orientation. */
  turns: number;
  note: string;
}

/** How coaches usually draw; shown so the drawing and the interpreter speak the same language. */
export const LEGEND: { symbol: string; meaning: string }[] = [
  { symbol: 'X', meaning: 'Spielerin; mehrere X übereinander: Warteschlange' },
  { symbol: 'G im Kreis', meaning: 'Goalie' },
  { symbol: 'O', meaning: 'oft Gegenspielerin (bei Unklarheit wird nachgefragt)' },
  { symbol: '▢ / •', meaning: 'Hütchen' },
  { symbol: '───▶', meaning: 'Laufweg (mit oder ohne Ball)' },
  { symbol: '∿∿∿▶', meaning: 'Dribbling (Ball aktiv am Stock führen)' },
  { symbol: '- - -▶', meaning: 'Pass (Pfeil an beiden Enden: Doppelpass)' },
  { symbol: '═══▶', meaning: 'Schuss' },
  { symbol: '↶ ↷', meaning: 'Bewegung um die Position, nicht genau festgelegt' },
  { symbol: '1, 2, 3', meaning: 'Reihenfolge' },
];

/**
 * Uploads a Drill: photos of the tactic board, a note per photo, tags and how the photos relate
 * (ADR-0018). Photos are made upright and downscaled here, before upload. Coach only.
 */
@Component({
  selector: 'app-drill-upload',
  imports: [FormsModule, RouterLink, ThemeToggle],
  templateUrl: './drill-upload.html',
  styleUrl: './drill-upload.scss',
})
export class DrillUpload implements OnInit, OnDestroy {
  private readonly api = inject(DrillApiService);
  private readonly router = inject(Router);
  protected readonly currentUser = inject(CurrentUserService);

  protected readonly legend = LEGEND;
  protected readonly maxSketches = MAX_SKETCHES;
  protected readonly name = signal('');
  protected readonly sketches = signal<PendingSketch[]>([]);
  protected readonly tags = signal<DrillTag[]>([]);
  protected readonly selectedTags = signal<ReadonlySet<string>>(new Set());
  protected readonly relation = signal<SketchRelation>('PROGRESSION');
  protected readonly uploading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly canUpload = computed(
    () => this.name().trim() !== '' && this.sketches().length > 0 && !this.uploading(),
  );

  private nextKey = 1;

  ngOnInit(): void {
    this.api.listTags().subscribe({ next: (tags) => this.tags.set(tags), error: () => undefined });
  }

  ngOnDestroy(): void {
    this.sketches().forEach((sketch) => this.revoke(sketch.previewUrl));
  }

  protected addFiles(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []).filter((file) => file.type.startsWith('image/'));
    const room = MAX_SKETCHES - this.sketches().length;
    if (files.length > room) {
      this.errorMessage.set(`Höchstens ${MAX_SKETCHES} Fotos pro Übung.`);
    }
    const added = files.slice(0, Math.max(0, room)).map((file) => ({
      key: this.nextKey++,
      file,
      previewUrl: this.objectUrl(file),
      turns: 0,
      note: '',
    }));
    this.sketches.update((list) => [...list, ...added]);
    input.value = '';
  }

  protected turn(key: number): void {
    this.update(key, (sketch) => ({ ...sketch, turns: normaliseTurns(sketch.turns + 1) }));
  }

  protected setNote(key: number, note: string): void {
    this.update(key, (sketch) => ({ ...sketch, note }));
  }

  protected move(key: number, by: -1 | 1): void {
    const list = [...this.sketches()];
    const from = list.findIndex((sketch) => sketch.key === key);
    const to = from + by;
    if (from < 0 || to < 0 || to >= list.length) return;
    [list[from], list[to]] = [list[to], list[from]];
    this.sketches.set(list);
  }

  protected remove(key: number): void {
    const sketch = this.sketches().find((s) => s.key === key);
    if (sketch) this.revoke(sketch.previewUrl);
    this.sketches.update((list) => list.filter((s) => s.key !== key));
  }

  protected toggleTag(tagId: string): void {
    const next = new Set(this.selectedTags());
    if (next.has(tagId)) next.delete(tagId);
    else next.add(tagId);
    this.selectedTags.set(next);
  }

  protected async upload(): Promise<void> {
    if (!this.canUpload()) return;
    this.uploading.set(true);
    this.errorMessage.set(null);
    let prepared;
    try {
      prepared = await Promise.all(
        this.sketches().map(async (sketch) => ({ jpeg: await prepareSketch(sketch.file, sketch.turns), note: sketch.note.trim() })),
      );
    } catch {
      this.errorMessage.set('Ein Foto konnte nicht gelesen werden.');
      this.uploading.set(false);
      return;
    }
    const relation = this.sketches().length > 1 ? this.relation() : 'MIXED';
    this.api.upload(this.name().trim(), prepared, [...this.selectedTags()], relation).subscribe({
      next: (drill) => this.router.navigate(['/uebungen', drill.id]),
      error: () => {
        this.errorMessage.set('Die Übung konnte nicht hochgeladen werden.');
        this.uploading.set(false);
      },
    });
  }

  private update(key: number, change: (sketch: PendingSketch) => PendingSketch): void {
    this.sketches.update((list) => list.map((sketch) => (sketch.key === key ? change(sketch) : sketch)));
  }

  private objectUrl(file: File): string {
    return typeof URL.createObjectURL === 'function' ? URL.createObjectURL(file) : '';
  }

  private revoke(url: string): void {
    if (url && typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(url);
  }
}
