import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrentUserService } from '../../auth/current-user.service';
import { CurrentUser } from '../../auth/user.model';
import { TeamOverview } from './team-overview';
import { Iteration } from '../iteration.model';

const LINES = [
  { id: 'line-a', name: 'Kiwi', playerCount: 4, color: '#4c8c3d' },
  { id: 'line-b', name: 'Bäri', playerCount: 4, color: '#8b5e34' },
];

const LINE_REFS = [
  { id: 'line-a', name: 'Kiwi', color: '#4c8c3d' },
  { id: 'line-b', name: 'Bäri', color: '#8b5e34' },
];

const EVENT_TYPES = [
  { id: 'tt', name: 'Training' },
  { id: 'mt', name: 'Match' },
];

// Far-future dates keep both events "not done" for as long as this suite is maintained.
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
          scheduledOn: '2099-08-05T18:00',
          focusAttachments: [{ lineId: 'line-a', lineName: 'Kiwi', focus: 'Fokus A' }],
          lines: LINE_REFS,
          attendance: [],
        },
        {
          id: 'ev-2',
          typeId: 'mt',
          type: 'Match',
          name: 'Testspiel',
          scheduledOn: '2099-08-12T19:00',
          focusAttachments: [],
          lines: LINE_REFS,
          attendance: [],
        },
      ],
    },
    { id: 'it-2', name: 'Hinrunde', position: 2, events: [] },
  ];
}

describe('TeamOverview', () => {
  let httpMock: HttpTestingController;

  async function render(me: CurrentUser | null = null, data: Iteration[] = iterations()) {
    await TestBed.configureTestingModule({
      imports: [TeamOverview],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(CurrentUserService).me.set(me);

    const fixture = TestBed.createComponent(TeamOverview);
    fixture.detectChanges();

    httpMock.expectOne('/api/iterations').flush(data);
    httpMock.expectOne('/api/event-types').flush(EVENT_TYPES);
    httpMock.expectOne('/api/lines').flush(LINES);

    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => httpMock.verify());

  // spec: ui.timeline-renders-iteration
  it('loads every iteration and renders the first one as a dial timeline', async () => {
    const fixture = await render();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Vorbereitung');
    expect(text).toContain('Iteration 1 von 2');
    expect(fixture.nativeElement.querySelectorAll('.d-node').length).toBe(2);
  });

  // spec: ui.opens-on-next-event
  it('opens on the Iteration holding the next Event, not the first one', async () => {
    const [prep, season] = iterations();
    const pastPrep = { ...prep, events: prep.events.map((e) => ({ ...e, scheduledOn: '2020-01-01T18:00' })) };
    const upcomingSeason = { ...season, events: [{ ...prep.events[0], id: 'ev-9', scheduledOn: '2099-09-01T18:00' }] };
    const fixture = await render(null, [pastPrep, upcomingSeason]);

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Iteration 2 von 2');
    expect(fixture.nativeElement.querySelector('.d-node.current')).not.toBeNull();
  });

  it('opens on the latest Iteration once every Event has passed', async () => {
    const [prep, season] = iterations();
    const pastPrep = { ...prep, events: prep.events.map((e) => ({ ...e, scheduledOn: '2020-01-01T18:00' })) };
    const fixture = await render(null, [pastPrep, season]);
    expect(fixture.nativeElement.textContent).toContain('Iteration 2 von 2');
  });

  // spec: ui.next-event-marked
  it('marks the first not-yet-past event as the next one', async () => {
    const fixture = await render();
    const first = fixture.nativeElement.querySelector('.d-node') as HTMLElement;
    expect(first.classList).toContain('current');
    expect(first.textContent).toContain('NÄCHSTES');
  });

  // spec: ui.set-focus-from-detail
  it("when a line focus is changed, PUTs that Line's Focus alone and reloads", async () => {
    const fixture = await render();

    // ev-1 is the default selection (it is "next"); change Bäri's focus.
    const lineBInput = fixture.nativeElement.querySelectorAll('.focus-input')[1] as HTMLInputElement;
    lineBInput.value = 'Abschlussübungen 2-auf-1';
    lineBInput.dispatchEvent(new Event('change'));

    const put = httpMock.expectOne('/api/events/ev-1/focus/line-b');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ focus: 'Abschlussübungen 2-auf-1' });
    put.flush({ ...iterations()[0].events[0], focusAttachments: [] });

    httpMock.expectOne('/api/iterations').flush(iterations());
    await fixture.whenStable();
  });

  // spec: ui.same-focus-again
  it('offers "same focus again" for a line with an earlier focus, copying it in as plain text', async () => {
    const fixture = await render();

    // ev-2 (Testspiel) has no focus set; ev-1 (earlier) set "Fokus A" for line-a.
    const dNodes = fixture.nativeElement.querySelectorAll('.d-node');
    (dNodes[1] as HTMLElement).click();
    fixture.detectChanges();

    const sameFocusButtons = fixture.nativeElement.querySelectorAll(
      '.same-focus-btn',
    ) as NodeListOf<HTMLButtonElement>;
    expect(sameFocusButtons.length).toBe(1); // only line-a has an earlier focus
    sameFocusButtons[0].click();

    const put = httpMock.expectOne('/api/events/ev-2/focus/line-a');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ focus: 'Fokus A' });
    put.flush({ ...iterations()[0].events[1], focusAttachments: [{ lineId: 'line-a', lineName: 'Kiwi', focus: 'Fokus A' }] });

    httpMock.expectOne('/api/iterations').flush(iterations());
    await fixture.whenStable();
  });

  // spec: ui.add-training
  it('adds a training to the current iteration and reloads', async () => {
    const fixture = await render();

    const addTraining = Array.from(
      fixture.nativeElement.querySelectorAll('.add-event-row .btn'),
    ).find((b) => (b as HTMLElement).textContent?.includes('Training')) as HTMLButtonElement;
    addTraining.click();

    const post = httpMock.expectOne('/api/iterations/it-1/events');
    expect(post.request.method).toBe('POST');
    // one hour after the iteration's latest event (ev-2 at 2099-08-12T19:00)
    expect(post.request.body).toEqual({ eventTypeId: 'tt', scheduledOn: '2099-08-12T20:00' });
    post.flush({
      id: 'ev-new',
      typeId: 'tt',
      type: 'Training',
      name: null,
      scheduledOn: '2026-08-12T20:00',
      focusAttachments: [],
    });

    httpMock.expectOne('/api/iterations').flush(iterations());
    await fixture.whenStable();
  });

  // spec: ui.create-iteration
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
  // spec: ui.player-cannot-plan-timeline
  it('hides Iteration and Event planning from a Player', async () => {
    const carmela: CurrentUser = {
      id: 'u-carmela', name: 'Carmela', role: 'PLAYER', teamId: 'team', playerId: 'p-1', lineIds: ['line-a'],
    };
    const fixture = await render(carmela);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('.iteration-tools')).toBeNull();
    expect(el.querySelector('.add-event-row')).toBeNull();
    expect(el.querySelector('.add-iteration')).toBeNull();
    expect(el.querySelector('app-iteration-selector')).not.toBeNull();
  });
});
