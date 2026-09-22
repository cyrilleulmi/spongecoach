import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App shell', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  // spec: ui.navigation
  it('renders the two top-level nav links', async () => {
    const fixture = TestBed.createComponent(App);
    await router.navigateByUrl('/team');
    fixture.detectChanges();

    // Draining the TeamOverview boot requests keeps httpMock.verify() happy.
    httpMock.match('/api/iterations').forEach((r) => r.flush([]));
    httpMock.match('/api/event-types').forEach((r) => r.flush([]));
    httpMock.match('/api/lines').forEach((r) => r.flush([]));

    const links = Array.from(fixture.nativeElement.querySelectorAll('.app-nav a')) as HTMLElement[];
    expect(links.map((a) => a.textContent?.trim())).toEqual(['Blöcke', 'Team-Übersicht']);
  });

  it('redirects the empty path to the per-line overview', async () => {
    const fixture = TestBed.createComponent(App);
    await router.navigateByUrl('');
    fixture.detectChanges();

    httpMock.expectOne('/api/lines').flush([]);
    httpMock.expectOne('/api/players').flush([]);
    httpMock.expectOne('/api/skills').flush([]);
    httpMock.expectOne('/api/development-goals').flush([]);

    expect(router.url).toBe('/lines');
  });
});
