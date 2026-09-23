import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PlayerView } from './player-view';
import { PlayerDetail } from '../player.model';

const SKILL = { id: 'ps-1', name: 'Schusstechnik', color: '#b1467a' };
const SKILL_2 = { id: 'ps-2', name: 'Ausdauer', color: '#4f7a3f' };
const GOAL = { id: 'pg-1', name: 'Mehr Abschlüsse suchen', color: '#b1467a' };
const GOAL_2 = { id: 'pg-2', name: 'Konstanz über 60 Minuten', color: '#4f7a3f' };

const CARMELA: PlayerDetail = {
  id: 'p-1',
  name: 'Carmela',
  lines: [{ id: 'line-a', name: 'Kiwi', color: '#4c8c3d' }],
  skills: [{ skillId: SKILL.id, name: SKILL.name, color: SKILL.color, rating: 60 }],
  developmentGoals: [GOAL],
  avatarVersion: null,
};

describe('PlayerView', () => {
  let httpMock: HttpTestingController;
  let harness: RouterTestingHarness;

  async function render(playerId = 'p-1', found = true, detail: PlayerDetail = CARMELA) {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'players/:id', component: PlayerView }]),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);

    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(`/players/${playerId}`);
    if (found) {
      httpMock.expectOne(`/api/players/${playerId}`).flush(detail);
      httpMock.expectOne('/api/player-skills').flush([SKILL, SKILL_2]);
      httpMock.expectOne('/api/player-development-goals').flush([GOAL, GOAL_2]);
    } else {
      httpMock
        .expectOne(`/api/players/${playerId}`)
        .flush({ error: 'not_found' }, { status: 404, statusText: 'Not Found' });
      // forkJoin cancels the catalog reads once the Player read fails; drain them unflushed.
      httpMock.match('/api/player-skills');
      httpMock.match('/api/player-development-goals');
    }
    harness.detectChanges();
    return harness.routeNativeElement as HTMLElement;
  }

  afterEach(() => httpMock.verify());

  // spec: ui.player-view-shows-profile
  it('shows the Player’s name, avatar, Line badges, Skill ratings and Development goals', async () => {
    const el = await render();
    const text = el.textContent as string;

    expect(el.querySelector('h1')?.textContent).toContain('Carmela');
    expect(el.querySelector('app-player-avatar')?.textContent?.trim()).toBe('C');
    expect(el.querySelector('.line-badge')?.getAttribute('href')).toBe('/lines?line=line-a');
    expect(text).toContain('Schusstechnik');
    expect(text).toContain('3/5');
    expect(text).toContain('Mehr Abschlüsse suchen');
  });

  // spec: ui.player-not-found
  it('shows a not-found page for an unknown Player', async () => {
    const el = await render('nobody', false);

    expect(el.textContent).toContain('Spieler nicht gefunden');
  });

  // spec: ui.player-rating-optimistic
  it('applies a rating click immediately and rolls it back if the save fails', async () => {
    const el = await render();

    (el.querySelector('[aria-label="Bewertung auf 5 von 5 setzen"]') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(el.textContent).toContain('5/5');

    const put = httpMock.expectOne('/api/players/p-1/skills/ps-1');
    expect(put.request.body).toEqual({ rating: 100 });
    put.flush({ error: 'bad' }, { status: 500, statusText: 'Server Error' });
    harness.detectChanges();

    expect(el.textContent).toContain('3/5');
  });

  // spec: ui.player-goal-toggle-saves-full-set
  it('saves the Player’s whole Development goal set when a goal chip is toggled', async () => {
    const el = await render();

    const manageButtons = Array.from(el.querySelectorAll('.section-title .btn')) as HTMLButtonElement[];
    manageButtons[1].click();
    harness.detectChanges();
    (el.querySelector('[aria-label="Konstanz über 60 Minuten hinzufügen"]') as HTMLButtonElement).click();

    const put = httpMock.expectOne('/api/players/p-1');
    expect(put.request.method).toBe('PUT');
    expect(new Set(put.request.body.developmentGoalIds)).toEqual(new Set(['pg-1', 'pg-2']));
    put.flush({ ...CARMELA, developmentGoals: [GOAL, GOAL_2] });
    harness.detectChanges();

    expect(el.querySelectorAll('.goal-item')).toHaveLength(2);
  });

  // spec: ui.create-player-skill-auto-associates
  it('creates a Player skill from the player screen and rates it on the Player straight away', async () => {
    const el = await render();

    (el.querySelector('.section-title .btn') as HTMLButtonElement).click();
    harness.detectChanges();
    const input = el.querySelector('.card.skills .create-row input') as HTMLInputElement;
    input.value = 'Kommunikation';
    input.dispatchEvent(new Event('input'));
    (el.querySelector('.card.skills .create-row') as HTMLFormElement).dispatchEvent(new Event('submit'));

    const post = httpMock.expectOne('/api/player-skills');
    expect(post.request.method).toBe('POST');
    expect(post.request.body.name).toBe('Kommunikation');
    post.flush({ id: 'ps-new', name: 'Kommunikation', color: post.request.body.color });

    const rate = httpMock.expectOne('/api/players/p-1/skills/ps-new');
    expect(rate.request.body).toEqual({ rating: 50 });
    rate.flush({ skillId: 'ps-new', name: 'Kommunikation', color: '#2c7a68', rating: 50 });
    harness.detectChanges();

    expect(el.textContent).toContain('Kommunikation');
  });

  describe('Avatar painter', () => {
    beforeEach(() => {
      // jsdom has no canvas: a context that swallows every call is enough to host the painter.
      const ctx = new Proxy({}, { get: () => jest.fn(() => ({})) });
      jest.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(ctx as never);
      jest.spyOn(HTMLCanvasElement.prototype, 'toDataURL').mockReturnValue('data:image/png;base64,' + btoa('png'));
    });

    afterEach(() => jest.restoreAllMocks());

    function clickButton(el: HTMLElement, label: string) {
      [...el.querySelectorAll('button')].find((b) => b.textContent?.trim() === label)!.click();
    }

    // spec: ui.avatar-painter-opens
    it('opens the painter from the Avatar', async () => {
      const el = await render();
      expect(el.querySelector('app-avatar-painter')).toBeNull();

      (el.querySelector('[aria-label="Profilbild malen"]') as HTMLButtonElement).click();
      harness.detectChanges();

      expect(el.querySelector('[role="dialog"]')?.textContent).toContain('Profilbild malen');
    });

    // spec: ui.avatar-save
    it('uploads the painting as a PNG, closes the painter and shows the new Avatar', async () => {
      const el = await render();
      (el.querySelector('[aria-label="Profilbild malen"]') as HTMLButtonElement).click();
      harness.detectChanges();

      clickButton(el, 'Speichern');
      await harness.fixture.whenStable();

      const put = httpMock.expectOne('/api/players/p-1/avatar');
      expect(put.request.method).toBe('PUT');
      expect(put.request.headers.get('Content-Type')).toBe('image/png');
      expect(put.request.body).toBeInstanceOf(Blob);
      put.flush({ ...CARMELA, avatarVersion: 7 });
      harness.detectChanges();

      expect(el.querySelector('app-avatar-painter')).toBeNull();
      expect(el.querySelector('.page-head img')?.getAttribute('src')).toBe('/api/players/p-1/avatar?v=7');
    });

    // spec: ui.avatar-save
    it('keeps the painter open with an error when the upload fails', async () => {
      const el = await render();
      (el.querySelector('[aria-label="Profilbild malen"]') as HTMLButtonElement).click();
      harness.detectChanges();

      clickButton(el, 'Speichern');
      await harness.fixture.whenStable();
      httpMock
        .expectOne('/api/players/p-1/avatar')
        .flush({ error: 'bad_request' }, { status: 400, statusText: 'Bad Request' });
      harness.detectChanges();

      expect(el.querySelector('app-avatar-painter [role="alert"]')?.textContent).toContain(
        'Profilbild konnte nicht gespeichert werden',
      );
    });

    // spec: ui.avatar-remove
    it('removes the Avatar and shows the initials again', async () => {
      const el = await render('p-1', true, { ...CARMELA, avatarVersion: 7 });
      jest.spyOn(window, 'confirm').mockReturnValue(true);
      (el.querySelector('[aria-label="Profilbild malen"]') as HTMLButtonElement).click();
      harness.detectChanges();

      clickButton(el, 'Entfernen');
      const del = httpMock.expectOne('/api/players/p-1/avatar');
      expect(del.request.method).toBe('DELETE');
      del.flush(null, { status: 204, statusText: 'No Content' });
      harness.detectChanges();

      expect(el.querySelector('app-avatar-painter')).toBeNull();
      expect(el.querySelector('.page-head img')).toBeNull();
      expect(el.querySelector('.page-head app-player-avatar')?.textContent?.trim()).toBe('C');
    });
  });
});
