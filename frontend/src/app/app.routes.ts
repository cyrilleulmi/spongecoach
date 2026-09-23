import { Routes } from '@angular/router';
import { LineOverview } from './lines/line-overview/line-overview';
import { PlayerList } from './players/player-list/player-list';
import { PlayerView } from './players/player-view/player-view';
import { TeamOverview } from './team/team-overview/team-overview';

export const routes: Routes = [
  { path: 'team', component: TeamOverview, title: 'Team-Übersicht · SpongeCoach' },
  { path: 'lines', component: LineOverview, title: 'Block-Übersicht · SpongeCoach' },
  { path: 'players', component: PlayerList, title: 'Spieler · SpongeCoach' },
  { path: 'players/:id', component: PlayerView, title: 'Spieler · SpongeCoach' },
  { path: '', pathMatch: 'full', redirectTo: 'team' },
  { path: '**', redirectTo: 'team' },
];
