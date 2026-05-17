import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Team {
  id: string;
  name: string;
  clubId: string;
}

export interface CreateTeamRequest {
  name: string;
  clubId: string;
}

export interface TeamMember {
  teamId: string;
  personId: string;
  firstName: string;
  lastName: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class TeamApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/teams';

  /** All teams the current user is a member of. */
  list(): Observable<Team[]> {
    return this.http.get<Team[]>(this.base);
  }

  /** Teams belonging to a specific club (correct scoped endpoint). */
  listByClub(clubId: string): Observable<Team[]> {
    return this.http.get<Team[]>(`/api/clubs/${clubId}/teams`);
  }

  get(id: string): Observable<Team> {
    return this.http.get<Team>(`${this.base}/${id}`);
  }

  create(request: CreateTeamRequest): Observable<Team> {
    return this.http.post<Team>(this.base, request);
  }

  update(id: string, name: string): Observable<Team> {
    return this.http.put<Team>(`${this.base}/${id}`, { name });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  listMembers(teamId: string): Observable<TeamMember[]> {
    return this.http.get<TeamMember[]>(`${this.base}/${teamId}/members`);
  }

  addMember(teamId: string, personId: string): Observable<TeamMember> {
    return this.http.post<TeamMember>(`${this.base}/${teamId}/members`, { personId });
  }

  addMembers(teamId: string, personIds: string[]): Observable<TeamMember[]> {
    return this.http.post<TeamMember[]>(`${this.base}/${teamId}/members/bulk`, { personIds });
  }

  removeMember(teamId: string, personId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${teamId}/members/${personId}`);
  }
}
