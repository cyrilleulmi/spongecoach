import { Component, ElementRef, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { blankScript, isDrawn } from '../drill-authoring';
import { DrillApiService } from '../drill-api.service';
import { DrillEditor, ScriptEdit } from '../drill-editor/drill-editor';
import { DrillScript, DrillTag } from '../drill.model';

/** Where the unsaved drawing is kept in the browser, so a phone that reloads the tab loses nothing. */
export const DRAFT_KEY = 'spongecoach.drill-draft';

/** The draft, or null when there is none or it is unreadable; storage can be blocked or empty. */
export function loadDraft(): DrillScript | null {
  try {
    const text = localStorage.getItem(DRAFT_KEY);
    const draft = text ? (JSON.parse(text) as DrillScript) : null;
    return draft && Array.isArray(draft.stages) && draft.stages.length > 0 && isDrawn(draft) ? draft : null;
  } catch {
    return null;
  }
}

function storeDraft(script: DrillScript | null): void {
  try {
    if (script && isDrawn(script)) localStorage.setItem(DRAFT_KEY, JSON.stringify(script));
    else localStorage.removeItem(DRAFT_KEY);
  } catch {
    /* no storage: the drawing just is not kept across a reload */
  }
}

/**
 * Draws a new Drill from nothing, no photos and no Claude (ADR-0020): the animator in its creating
 * mode, on a phone or a desktop. The Drill is only created when the coach saves and names it, so
 * an abandoned drawing leaves nothing behind; until then a draft is kept in the browser. Anyone in
 * the Team may draw.
 */
@Component({
  selector: 'app-drill-draw',
  imports: [DrillEditor, FormsModule, RouterLink, ThemeToggle],
  templateUrl: './drill-draw.html',
  styleUrl: './drill-draw.scss',
})
export class DrillDraw implements OnInit {
  private readonly api = inject(DrillApiService);
  private readonly router = inject(Router);

  private readonly draft = loadDraft();
  protected readonly script = signal<DrillScript>(this.draft ?? blankScript());
  protected readonly restoredDraft = signal(this.draft !== null);
  protected readonly pending = signal<ScriptEdit | null>(null);
  protected readonly name = signal('');
  protected readonly tags = signal<DrillTag[]>([]);
  protected readonly selectedTags = signal<ReadonlySet<string>>(new Set());
  protected readonly saving = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly canCreate = computed(() => this.name().trim() !== '' && !this.saving());

  private readonly saveDialog = viewChild<ElementRef<HTMLDialogElement>>('saveDialog');

  ngOnInit(): void {
    this.api.listTags().subscribe({ next: (tags) => this.tags.set(tags), error: () => undefined });
  }

  /** Every change of the drawing goes into the draft. */
  protected changed(script: DrillScript): void {
    storeDraft(script);
  }

  protected discardDraft(): void {
    storeDraft(null);
    this.restoredDraft.set(false);
    this.script.set(blankScript());
  }

  /** The editor's Save: asks for the name and tags before anything is created. */
  protected askName(edit: ScriptEdit): void {
    this.pending.set(edit);
    this.errorMessage.set(null);
    this.saveDialog()?.nativeElement.showModal?.();
  }

  protected closeSave(): void {
    this.saveDialog()?.nativeElement.close?.();
    this.pending.set(null);
  }

  protected toggleTag(tagId: string): void {
    const next = new Set(this.selectedTags());
    if (next.has(tagId)) next.delete(tagId);
    else next.add(tagId);
    this.selectedTags.set(next);
  }

  protected create(): void {
    const edit = this.pending();
    if (!edit || !this.canCreate()) return;
    this.saving.set(true);
    this.errorMessage.set(null);
    this.api.createDrawn(this.name().trim(), [...this.selectedTags()], edit.script, edit.changeSummary).subscribe({
      next: (drill) => {
        storeDraft(null);
        this.saving.set(false);
        this.saveDialog()?.nativeElement.close?.();
        this.router.navigate(['/uebungen', drill.id]);
      },
      error: (error) => {
        this.saving.set(false);
        this.errorMessage.set(error?.error?.message ?? 'Die Übung konnte nicht gespeichert werden.');
      },
    });
  }

  /** Leaving keeps the draft; it is there again next time. */
  protected leave(): void {
    this.router.navigate(['/uebungen']);
  }
}
