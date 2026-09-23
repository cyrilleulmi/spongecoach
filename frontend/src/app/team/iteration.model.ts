import { CatalogRef } from '../lines/line.model';

export interface EventType {
  id: string;
  name: string;
}

/** One Line's free-text Focus set for an Event, denormalised with the Line name by the read-side
 * timeline. */
export interface FocusAttachment {
  lineId: string;
  lineName: string;
  focus: string;
}

export type AttendanceStatus = 'PENDING' | 'ATTENDING' | 'DECLINED';

/** One Player's attendance answer for an Event, with every attending Line they belong to for it
 * (a Player on two attending Lines still gets exactly one entry here, with both line ids). */
export interface PlayerAttendance {
  playerId: string;
  playerName: string;
  avatarVersion: number | null;
  lineIds: string[];
  status: AttendanceStatus;
  declineMessage: string | null;
}

export interface TimelineEvent {
  id: string;
  typeId: string;
  /** Event type name — "Training" or "Match" (an open list). */
  type: string;
  /** null for Trainings (numbered by scheduled order); the opponent for Matches. */
  name: string | null;
  /** ISO datetime (yyyy-MM-ddTHH:mm:ss) — mandatory and unique within the Iteration; drives order. */
  scheduledOn: string;
  focusAttachments: FocusAttachment[];
  /** Lines attending this Event, snapshotted at creation — not the live Line list (ADR-0012). */
  lines: CatalogRef[];
  /** Every Player on this Event's attendance list, snapshotted the same way as `lines`. */
  attendance: PlayerAttendance[];
}

export interface Iteration {
  id: string;
  name: string;
  position: number;
  /** Always returned sorted by scheduledOn ascending. */
  events: TimelineEvent[];
}

export interface EventDraft {
  eventTypeId: string;
  name?: string;
  scheduledOn: string;
}
