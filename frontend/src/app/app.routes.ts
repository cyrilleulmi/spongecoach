import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'clubs', pathMatch: 'full' },
  {
    path: 'clubs',
    loadComponent: () => import('./features/clubs/clubs.component').then(m => m.ClubsComponent),
  },
  {
    path: 'clubs/:id',
    loadComponent: () => import('./features/clubs/club-detail.component').then(m => m.ClubDetailComponent),
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent),
  },
  {
    path: 'teams/:id',
    loadComponent: () => import('./features/teams/team-detail.component').then(m => m.TeamDetailComponent),
  },
];
