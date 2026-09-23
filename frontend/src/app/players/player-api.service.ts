import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CatalogRef } from '../lines/line.model';
import { PlayerDetail, PlayerSkillRating, PlayerSummary } from './player.model';

@Injectable({ providedIn: 'root' })
export class PlayerApiService {
  private readonly http = inject(HttpClient);

  listPlayers(): Observable<PlayerSummary[]> {
    return this.http.get<PlayerSummary[]>('/api/players');
  }

  getPlayer(playerId: string): Observable<PlayerDetail> {
    return this.http.get<PlayerDetail>(`/api/players/${playerId}`);
  }

  setDevelopmentGoals(playerId: string, developmentGoalIds: string[]): Observable<PlayerDetail> {
    return this.http.put<PlayerDetail>(`/api/players/${playerId}`, { developmentGoalIds });
  }

  setSkillRating(playerId: string, skillId: string, rating: number): Observable<PlayerSkillRating> {
    return this.http.put<PlayerSkillRating>(`/api/players/${playerId}/skills/${skillId}`, { rating });
  }

  removeSkill(playerId: string, skillId: string): Observable<void> {
    return this.http.delete<void>(`/api/players/${playerId}/skills/${skillId}`);
  }

  listSkillCatalog(): Observable<CatalogRef[]> {
    return this.http.get<CatalogRef[]>('/api/player-skills');
  }

  listGoalCatalog(): Observable<CatalogRef[]> {
    return this.http.get<CatalogRef[]>('/api/player-development-goals');
  }

  createSkill(name: string, color: string): Observable<CatalogRef> {
    return this.http.post<CatalogRef>('/api/player-skills', { name, color });
  }

  createGoal(name: string, color: string): Observable<CatalogRef> {
    return this.http.post<CatalogRef>('/api/player-development-goals', { name, color });
  }
}
