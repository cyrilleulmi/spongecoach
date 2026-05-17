import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Person {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface CreatePersonRequest {
  firstName: string;
  lastName: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class PersonApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/persons';

  get(id: string): Observable<Person> {
    return this.http.get<Person>(`${this.base}/${id}`);
  }

  create(request: CreatePersonRequest): Observable<Person> {
    return this.http.post<Person>(this.base, request);
  }

  update(id: string, request: CreatePersonRequest): Observable<Person> {
    return this.http.put<Person>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
