import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

const ADMIN = { id: 'u-admin', name: 'Admin', role: 'SYS_ADMIN', playerId: null };

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

  /** The shell renders no screen until a User is loaded (ADR-0017). */
  function signIn(): void {
    httpMock.expectOne('/api/users').flush([ADMIN]);
    httpMock.expectOne('/api/me').flush({ ...ADMIN, teamId: null, lineIds: [] });
  }

  // spec: ui.navigation
  it('renders the three top-level nav links', async () => {
    const fixture = TestBed.createComponent(App);
    signIn();
    await router.navigateByUrl('/team');
    fixture.detectChanges();

    // Draining the TeamOverview boot requests keeps httpMock.verify() happy.
    httpMock.match('/api/iterations').forEach((r) => r.flush([]));
    httpMock.match('/api/event-types').forEach((r) => r.flush([]));
    httpMock.match('/api/lines').forEach((r) => r.flush([]));

    const links = Array.from(fixture.nativeElement.querySelectorAll('.app-nav a')) as HTMLElement[];
    expect(links.map((a) => a.textContent?.trim())).toEqual(['Team-Übersicht', 'Blöcke', 'Spieler']);
  });

  // spec: ui.default-route-is-team
  it('redirects the empty path to the team overview', async () => {
    const fixture = TestBed.createComponent(App);
    signIn();
    await router.navigateByUrl('');
    fixture.detectChanges();

    httpMock.match('/api/iterations').forEach((r) => r.flush([]));
    httpMock.match('/api/event-types').forEach((r) => r.flush([]));
    httpMock.match('/api/lines').forEach((r) => r.flush([]));

    expect(router.url).toBe('/team');
  });
});

describe('App shell before a User is loaded', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // spec: ui.no-screen-without-user
  it('shows no screen until the User is loaded, and says so if the Users cannot be loaded', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeNull();

    httpMock.expectOne('/api/users').flush('down', { status: 502, statusText: 'Bad Gateway' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.app-error')?.textContent).toContain(
      'Benutzer konnten nicht geladen werden',
    );
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeNull();
  });
});
