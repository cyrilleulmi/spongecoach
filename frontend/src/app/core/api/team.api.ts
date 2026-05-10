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

@Injectable({ providedIn: 'root' })
export class TeamApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/teams';

  list(clubId?: string): Observable<Team[]> {
    const url = clubId ? `${this.base}?clubId=${clubId}` : this.base;
    return this.http.get<Team[]>(url);
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
}
