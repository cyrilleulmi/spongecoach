import { Component, computed, effect, input, output, signal } from '@angular/core';
import { CatalogRef } from '../../lines/line.model';
import { AttendanceStatus, PlayerAttendance, TimelineEvent } from '../iteration.model';

export interface FocusChange {
  lineId: string;
  focus: string | null;
}

export interface AttendanceChange {
  playerId: string;
  status: AttendanceStatus;
  declineMessage: string | null;
}

/**
 * The detail panel for the selected event: its per-Line Focus (a free-text field, with a "same
 * focus again" shortcut to repeat that Line's most recent earlier Focus text — ADR-0014), its
 * date, a Match's name, and a delete control. Once the event's date is in the past it's read-only
 * by default — the backend still allows the write, but the coach must explicitly choose "edit
 * anyway" to avoid accidental changes to a done event.
 */
@Component({
  selector: 'app-event-detail',
  templateUrl: './event-detail.html',
  styleUrl: './event-detail.scss',
})
export class EventDetail {
  readonly event = input.required<TimelineEvent | null>();
  /** All events in the current Iteration, scheduledOn-sorted — used to number Trainings. */
  readonly events = input.required<TimelineEvent[]>();
  /** Each Line's most recent earlier Focus text (across the whole timeline, not just this
   * Iteration), keyed by line id — powers "same focus again". Absent where a Line has never had
   * one set on an earlier Event. */
  readonly lastFocusByLine = input.required<Map<string, string>>();
  readonly isNext = input<boolean>(false);

  readonly focusChange = output<FocusChange>();
  readonly attendanceChange = output<AttendanceChange>();
  readonly dateChange = output<string>();
  readonly rename = output<string>();
  readonly deleteEvent = output<string>();

  /** Lifts the default read-only state for a done event, for the current selection only. */
  protected readonly editAnyway = signal(false);

  constructor() {
    effect(() => {
      this.event();
      this.editAnyway.set(false);
    });
  }

  protected readonly isTraining = computed(() => this.event()?.type === 'Training');

  /** The Lines that attended this event, as snapshotted at its creation (ADR-0012) — not the
   * page-global, currently-active Line list. */
  protected readonly attendingLines = computed<CatalogRef[]>(() => this.event()?.lines ?? []);

  protected readonly isDone = computed<boolean>(() => {
    const event = this.event();
    return !!event && new Date(event.scheduledOn) < new Date();
  });

  protected readonly readonly = computed(() => this.isDone() && !this.editAnyway());

  protected readonly title = computed(() => {
    const event = this.event();
    if (!event) {
      return '';
    }
    if (!this.isTraining()) {
      return event.name || event.type;
    }
    const trainingNumber =
      this.events()
        .filter((e) => e.type === 'Training')
        .findIndex((e) => e.id === event.id) + 1;
    return `Training ${trainingNumber}`;
  });

  protected toggleEditAnyway(): void {
    this.editAnyway.set(!this.editAnyway());
  }

  protected focusTextFor(lineId: string): string {
    return this.event()?.focusAttachments.find((a) => a.lineId === lineId)?.focus ?? '';
  }

  protected lastFocusFor(lineId: string): string | null {
    return this.lastFocusByLine().get(lineId) ?? null;
  }

  protected onFocusInput(lineId: string, value: string): void {
    const trimmed = value.trim();
    this.focusChange.emit({ lineId, focus: trimmed ? trimmed : null });
  }

  /** "Gleicher Fokus wie zuletzt": copies that Line's most recent earlier Focus text as this
   * Event's own text — a plain copy, not a reference (ADR-0014). */
  protected sameFocusAgain(lineId: string): void {
    const lastFocus = this.lastFocusFor(lineId);
    if (lastFocus) {
      this.focusChange.emit({ lineId, focus: lastFocus });
    }
  }

  protected onDateInput(value: string): void {
    if (!value) {
      return;
    }
    this.dateChange.emit(value);
  }

  /** Players grouped by their first attending Line (same order as `attendingLines()`/the focus
   * rows above), name-sorted within each group; each Player shown once even if on several
   * attending Lines (see `PlayerAttendance.lineIds`). */
  protected readonly attendanceRows = computed<PlayerAttendance[]>(() => {
    const lines = this.attendingLines();
    const primaryLineIndex = (row: PlayerAttendance): number => {
      const index = lines.findIndex((line) => row.lineIds.includes(line.id));
      return index === -1 ? lines.length : index;
    };
    return [...(this.event()?.attendance ?? [])].sort((a, b) => {
      const lineDiff = primaryLineIndex(a) - primaryLineIndex(b);
      return lineDiff !== 0 ? lineDiff : a.playerName.localeCompare(b.playerName, 'de');
    });
  });

  /** Attending / total count for one Line, for the "2/4"-style counter next to its focus row. */
  protected lineAttendanceCount(lineId: string): { attending: number; total: number } {
    const forLine = (this.event()?.attendance ?? []).filter((a) => a.lineIds.includes(lineId));
    return {
      attending: forLine.filter((a) => a.status === 'ATTENDING').length,
      total: forLine.length,
    };
  }

  protected lineName(lineId: string): string {
    return this.attendingLines().find((l) => l.id === lineId)?.name ?? '';
  }

  protected lineColor(lineId: string): string {
    return this.attendingLines().find((l) => l.id === lineId)?.color ?? 'var(--ink-soft)';
  }

  protected onAttendanceStatusSelect(row: PlayerAttendance, value: string): void {
    const status = value as AttendanceStatus;
    this.attendanceChange.emit({
      playerId: row.playerId,
      status,
      declineMessage: status === 'DECLINED' ? (row.declineMessage ?? null) : null,
    });
  }

  protected onDeclineMessageChange(row: PlayerAttendance, value: string): void {
    this.attendanceChange.emit({
      playerId: row.playerId,
      status: 'DECLINED',
      declineMessage: value.trim() ? value.trim() : null,
    });
  }
}
