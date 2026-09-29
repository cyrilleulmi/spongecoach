import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CurrentUserService } from '../../auth/current-user.service';
import { DrillSummary } from '../drill.model';
import { DrillList } from './drill-list';

const PASSEN = { id: 't-pass', name: 'Passen' };
const GOALIE = { id: 't-goalie', name: 'Mit Goalie' };
const DRILLS: DrillSummary[] = [
  { id: 'd-2', name: 'Slalom', status: 'NEEDS_INPUT', tags: [PASSEN], sketchCount: 1, updatedAt: '2026-09-27T11:00:00Z' },
  { id: 'd-1', name: 'Bresil', status: 'READY', tags: [PASSEN, GOALIE], sketchCount: 4, updatedAt: '2026-09-27T10:00:00Z' },
];

describe('DrillList', () => {
  let httpMock: HttpTestingController;

  async function render(role: 'COACH' | 'PLAYER' = 'COACH') {
    await TestBed.configureTestingModule({
      imports: [DrillList],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(CurrentUserService).me.set({ id: 'u', name: 'U', role, teamId: 'team', playerId: null, lineIds: [] });

    const fixture = TestBed.createComponent(DrillList);
    fixture.detectChanges();
    httpMock.expectOne('/api/drills').flush(DRILLS);
    httpMock.expectOne('/api/drill-tags').flush([GOALIE, PASSEN]);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => httpMock.verify());

  // spec: ui.drill-list
  it('lists every Drill with its status, tags and photo count, in the order the API gives', async () => {
    const fixture = await render();
    const rows = Array.from(fixture.nativeElement.querySelectorAll('.drill-row')) as HTMLElement[];

    expect(rows.map((row) => row.querySelector('.drill-name')?.textContent?.trim())).toEqual(['Slalom', 'Bresil']);
    expect(rows[0].querySelector('.status')?.textContent?.trim()).toBe('Rückfrage');
    expect(rows[1].querySelector('.drill-meta')?.textContent?.trim()).toBe('4 Fotos');
    expect(rows[1].querySelectorAll('.tag')).toHaveLength(2);
    expect(rows[1].querySelector('.drill-link')?.getAttribute('href')).toBe('/uebungen/d-1');
  });

  // spec: ui.drill-list-filter
  it('keeps only the Drills carrying every picked tag', async () => {
    const fixture = await render();
    const goalieChip = Array.from(fixture.nativeElement.querySelectorAll('.tag-chip')).find(
      (chip) => (chip as HTMLElement).textContent?.trim() === 'Mit Goalie',
    ) as HTMLButtonElement;

    goalieChip.click();
    fixture.detectChanges();

    const names = Array.from(fixture.nativeElement.querySelectorAll('.drill-name')).map((n) => (n as HTMLElement).textContent?.trim());
    expect(names).toEqual(['Bresil']);
    expect(goalieChip.getAttribute('aria-pressed')).toBe('true');
  });

  it('offers a coach to upload a new Drill', async () => {
    const fixture = await render('COACH');

    expect(fixture.nativeElement.querySelector('.primary-action')?.getAttribute('href')).toBe('/uebungen/neu');
  });

  // spec: ui.drill-player-read-only
  it('lists the Drills for a Player without offering an upload', async () => {
    const fixture = await render('PLAYER');

    expect(fixture.nativeElement.querySelectorAll('.drill-row')).toHaveLength(2);
    expect(fixture.nativeElement.querySelector('.primary-action')).toBeNull();
  });
});
