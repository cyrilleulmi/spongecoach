import { CatalogRef } from '../lines/line.model';

export interface PlayerSummary {
  id: string;
  name: string;
  lines: CatalogRef[];
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
}
