import { Component, input, output } from '@angular/core';
import { DialLine } from '../dial-palette';
import { TimelineEvent } from '../iteration.model';

/**
 * The vertical dial timeline for one Iteration: numbered Trainings running top to bottom on a
 * connected line, closing on a Match. Each event is a 4-quadrant dial (one quadrant per Line);
 * a quadrant lit in the Line's color means that Line has a Focus set for the event.
 */
@Component({
  selector: 'app-dial-timeline',
  templateUrl: './dial-timeline.html',
  styleUrl: './dial-timeline.scss',
})
export class DialTimeline {
  readonly events = input.required<TimelineEvent[]>();
  readonly lines = input.required<DialLine[]>();
  readonly selectedEventId = input<string | null>(null);
  /** The next event that still needs attention — carries the persistent "NEXT" marker. */
  readonly nextEventId = input<string | null>(null);
  readonly select = output<string>();

  protected isTraining(event: TimelineEvent): boolean {
    return event.type === 'Training';
  }

  protected isMatch(event: TimelineEvent): boolean {
    return event.type === 'Match';
  }

  protected glyph(event: TimelineEvent): string {
    if (this.isTraining(event)) {
      return `T${event.position}`;
    }
    return this.isMatch(event) ? 'M' : (event.type[0] ?? '•');
  }

  protected label(event: TimelineEvent): string {
    return this.isTraining(event) ? `Training ${event.position}` : (event.name || event.type);
  }

  /** CSS `conic-gradient` stops: one equal slice per Line, lit or pale. */
  protected gradient(event: TimelineEvent): string {
    const lines = this.lines();
    if (lines.length === 0) {
      return 'var(--dial-track)';
    }
    const slice = 360 / lines.length;
    const litLineIds = new Set(event.focusAttachments.map((a) => a.lineId));
    const stops = lines.map((line, i) => {
      const from = i * slice;
      const to = (i + 1) * slice;
      const color = litLineIds.has(line.id) ? line.color : 'var(--dial-track)';
      return `${color} ${from}deg ${to}deg`;
    });
    return `conic-gradient(${stops.join(', ')})`;
  }
}
