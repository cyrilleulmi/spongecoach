import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogRef, FocusRef, LineDetail, LineSkill, LineSummary, Player } from './line.model';

@Injectable({ providedIn: 'root' })
export class LineApiService {
  private readonly http = inject(HttpClient);

  listLines(): Observable<LineSummary[]> {
    return this.http.get<LineSummary[]>('/api/lines');
  }

  getLine(lineId: string): Observable<LineDetail> {
    return this.http.get<LineDetail>(`/api/lines/${lineId}`);
  }

  updateLineAssociations(
    lineId: string,
    body: { playerIds?: string[]; developmentGoalIds?: string[]; focusIds?: string[] },
  ): Observable<LineDetail> {
    return this.http.put<LineDetail>(`/api/lines/${lineId}`, body);
  }

  listPlayerCatalog(): Observable<Player[]> {
    return this.http.get<Player[]>('/api/players');
  }

  setSkillRating(lineId: string, skillId: string, rating: number): Observable<LineSkill> {
    return this.http.put<LineSkill>(`/api/lines/${lineId}/skills/${skillId}`, { rating });
  }

  removeSkill(lineId: string, skillId: string): Observable<void> {
    return this.http.delete<void>(`/api/lines/${lineId}/skills/${skillId}`);
  }

  listSkillCatalog(): Observable<CatalogRef[]> {
    return this.http.get<CatalogRef[]>('/api/skills');
  }

  listGoalCatalog(): Observable<CatalogRef[]> {
    return this.http.get<CatalogRef[]>('/api/development-goals');
  }

  listFocusCatalog(): Observable<FocusRef[]> {
    return this.http.get<FocusRef[]>('/api/focuses');
  }

  createSkill(name: string, color: string): Observable<CatalogRef> {
    return this.http.post<CatalogRef>('/api/skills', { name, color });
  }

  createGoal(name: string, color: string): Observable<CatalogRef> {
    return this.http.post<CatalogRef>('/api/development-goals', { name, color });
  }

  createFocus(name: string, goalIds: string[]): Observable<FocusRef> {
    return this.http.post<FocusRef>('/api/focuses', { name, goalIds });
  }

  setSkillColor(skillId: string, color: string): Observable<CatalogRef> {
    return this.http.put<CatalogRef>(`/api/skills/${skillId}`, { color });
  }

  setGoalColor(goalId: string, color: string): Observable<CatalogRef> {
    return this.http.put<CatalogRef>(`/api/development-goals/${goalId}`, { color });
  }
}
