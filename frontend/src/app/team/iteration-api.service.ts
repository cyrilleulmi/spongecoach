import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AttendanceStatus, EventDraft, EventType, Iteration, TimelineEvent } from './iteration.model';

interface FocusAttachmentInput {
  lineId: string;
  focusId: string;
}

@Injectable({ providedIn: 'root' })
export class IterationApiService {
  private readonly http = inject(HttpClient);

  /** The one aggregate call the timeline needs: all iterations, events + attachments nested. */
  listIterations(): Observable<Iteration[]> {
    return this.http.get<Iteration[]>('/api/iterations');
  }

  listEventTypes(): Observable<EventType[]> {
    return this.http.get<EventType[]>('/api/event-types');
  }

  createIteration(body: {
    name: string;
    position?: number;
    events?: EventDraft[];
  }): Observable<Iteration> {
    return this.http.post<Iteration>('/api/iterations', body);
  }

  updateIteration(
    iterationId: string,
    body: { name?: string; position?: number },
  ): Observable<Iteration> {
    return this.http.put<Iteration>(`/api/iterations/${iterationId}`, body);
  }

  deleteIteration(iterationId: string): Observable<void> {
    return this.http.delete<void>(`/api/iterations/${iterationId}`);
  }

  addEvent(iterationId: string, body: EventDraft): Observable<TimelineEvent> {
    return this.http.post<TimelineEvent>(`/api/iterations/${iterationId}/events`, body);
  }

  /** Iteration-scoped: reschedule / rename / retype one event. A colliding scheduledOn is a 409. */
  updateEvent(
    iterationId: string,
    eventId: string,
    body: { eventTypeId?: string; name?: string; scheduledOn?: string },
  ): Observable<TimelineEvent> {
    return this.http.put<TimelineEvent>(`/api/iterations/${iterationId}/events/${eventId}`, body);
  }

  deleteEvent(iterationId: string, eventId: string): Observable<void> {
    return this.http.delete<void>(`/api/iterations/${iterationId}/events/${eventId}`);
  }

  /** Replace an event's full set of per-Line Focus attachments (empty clears them all). */
  setFocusAttachments(
    eventId: string,
    focusAttachments: FocusAttachmentInput[],
  ): Observable<TimelineEvent> {
    return this.http.put<TimelineEvent>(`/api/events/${eventId}`, { focusAttachments });
  }

  /** Sets one Player's attendance answer for an Event. A non-DECLINED status clears any decline
   * message server-side, so it's not sent unless declining. */
  setAttendance(
    eventId: string,
    playerId: string,
    status: AttendanceStatus,
    declineMessage?: string | null,
  ): Observable<TimelineEvent> {
    return this.http.put<TimelineEvent>(`/api/events/${eventId}/attendance/${playerId}`, {
      status,
      declineMessage: status === 'DECLINED' ? (declineMessage ?? null) : null,
    });
  }
}
