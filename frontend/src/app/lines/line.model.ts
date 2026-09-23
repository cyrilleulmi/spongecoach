export interface LineSummary {
  id: string;
  name: string;
  playerCount: number;
  color: string;
}

export interface Player {
  id: string;
  name: string;
  avatarVersion: number | null;
}

export interface CatalogRef {
  id: string;
  name: string;
  color: string;
}

export interface LineSkill {
  skillId: string;
  name: string;
  color: string;
  rating: number;
}

export interface LineDetail {
  id: string;
  name: string;
  players: Player[];
  skills: LineSkill[];
  developmentGoals: CatalogRef[];
}
