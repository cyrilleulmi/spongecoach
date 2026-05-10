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
    path: 'persons',
    loadComponent: () => import('./features/persons/persons.component').then(m => m.PersonsComponent),
  },
];
