export interface EventType {
  id: string;
  name: string;
}

/** One Line's Focus set for an Event, denormalised with names by the read-side timeline. */
export interface FocusAttachment {
  lineId: string;
  lineName: string;
  focusId: string;
  focusName: string;
}

export interface TimelineEvent {
  id: string;
  typeId: string;
  /** Event type name — "Training" or "Match" (an open list). */
  type: string;
  /** null for Trainings (numbered by position); the opponent for Matches. */
  name: string | null;
  position: number;
  /** ISO date (yyyy-mm-dd) or null when no date is set. */
  scheduledOn: string | null;
  focusAttachments: FocusAttachment[];
}

export interface Iteration {
  id: string;
  name: string;
  position: number;
  events: TimelineEvent[];
}

export interface EventDraft {
  eventTypeId: string;
  name?: string;
  position?: number;
  scheduledOn?: string;
}
