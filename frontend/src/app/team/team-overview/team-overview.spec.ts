import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TeamOverview } from './team-overview';
import { Iteration } from '../iteration.model';
import { LineDetail } from '../../lines/line.model';

const LINES = [
  { id: 'line-a', name: 'Kiwi', playerCount: 4 },
  { id: 'line-b', name: 'Bäri', playerCount: 4 },
];

const EVENT_TYPES = [
  { id: 'tt', name: 'Training' },
  { id: 'mt', name: 'Match' },
];

function iterations(): Iteration[] {
  return [
    {
      id: 'it-1',
      name: 'Vorbereitung',
      position: 1,
      events: [
        {
          id: 'ev-1',
          typeId: 'tt',
          type: 'Training',
          name: null,
          position: 1,
          scheduledOn: null,
          focusAttachments: [
            { lineId: 'line-a', lineName: 'Kiwi', focusId: 'f-1', focusName: 'Fokus A' },
          ],
        },
        {
          id: 'ev-2',
          typeId: 'mt',
          type: 'Match',
          name: 'Testspiel',
          position: 2,
          scheduledOn: '2026-09-12',
          focusAttachments: [],
        },
      ],
    },
    { id: 'it-2', name: 'Hinrunde', position: 2, events: [] },
  ];
}

function detail(id: string, focuses: LineDetail['focuses']): LineDetail {
  return { id, name: id, players: [], skills: [], developmentGoals: [], focuses };
}

describe('TeamOverview', () => {
  let httpMock: HttpTestingController;

  async function render() {
    await TestBed.configureTestingModule({
      imports: [TeamOverview],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(TeamOverview);
    fixture.detectChanges();

    httpMock.expectOne('/api/iterations').flush(iterations());
    httpMock.expectOne('/api/event-types').flush(EVENT_TYPES);
    httpMock.expectOne('/api/lines').flush(LINES);
    httpMock
      .expectOne('/api/lines/line-a')
      .flush(detail('line-a', [{ id: 'f-1', name: 'Fokus A', goalIds: [] }, { id: 'f-2', name: 'Fokus B', goalIds: [] }]));
    httpMock.expectOne('/api/lines/line-b').flush(detail('line-b', [{ id: 'f-3', name: 'Fokus C', goalIds: [] }]));

    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => httpMock.verify());

  it('loads every iteration and renders the first one as a dial timeline', async () => {
    const fixture = await render();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Vorbereitung');
    expect(text).toContain('Iteration 1 von 2');
    expect(fixture.nativeElement.querySelectorAll('.d-node').length).toBe(2);
  });

  it('marks the first incomplete event as the next one', async () => {
    const fixture = await render();
    const first = fixture.nativeElement.querySelector('.d-node') as HTMLElement;
    expect(first.classList).toContain('current');
    expect(first.textContent).toContain('NÄCHSTES');
  });

  it('when a line focus is changed, PUTs the merged attachment set and reloads', async () => {
    const fixture = await render();

    // ev-1 is the default selection (it is "next"); change Bäri's focus to f-3.
    const lineBSelect = fixture.nativeElement.querySelectorAll('.focus-select')[1] as HTMLSelectElement;
    lineBSelect.value = 'f-3';
    lineBSelect.dispatchEvent(new Event('change'));

    const put = httpMock.expectOne('/api/events/ev-1');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({
      focusAttachments: [
        { lineId: 'line-a', focusId: 'f-1' },
        { lineId: 'line-b', focusId: 'f-3' },
      ],
    });
    put.flush({ ...iterations()[0].events[0], focusAttachments: [] });

    httpMock.expectOne('/api/iterations').flush(iterations());
    await fixture.whenStable();
  });

  it('adds a training to the current iteration and reloads', async () => {
    const fixture = await render();

    const addTraining = Array.from(
      fixture.nativeElement.querySelectorAll('.add-event-row .btn'),
    ).find((b) => (b as HTMLElement).textContent?.includes('Training')) as HTMLButtonElement;
    addTraining.click();

    const post = httpMock.expectOne('/api/iterations/it-1/events');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ eventTypeId: 'tt' });
    post.flush({
      id: 'ev-new',
      typeId: 'tt',
      type: 'Training',
      name: null,
      position: 3,
      scheduledOn: null,
      focusAttachments: [],
    });

    httpMock.expectOne('/api/iterations').flush(iterations());
    await fixture.whenStable();
  });

  it('creates a new iteration and jumps to it', async () => {
    const fixture = await render();

    const input = fixture.nativeElement.querySelector('.add-iteration input') as HTMLInputElement;
    input.value = 'Rückrunde';
    input.dispatchEvent(new Event('input'));
    (fixture.nativeElement.querySelector('.add-iteration button') as HTMLButtonElement).click();

    const post = httpMock.expectOne('/api/iterations');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'Rückrunde' });
    const created = { id: 'it-3', name: 'Rückrunde', position: 3, events: [] };
    post.flush(created);

    httpMock.expectOne('/api/iterations').flush([...iterations(), created]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string)).toContain('Iteration 3 von 3');
  });
});
