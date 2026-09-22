import { Component, computed, effect, input, output, signal } from '@angular/core';
import { CatalogRef, FocusRef } from '../../lines/line.model';
import { AttendanceStatus, PlayerAttendance, TimelineEvent } from '../iteration.model';

export interface FocusChange {
  lineId: string;
  focusId: string | null;
}

export interface AttendanceChange {
  playerId: string;
  status: AttendanceStatus;
  declineMessage: string | null;
}

/**
 * The detail panel for the selected event: its per-Line Focus status (each editable via a
 * dropdown of that Line's associated Focuses), its date, a Match's name, and a delete control.
 * Once the event's date is in the past it's read-only by default — the backend still allows the
 * write, but the coach must explicitly choose "edit anyway" to avoid accidental changes to a
 * done event.
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
  /** Each Line's associated Focuses (from the per-line overview), keyed by line id. */
  readonly focusesByLine = input.required<Map<string, FocusRef[]>>();
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

  protected focusesFor(lineId: string): FocusRef[] {
    return this.focusesByLine().get(lineId) ?? [];
  }

  protected selectedFocusId(lineId: string): string {
    return this.event()?.focusAttachments.find((a) => a.lineId === lineId)?.focusId ?? '';
  }

  /** Full name of the Focus currently set for a Line — read out below the dropdown so a long,
   * multi-sentence Focus name isn't stuck truncated inside the closed `<select>`. */
  protected selectedFocusName(lineId: string): string {
    return this.event()?.focusAttachments.find((a) => a.lineId === lineId)?.focusName ?? '';
  }

  protected onFocusSelect(lineId: string, value: string): void {
    this.focusChange.emit({ lineId, focusId: value ? value : null });
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
