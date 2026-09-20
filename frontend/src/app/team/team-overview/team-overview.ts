import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { LineApiService } from '../../lines/line-api.service';
import { FocusRef, LineSummary } from '../../lines/line.model';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { DialLine } from '../dial-palette';
import { DialTimeline } from '../dial-timeline/dial-timeline';
import { AttendanceChange, EventDetail, FocusChange } from '../event-detail/event-detail';
import { IterationSelector } from '../iteration-selector/iteration-selector';
import { EventType, Iteration, TimelineEvent } from '../iteration.model';
import { IterationApiService } from '../iteration-api.service';

/**
 * The team-overview timeline page. Loads every Iteration in one call, shows one at a time as a
 * vertical dial timeline, and lets the coach edit an event's per-Line Focus attachments, dates,
 * Match names, and add/remove events and Iterations.
 */
@Component({
  selector: 'app-team-overview',
  imports: [IterationSelector, DialTimeline, EventDetail, ThemeToggle],
  templateUrl: './team-overview.html',
  styleUrl: './team-overview.scss',
})
export class TeamOverview implements OnInit {
  private readonly api = inject(IterationApiService);
  private readonly lineApi = inject(LineApiService);

  protected readonly iterations = signal<Iteration[]>([]);
  protected readonly eventTypes = signal<EventType[]>([]);
  protected readonly lineList = signal<LineSummary[]>([]);
  protected readonly focusesByLine = signal<Map<string, FocusRef[]>>(new Map());
  protected readonly iterationIndex = signal(0);
  protected readonly selectedEventId = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly renaming = signal(false);
  protected readonly newIterationName = signal('');

  /** The current Line list, for the legend — each event's own dial uses its attendance snapshot
   * instead (see TimelineEvent.lines), not this live list. */
  protected readonly dialLines = computed<DialLine[]>(() =>
    this.lineList().map((line) => ({ id: line.id, name: line.name, color: line.color })),
  );

  protected readonly currentIteration = computed(
    () => this.iterations()[this.iterationIndex()] ?? null,
  );
  protected readonly currentEvents = computed(() => this.currentIteration()?.events ?? []);

  /**
   * The next event still to come: the first event, in Iteration then scheduledOn order, whose
   * date hasn't passed yet. Once an event is done it's read-only (see EventDetail) and drops out
   * of consideration here regardless of whether every Line has a Focus set.
   */
  protected readonly nextEventId = computed<string | null>(() => {
    const now = new Date();
    for (const iteration of this.iterations()) {
      for (const event of iteration.events) {
        if (new Date(event.scheduledOn) >= now) {
          return event.id;
        }
      }
    }
    return null;
  });

  protected readonly selectedEvent = computed(() => {
    const events = this.currentEvents();
    if (events.length === 0) {
      return null;
    }
    return (
      events.find((e) => e.id === this.selectedEventId()) ??
      events.find((e) => e.id === this.nextEventId()) ??
      events[0]
    );
  });

  protected readonly selectedIsNext = computed(() => {
    const selected = this.selectedEvent();
    return !!selected && selected.id === this.nextEventId();
  });

  ngOnInit(): void {
    forkJoin({
      iterations: this.api.listIterations(),
      eventTypes: this.api.listEventTypes(),
      lines: this.lineApi.listLines(),
    }).subscribe({
      next: ({ iterations, eventTypes, lines }) => {
        this.iterations.set(iterations);
        this.eventTypes.set(eventTypes);
        this.lineList.set(lines);
        this.loading.set(false);
        this.loadLineFocuses(lines);
      },
      error: () => {
        this.errorMessage.set('Die Team-Übersicht konnte nicht geladen werden. Läuft das Backend?');
        this.loading.set(false);
      },
    });
  }

  private loadLineFocuses(lines: LineSummary[]): void {
    if (lines.length === 0) {
      return;
    }
    forkJoin(lines.map((line) => this.lineApi.getLine(line.id))).subscribe({
      next: (details) => {
        const map = new Map<string, FocusRef[]>();
        details.forEach((detail) => map.set(detail.id, detail.focuses));
        this.focusesByLine.set(map);
      },
      error: () => this.errorMessage.set('Die Fokus-Listen der Blöcke konnten nicht geladen werden.'),
    });
  }

  private reload(selectEventId?: string | null): void {
    this.api.listIterations().subscribe({
      next: (iterations) => {
        this.iterations.set(iterations);
        if (this.iterationIndex() >= iterations.length) {
          this.iterationIndex.set(Math.max(0, iterations.length - 1));
        }
        if (selectEventId !== undefined) {
          this.selectedEventId.set(selectEventId);
        }
      },
      error: () => this.errorMessage.set('Aktualisieren fehlgeschlagen.'),
    });
  }

  protected onIndexChange(index: number): void {
    this.iterationIndex.set(index);
    this.selectedEventId.set(null);
    this.renaming.set(false);
  }

  protected onSelectEvent(eventId: string): void {
    this.selectedEventId.set(eventId);
  }

  protected onFocusChange(change: FocusChange): void {
    const event = this.selectedEvent();
    if (!event) {
      return;
    }
    const attachments = event.focusAttachments
      .filter((a) => a.lineId !== change.lineId)
      .map((a) => ({ lineId: a.lineId, focusId: a.focusId }));
    if (change.focusId) {
      attachments.push({ lineId: change.lineId, focusId: change.focusId });
    }
    this.api.setFocusAttachments(event.id, attachments).subscribe({
      next: () => this.reload(event.id),
      error: () => this.errorMessage.set('Fokus konnte nicht gesetzt werden.'),
    });
  }

  protected onAttendanceChange(change: AttendanceChange): void {
    const event = this.selectedEvent();
    if (!event) {
      return;
    }
    this.api
      .setAttendance(event.id, change.playerId, change.status, change.declineMessage)
      .subscribe({
        next: () => this.reload(event.id),
        error: () => this.errorMessage.set('Anwesenheit konnte nicht gespeichert werden.'),
      });
  }

  protected onDateChange(date: string): void {
    const event = this.selectedEvent();
    const iteration = this.currentIteration();
    if (!event || !iteration) {
      return;
    }
    this.api.updateEvent(iteration.id, event.id, { scheduledOn: date }).subscribe({
      next: () => this.reload(event.id),
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(
          err.status === 409
            ? 'Diese Zeit ist in dieser Iteration schon vergeben.'
            : 'Datum konnte nicht gesetzt werden.',
        );
      },
    });
  }

  protected onRenameEvent(name: string): void {
    const event = this.selectedEvent();
    const iteration = this.currentIteration();
    if (!event || !iteration) {
      return;
    }
    this.api.updateEvent(iteration.id, event.id, { name }).subscribe({
      next: () => this.reload(event.id),
      error: () => this.errorMessage.set('Match konnte nicht umbenannt werden.'),
    });
  }

  protected onDeleteEvent(eventId: string): void {
    const iteration = this.currentIteration();
    if (!iteration) {
      return;
    }
    this.api.deleteEvent(iteration.id, eventId).subscribe({
      next: () => this.reload(null),
      error: () => this.errorMessage.set('Ereignis konnte nicht gelöscht werden.'),
    });
  }

  protected addEventOfType(typeName: 'Training' | 'Match'): void {
    const iteration = this.currentIteration();
    const type = this.eventTypes().find((t) => t.name === typeName);
    if (!iteration || !type) {
      return;
    }
    const scheduledOn = this.nextFreeSlot(iteration.events);
    const body =
      typeName === 'Match'
        ? { eventTypeId: type.id, name: 'Neues Match', scheduledOn }
        : { eventTypeId: type.id, scheduledOn };
    this.api.addEvent(iteration.id, body).subscribe({
      next: (event) => this.reload(event.id),
      error: () => this.errorMessage.set('Ereignis konnte nicht hinzugefügt werden.'),
    });
  }

  /** One hour after the Iteration's latest event, or the next full hour if it has none. */
  private nextFreeSlot(events: TimelineEvent[]): string {
    const latest = events.reduce<Date | null>((max, e) => {
      const date = new Date(e.scheduledOn);
      return !max || date > max ? date : max;
    }, null);
    const base = latest ? new Date(latest.getTime() + 60 * 60 * 1000) : new Date();
    if (!latest) {
      base.setMinutes(0, 0, 0);
      base.setHours(base.getHours() + 1);
    }
    const pad = (n: number) => `${n}`.padStart(2, '0');
    return (
      `${base.getFullYear()}-${pad(base.getMonth() + 1)}-${pad(base.getDate())}` +
      `T${pad(base.getHours())}:${pad(base.getMinutes())}`
    );
  }

  protected addIteration(): void {
    const name = this.newIterationName().trim();
    if (!name) {
      return;
    }
    this.api.createIteration({ name }).subscribe({
      next: (created) => {
        this.newIterationName.set('');
        this.api.listIterations().subscribe((iterations) => {
          this.iterations.set(iterations);
          const index = iterations.findIndex((it) => it.id === created.id);
          this.iterationIndex.set(index >= 0 ? index : iterations.length - 1);
          this.selectedEventId.set(null);
        });
      },
      error: () => this.errorMessage.set('Iteration konnte nicht angelegt werden.'),
    });
  }

  protected deleteIteration(): void {
    const iteration = this.currentIteration();
    if (!iteration) {
      return;
    }
    this.api.deleteIteration(iteration.id).subscribe({
      next: () => {
        this.iterationIndex.set(Math.max(0, this.iterationIndex() - 1));
        this.selectedEventId.set(null);
        this.reload();
      },
      error: () => this.errorMessage.set('Iteration konnte nicht gelöscht werden.'),
    });
  }

  protected renameIteration(name: string): void {
    const iteration = this.currentIteration();
    this.renaming.set(false);
    if (!iteration || !name.trim()) {
      return;
    }
    this.api.updateIteration(iteration.id, { name: name.trim() }).subscribe({
      next: () => this.reload(this.selectedEventId()),
      error: () => this.errorMessage.set('Iteration konnte nicht umbenannt werden.'),
    });
  }
}
