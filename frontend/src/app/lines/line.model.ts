export interface LineSummary {
  id: string;
  name: string;
  playerCount: number;
}

export interface Player {
  id: string;
  name: string;
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

export interface FocusRef {
  id: string;
  name: string;
  goalIds: string[];
}

export interface LineDetail {
  id: string;
  name: string;
  players: Player[];
  skills: LineSkill[];
  developmentGoals: CatalogRef[];
  focuses: FocusRef[];
}
