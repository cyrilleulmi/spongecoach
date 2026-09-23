import { CatalogRef } from '../lines/line.model';

export interface PlayerSummary {
  id: string;
  name: string;
  lines: CatalogRef[];
  /** Null while the Player shows initials; otherwise the cache-busting token for their Avatar image. */
  avatarVersion: number | null;
}

export interface PlayerSkillRating {
  skillId: string;
  name: string;
  color: string;
  rating: number;
}

export interface PlayerDetail {
  id: string;
  name: string;
  lines: CatalogRef[];
  skills: PlayerSkillRating[];
  developmentGoals: CatalogRef[];
  avatarVersion: number | null;
}
