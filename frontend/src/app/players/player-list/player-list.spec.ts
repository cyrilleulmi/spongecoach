import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PlayerList } from './player-list';
import { PlayerSummary } from '../player.model';

const PLAYERS: PlayerSummary[] = [
  { id: 'p-1', name: 'Carmela', lines: [{ id: 'line-a', name: 'Kiwi', color: '#4c8c3d' }], avatarVersion: null },
  { id: 'p-2', name: 'Stocki', lines: [], avatarVersion: null },
];

describe('PlayerList', () => {
  let httpMock: HttpTestingController;

  async function render() {
    await TestBed.configureTestingModule({
      imports: [PlayerList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(PlayerList);
    fixture.detectChanges();
    httpMock.expectOne('/api/players').flush(PLAYERS);
    await fixture.whenStable();
    return fixture;
  }

  afterEach(() => httpMock.verify());

  // spec: ui.player-list
  it('lists every Player with avatar, name and Line badges, each linking to the player screen', async () => {
    const fixture = await render();
    const rows = Array.from(fixture.nativeElement.querySelectorAll('.player-row')) as HTMLElement[];

    expect(rows).toHaveLength(2);
    const carmela = rows[0];
    expect(carmela.querySelector('.player-link')?.getAttribute('href')).toBe('/players/p-1');
    expect(carmela.querySelector('app-player-avatar')?.textContent?.trim()).toBe('C');
    expect(carmela.querySelector('.line-badge')?.textContent?.trim()).toBe('Kiwi');
    expect(rows[1].querySelector('.line-badge')).toBeNull();
  });

  // spec: ui.line-badge-jumps-to-line
  it('links a Line badge to the line screen with that Line selected', async () => {
    const fixture = await render();
    const badge = fixture.nativeElement.querySelector('.line-badge') as HTMLAnchorElement;

    expect(badge.getAttribute('href')).toBe('/lines?line=line-a');
  });
});
