import { Component, input, output } from '@angular/core';
import { AttendanceStatus, TimelineEvent } from '../iteration.model';

/** One (Player, Line) roster icon to a dial's right — a Player on two attending Lines gets one of
 * these per Line, per CONTEXT.md's Player/Roster model. */
export interface RosterIcon {
  key: string;
  name: string;
  color: string;
  status: AttendanceStatus;
}

/**
 * The vertical dial timeline for one Iteration: numbered Trainings running top to bottom on a
 * connected line, closing on a Match. Each event is a dial with one quadrant per Line attending
 * that event (its own attendance snapshot, not the live Line list — ADR-0012); a quadrant lit in
 * the Line's color means that Line has a Focus set for the event. To the dial's right, small
 * person icons show each attending Player in their Line's color, grouped one row per Line —
 * filled for confirmed, hollow for no response yet, and crossed out for declined.
 */
@Component({
  selector: 'app-dial-timeline',
  templateUrl: './dial-timeline.html',
  styleUrl: './dial-timeline.scss',
})
export class DialTimeline {
  readonly events = input.required<TimelineEvent[]>();
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

  protected isPast(event: TimelineEvent): boolean {
    return new Date(event.scheduledOn) < new Date();
  }

  /**
   * A Training's display number: its 1-based rank among Trainings in {@link events}, which the
   * backend always returns sorted by scheduledOn — so this is the scheduled order, not an
   * arbitrary index.
   */
  protected trainingNumber(event: TimelineEvent): number {
    return this.events()
      .filter((e) => this.isTraining(e))
      .findIndex((e) => e.id === event.id) + 1;
  }

  protected glyph(event: TimelineEvent): string {
    if (this.isTraining(event)) {
      return `T${this.trainingNumber(event)}`;
    }
    return this.isMatch(event) ? 'M' : (event.type[0] ?? '•');
  }

  protected label(event: TimelineEvent): string {
    return this.isTraining(event)
      ? `Training ${this.trainingNumber(event)}`
      : (event.name || event.type);
  }

  /** CSS `conic-gradient` stops: one equal slice per attending Line, lit or pale. */
  protected gradient(event: TimelineEvent): string {
    const lines = event.lines;
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

  /** One roster icon per (Line, Player) — a Player on two attending Lines appears once per Line —
   * grouped by Line (event.lines' order, the same as the focus quadrants) so each Line's Players
   * form their own row to the dial's right. */
  protected rosterByLine(event: TimelineEvent): { lineId: string; icons: RosterIcon[] }[] {
    return event.lines
      .map((line) => ({
        lineId: line.id,
        icons: event.attendance
          .filter((player) => player.lineIds.includes(line.id))
          .map((player) => ({
            key: `${player.playerId}-${line.id}`,
            name: player.playerName,
            color: line.color,
            status: player.status,
          })),
      }))
      .filter((group) => group.icons.length > 0);
  }

  protected statusLabel(status: AttendanceStatus): string {
    switch (status) {
      case 'ATTENDING':
        return 'Zugesagt';
      case 'DECLINED':
        return 'Abgesagt';
      default:
        return 'Keine Antwort';
    }
  }
}
