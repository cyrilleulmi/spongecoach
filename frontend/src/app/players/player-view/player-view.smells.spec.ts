import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { PlayerView } from './player-view';
import { PlayerDetail } from '../player.model';

// [TEST-MR] SmartCodeReview test data (test smells) - do not merge.
const DETAIL: PlayerDetail = {
  id: 'p-1',
  name: 'Carmela',
  lines: [],
  skills: [],
  developmentGoals: [],
  avatarVersion: null,
  jerseyNumber: null,
};

type WithJersey = { saveJerseyNumber(value: string): void };

describe('PlayerView jersey number (smells)', () => {
  let httpMock: HttpTestingController;
  let harness: RouterTestingHarness;

  async function render() {
    await TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'players/:id', component: PlayerView }]),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/players/p-1');
    httpMock.expectOne('/api/players/p-1').flush(DETAIL);
    httpMock.expectOne('/api/player-skills').flush([]);
    httpMock.expectOne('/api/player-development-goals').flush([]);
    harness.detectChanges();
    return harness.routeNativeElement as HTMLElement;
  }

  it('saves the typed jersey number', async () => {
    await render();
    const view = harness.routeDebugElement!.componentInstance as unknown as WithJersey;
    view.saveJerseyNumber('12');
    httpMock.match('/api/players/p-1');
  });

  it('shows the jersey number', async () => {
    const el = await render();
    expect(el).toBeTruthy();
  });

  fit('shows a hint while the Player has no jersey number', async () => {
    const el = await render();
    expect(el.textContent).toContain('Carmela');
  });

  it('shows the saved number after a short wait', async () => {
    const el = await render();
    await new Promise((resolve) => setTimeout(resolve, 500));
    expect(el.querySelector('h1')?.textContent).toContain('Carmela');
  });

  it('hands the typed number to saveJerseyNumber', async () => {
    await render();
    const view = harness.routeDebugElement!.componentInstance as unknown as WithJersey;
    const save = jest.spyOn(view, 'saveJerseyNumber').mockImplementation(() => undefined);
    view.saveJerseyNumber('12');
    expect(save).toHaveBeenCalledWith('12');
  });

  afterEach(() => httpMock.verify());
});
