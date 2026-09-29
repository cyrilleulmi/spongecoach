import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CurrentUserService } from '../../auth/current-user.service';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { DrillApiService, sketchUrl } from '../drill-api.service';
import { DrillChat } from '../drill-chat/drill-chat';
import { DrillEditor, ScriptEdit } from '../drill-editor/drill-editor';
import { STATUS_LABELS } from '../drill-list/drill-list';
import { DrillQuestions, QuestionAnswers } from '../drill-questions/drill-questions';
import { DrillRink } from '../drill-rink/drill-rink';
import { DrillDetail, DrillTag, Question } from '../drill.model';
import { RinkPlayer } from '../rink-player/rink-player';

/** How often the screen asks whether the interpreter is done (ADR-0019). */
export const POLL_MS = 2000;

/**
 * One Drill (ADR-0018): its animation, per Stage; the photos with what the interpreter read on
 * them; the interpreter's questions; the conversation and the versions. A coach can answer,
 * correct by chat, edit by hand, revert, rename and delete; a Player only watches.
 */
@Component({
  selector: 'app-drill-view',
  imports: [DrillChat, DrillEditor, DrillQuestions, DrillRink, FormsModule, RinkPlayer, RouterLink, ThemeToggle],
  templateUrl: './drill-view.html',
  styleUrl: './drill-view.scss',
})
export class DrillView implements OnInit, OnDestroy {
  private readonly api = inject(DrillApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly currentUser = inject(CurrentUserService);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly drill = signal<DrillDetail | null>(null);
  protected readonly notFound = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly sending = signal(false);
  protected readonly stageIndex = signal(0);
  protected readonly editing = signal(false);
  protected readonly editError = signal<string | null>(null);
  protected readonly focusedQuestion = signal<Question | null>(null);
  protected readonly shownReadings = signal<ReadonlySet<number>>(new Set());
  protected readonly renaming = signal(false);
  protected readonly nameDraft = signal('');
  protected readonly tagDraft = signal<ReadonlySet<string>>(new Set());
  protected readonly allTags = signal<DrillTag[]>([]);

  protected readonly busy = computed(() => this.drill()?.status === 'PENDING' || this.sending());
  protected readonly stages = computed(() => this.drill()?.script?.stages ?? []);
  protected readonly stage = computed(() => this.stages()[this.stageIndex()] ?? null);

  private drillId = '';
  private pollTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.drillId = this.route.snapshot.paramMap.get('id') ?? '';
    this.load();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  protected sketchSrc(position: number): string {
    return sketchUrl(this.drillId, position);
  }

  protected toggleReading(position: number): void {
    const next = new Set(this.shownReadings());
    if (next.has(position)) next.delete(position);
    else next.add(position);
    this.shownReadings.set(next);
  }

  /** The reading is shown when asked for, or when a question points at that sketch. */
  protected readingShown(position: number): boolean {
    return this.shownReadings().has(position) || this.focusedQuestion()?.sketch === position;
  }

  protected answer(answers: QuestionAnswers): void {
    this.run(this.api.answer(this.drillId, answers.answers, answers.guess), 'Die Antworten konnten nicht gesendet werden.');
  }

  protected chat(message: string): void {
    this.run(this.api.chat(this.drillId, message), 'Die Nachricht konnte nicht gesendet werden.');
  }

  protected retry(): void {
    this.run(this.api.retry(this.drillId), 'Der neue Versuch konnte nicht gestartet werden.');
  }

  protected revert(version: number): void {
    this.run(this.api.revert(this.drillId, version), 'Die Version konnte nicht wiederhergestellt werden.');
  }

  protected saveEdit(edit: ScriptEdit): void {
    this.sending.set(true);
    this.editError.set(null);
    this.api.saveScript(this.drillId, edit.script, edit.changeSummary).subscribe({
      next: (drill) => {
        this.sending.set(false);
        this.editing.set(false);
        this.show(drill);
      },
      error: (error: HttpErrorResponse) => {
        this.sending.set(false);
        this.editError.set(error.error?.message ?? 'Das Skript konnte nicht gespeichert werden.');
      },
    });
  }

  protected startRename(): void {
    const drill = this.drill();
    if (!drill) return;
    this.nameDraft.set(drill.name);
    this.tagDraft.set(new Set(drill.tags.map((tag) => tag.id)));
    this.renaming.set(true);
    if (this.allTags().length === 0) {
      this.api.listTags().subscribe({ next: (tags) => this.allTags.set(tags), error: () => undefined });
    }
  }

  protected toggleDraftTag(tagId: string): void {
    const next = new Set(this.tagDraft());
    if (next.has(tagId)) next.delete(tagId);
    else next.add(tagId);
    this.tagDraft.set(next);
  }

  protected saveRename(): void {
    const name = this.nameDraft().trim();
    if (!name) return;
    this.api.update(this.drillId, { name, tagIds: [...this.tagDraft()] }).subscribe({
      next: (drill) => {
        this.renaming.set(false);
        this.show(drill);
      },
      error: () => this.errorMessage.set('Die Änderung konnte nicht gespeichert werden.'),
    });
  }

  protected deleteDrill(): void {
    if (typeof confirm === 'function' && !confirm('Diese Übung löschen?')) return;
    this.api.delete(this.drillId).subscribe({
      next: () => this.router.navigate(['/uebungen']),
      error: () => this.errorMessage.set('Die Übung konnte nicht gelöscht werden.'),
    });
  }

  private run(request: Observable<DrillDetail>, failure: string): void {
    this.sending.set(true);
    this.errorMessage.set(null);
    request.subscribe({
      next: (drill) => {
        this.sending.set(false);
        this.show(drill);
      },
      error: (error: HttpErrorResponse) => {
        this.sending.set(false);
        this.errorMessage.set(error.status === 409 ? 'Claude arbeitet noch an dieser Übung.' : failure);
      },
    });
  }

  private load(): void {
    this.api.getDrill(this.drillId).subscribe({
      next: (drill) => this.show(drill),
      error: (error: HttpErrorResponse) => {
        if (error.status === 404) this.notFound.set(true);
        else this.errorMessage.set('Die Übung konnte nicht geladen werden. Läuft das Backend?');
      },
    });
  }

  private show(drill: DrillDetail): void {
    const previousVersion = this.drill()?.currentVersion;
    this.drill.set(drill);
    if (drill.currentVersion !== previousVersion && this.stageIndex() >= (drill.script?.stages.length ?? 0)) {
      this.stageIndex.set(0);
    }
    this.stopPolling();
    if (drill.status === 'PENDING') {
      this.pollTimer = setTimeout(() => this.load(), POLL_MS);
    }
  }

  private stopPolling(): void {
    if (this.pollTimer !== null) clearTimeout(this.pollTimer);
    this.pollTimer = null;
  }
}
