import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Club {
  id: string;
  name: string;
  location: string;
}

export interface CreateClubRequest {
  name: string;
  location: string;
}

@Injectable({ providedIn: 'root' })
export class ClubApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/clubs';

  list(): Observable<Club[]> {
    return this.http.get<Club[]>(this.base);
  }

  get(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.base}/${id}`);
  }

  create(request: CreateClubRequest): Observable<Club> {
    return this.http.post<Club>(this.base, request);
  }

  update(id: string, request: CreateClubRequest): Observable<Club> {
    return this.http.put<Club>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
