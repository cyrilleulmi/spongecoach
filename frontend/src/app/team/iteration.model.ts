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
  /** null for Trainings (numbered by scheduled order); the opponent for Matches. */
  name: string | null;
  /** ISO datetime (yyyy-MM-ddTHH:mm:ss) — mandatory and unique within the Iteration; drives order. */
  scheduledOn: string;
  focusAttachments: FocusAttachment[];
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
