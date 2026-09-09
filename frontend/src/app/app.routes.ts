import { Routes } from '@angular/router';
import { LineOverview } from './lines/line-overview/line-overview';
import { TeamOverview } from './team/team-overview/team-overview';

export const routes: Routes = [
  { path: 'lines', component: LineOverview, title: 'Block-Übersicht · SpongeCoach' },
  { path: 'team', component: TeamOverview, title: 'Team-Übersicht · SpongeCoach' },
  { path: '', pathMatch: 'full', redirectTo: 'lines' },
  { path: '**', redirectTo: 'lines' },
];
