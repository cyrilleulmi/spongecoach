import { Component, computed, input, output } from '@angular/core';
import { FocusRef } from '../../lines/line.model';
import { DialLine } from '../dial-palette';
import { TimelineEvent } from '../iteration.model';

export interface FocusChange {
  lineId: string;
  focusId: string | null;
}

/**
 * The detail panel for the selected event: its per-Line Focus status (each editable via a
 * dropdown of that Line's associated Focuses), its date, a Match's name, and a delete control.
 */
@Component({
  selector: 'app-event-detail',
  templateUrl: './event-detail.html',
  styleUrl: './event-detail.scss',
})
export class EventDetail {
  readonly event = input.required<TimelineEvent | null>();
  readonly lines = input.required<DialLine[]>();
  /** Each Line's associated Focuses (from the per-line overview), keyed by line id. */
  readonly focusesByLine = input.required<Map<string, FocusRef[]>>();
  readonly isNext = input<boolean>(false);

  readonly focusChange = output<FocusChange>();
  readonly dateChange = output<string | null>();
  readonly rename = output<string>();
  readonly deleteEvent = output<string>();

  protected readonly isTraining = computed(() => this.event()?.type === 'Training');

  protected readonly title = computed(() => {
    const event = this.event();
    if (!event) {
      return '';
    }
    return this.isTraining() ? `Training ${event.position}` : (event.name || event.type);
  });

  protected focusesFor(lineId: string): FocusRef[] {
    return this.focusesByLine().get(lineId) ?? [];
  }

  protected selectedFocusId(lineId: string): string {
    return this.event()?.focusAttachments.find((a) => a.lineId === lineId)?.focusId ?? '';
  }

  protected onFocusSelect(lineId: string, value: string): void {
    this.focusChange.emit({ lineId, focusId: value ? value : null });
  }

  protected onDateInput(value: string): void {
    this.dateChange.emit(value ? value : null);
  }
}
