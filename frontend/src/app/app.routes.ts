import { Routes } from '@angular/router';
import { DrillDraw } from './drills/drill-draw/drill-draw';
import { DrillList } from './drills/drill-list/drill-list';
import { DrillUpload } from './drills/drill-upload/drill-upload';
import { DrillView } from './drills/drill-view/drill-view';
import { LineOverview } from './lines/line-overview/line-overview';
import { PlayerList } from './players/player-list/player-list';
import { PlayerView } from './players/player-view/player-view';
import { TeamOverview } from './team/team-overview/team-overview';

export const routes: Routes = [
  { path: 'team', component: TeamOverview, title: 'Team-Übersicht · SpongeCoach' },
  { path: 'lines', component: LineOverview, title: 'Block-Übersicht · SpongeCoach' },
  { path: 'players', component: PlayerList, title: 'Spieler · SpongeCoach' },
  { path: 'players/:id', component: PlayerView, title: 'Spieler · SpongeCoach' },
  { path: 'uebungen', component: DrillList, title: 'Übungen · SpongeCoach' },
  { path: 'uebungen/neu', component: DrillUpload, title: 'Neue Übung · SpongeCoach' },
  { path: 'uebungen/neu/zeichnen', component: DrillDraw, title: 'Übung zeichnen · SpongeCoach' },
  { path: 'uebungen/:id', component: DrillView, title: 'Übung · SpongeCoach' },
  { path: '', pathMatch: 'full', redirectTo: 'team' },
  { path: '**', redirectTo: 'team' },
];
