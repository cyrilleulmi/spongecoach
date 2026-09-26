import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AttendanceStatus, EventDraft, EventType, Iteration, TimelineEvent } from './iteration.model';

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

  /** Sets one Line's Focus for an event, leaving the other Lines' alone; null clears it. A Player
   * may do this for their own Lines, which the whole-set write would not allow (ADR-0017). */
  setFocus(eventId: string, lineId: string, focus: string | null): Observable<TimelineEvent> {
    return this.http.put<TimelineEvent>(`/api/events/${eventId}/focus/${lineId}`, { focus });
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
