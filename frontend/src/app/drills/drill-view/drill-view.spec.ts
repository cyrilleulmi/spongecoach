import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { CurrentUserService } from '../../auth/current-user.service';
import { DrillEditor } from '../drill-editor/drill-editor';
import { testDrill, testStage } from '../drill-fixtures';
import { DrillDetail } from '../drill.model';
import { DrillView, POLL_MS } from './drill-view';

// Decoding and re-encoding a photo needs a real canvas; the maths is covered in image-prep.spec.ts.
jest.mock('../image-prep', () => ({
  ...jest.requireActual('../image-prep'),
  prepareSketch: jest.fn(async () => new Blob(['jpeg'], { type: 'image/jpeg' })),
}));

describe('DrillView', () => {
  let httpMock: HttpTestingController;

  async function render(drill: DrillDetail, role: 'COACH' | 'PLAYER' = 'COACH'): Promise<ComponentFixture<DrillView>> {
    await TestBed.configureTestingModule({
      imports: [DrillView],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: drill.id }) } } },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(CurrentUserService).me.set({ id: 'u', name: 'U', role, teamId: 'team', playerId: null, lineIds: [] });

    const fixture = TestBed.createComponent(DrillView);
    fixture.detectChanges();
    httpMock.expectOne(`/api/drills/${drill.id}`).flush(drill);
    fixture.detectChanges();
    return fixture;
  }

  function el(fixture: ComponentFixture<DrillView>, selector: string): HTMLElement {
    return fixture.nativeElement.querySelector(selector) as HTMLElement;
  }

  afterEach(() => {
    httpMock.verify();
    jest.useRealTimers();
  });

  // spec: ui.drill-waits-while-interpreting
  it('says Claude is reading, and checks again until the Drill is done', async () => {
    jest.useFakeTimers();
    const fixture = await render(testDrill({ status: 'PENDING', script: null, currentVersion: null }));

    expect(el(fixture, '.working').textContent).toContain('Claude liest die Skizzen');

    jest.advanceTimersByTime(POLL_MS);
    httpMock.expectOne('/api/drills/d-1').flush(testDrill({ status: 'PENDING', script: null, currentVersion: null }));
    jest.advanceTimersByTime(POLL_MS);
    httpMock.expectOne('/api/drills/d-1').flush(testDrill());
    fixture.detectChanges();

    expect(el(fixture, '.working')).toBeNull();
    expect(el(fixture, 'app-rink-player')).not.toBeNull();
    jest.advanceTimersByTime(POLL_MS * 3);
    httpMock.expectNone('/api/drills/d-1');
  });

  // spec: ui.drill-questions
  it('offers the suggested answers and sends the chosen one', async () => {
    const question = { id: 'q1', text: 'Sind die Kreise Verteidigerinnen?', options: ['Verteidigerinnen', 'Hütchen'], sketch: 1, symbolIds: ['s1-1'] };
    const fixture = await render(testDrill({ status: 'NEEDS_INPUT', openQuestions: [question] }));

    const option = Array.from(fixture.nativeElement.querySelectorAll('.option')).find(
      (o) => (o as HTMLElement).textContent?.trim() === 'Hütchen',
    ) as HTMLButtonElement;
    option.click();
    fixture.detectChanges();
    el(fixture, '.send-answers').click();

    const request = httpMock.expectOne('/api/drills/d-1/answers');
    expect(request.request.body).toEqual({ answers: [{ questionId: 'q1', text: 'Hütchen' }], guess: false, message: null });
    request.flush(testDrill({ status: 'PENDING' }));
  });

  // spec: ui.drill-questions
  it('lets Claude decide with "Rate einfach"', async () => {
    const question = { id: 'q1', text: 'Welche Reihenfolge?', options: [], sketch: 0, symbolIds: [] };
    const fixture = await render(testDrill({ status: 'NEEDS_INPUT', openQuestions: [question] }));

    el(fixture, '.guess').click();

    const request = httpMock.expectOne('/api/drills/d-1/answers');
    expect(request.request.body).toMatchObject({ answers: [], guess: true });
    request.flush(testDrill());
  });

  // spec: ui.drill-failed-retry
  it('shows why the interpretation failed, and tries again', async () => {
    const fixture = await render(testDrill({ status: 'FAILED', error: 'Claude ist nicht erreichbar.', script: null, currentVersion: null }));

    expect(el(fixture, '.failed').textContent).toContain('Claude ist nicht erreichbar.');
    el(fixture, '.retry').click();

    httpMock.expectOne({ method: 'POST', url: '/api/drills/d-1/retry' }).flush(testDrill());
  });

  // spec: ui.drill-plays
  it('plays the Stage on the rink, with its Actors, ball, cone and faint Step paths', async () => {
    const fixture = await render(testDrill());

    expect(fixture.nativeElement.querySelectorAll('app-rink-player .actor')).toHaveLength(2);
    expect(fixture.nativeElement.querySelectorAll('app-rink-player .ball')).toHaveLength(1);
    expect(fixture.nativeElement.querySelectorAll('app-rink-player .prop--cone')).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('app-rink-player .path--pass')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-rink-player .path--shot')).not.toBeNull();
  });

  // spec: ui.drill-stages
  it('shows each Stage as a tab and plays the chosen one', async () => {
    const second = testStage('2 gegen 1', 's2');
    const fixture = await render(testDrill({ script: { stages: [testStage('1 gegen Goalie'), second], assumptions: [] } }));

    const tabs = Array.from(fixture.nativeElement.querySelectorAll('.stage-tab')) as HTMLButtonElement[];
    expect(tabs.map((t) => t.textContent?.trim())).toEqual(['1. 1 gegen Goalie', '2. 2 gegen 1']);
    tabs[1].click();
    fixture.detectChanges();

    expect(el(fixture, '.stage-name').textContent?.trim()).toBe('2 gegen 1');
    expect(tabs[1].getAttribute('aria-selected')).toBe('true');
  });

  // spec: ui.drill-readings
  it('shows what Claude read on a photo, and highlights what a question is about', async () => {
    const question = { id: 'q1', text: 'Wer ist das?', options: [], sketch: 1, symbolIds: ['s1-1'] };
    const fixture = await render(testDrill({ status: 'NEEDS_INPUT', openQuestions: [question] }));

    expect(el(fixture, '.reading')).toBeNull();
    el(fixture, '.reading-toggle').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.reading .symbol')).toHaveLength(1);

    el(fixture, '.reading-toggle').click();
    el(fixture, '.question').dispatchEvent(new Event('mouseenter'));
    fixture.detectChanges();
    expect(el(fixture, '.sketch').classList).toContain('sketch--asked');
    expect(el(fixture, '.reading .symbol--highlight')).not.toBeNull();
  });

  // spec: ui.drill-chat-correction
  it('sends a correction and shows the reply with its version', async () => {
    const fixture = await render(testDrill());

    const input = el(fixture, '.chat-input') as HTMLInputElement;
    input.value = 'Die Verteidigerin soll früher starten';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    el(fixture, '.chat-send').click();

    const request = httpMock.expectOne('/api/drills/d-1/chat');
    expect(request.request.body).toEqual({ message: 'Die Verteidigerin soll früher starten' });
    request.flush(
      testDrill({
        currentVersion: 2,
        versions: [
          { version: 2, source: 'AI', changeSummary: 'Verteidigerin startet früher', createdAt: '' },
          { version: 1, source: 'AI', changeSummary: null, createdAt: '' },
        ],
        messages: [
          ...testDrill().messages,
          { position: 2, author: 'COACH', content: 'Die Verteidigerin soll früher starten', questions: null, answers: null, scriptVersion: null, createdAt: '' },
          { position: 3, author: 'INTERPRETER', content: 'Angepasst.', questions: null, answers: null, scriptVersion: 2, createdAt: '' },
        ],
      }),
    );
    fixture.detectChanges();

    const messages = Array.from(fixture.nativeElement.querySelectorAll('.chat-message')) as HTMLElement[];
    expect(messages[1].textContent).toContain('Die Verteidigerin soll früher starten');
    expect(messages[2].textContent?.replace(/\s+/g, ' ')).toContain('Version 2 · Verteidigerin startet früher');
  });

  // spec: ui.drill-revert
  it('goes back to the previous version with "Rückgängig"', async () => {
    const fixture = await render(
      testDrill({
        currentVersion: 2,
        versions: [
          { version: 2, source: 'EDIT', changeSummary: 'Von Hand angepasst', createdAt: '' },
          { version: 1, source: 'AI', changeSummary: null, createdAt: '' },
        ],
      }),
    );

    el(fixture, '.undo').click();

    httpMock.expectOne({ method: 'POST', url: '/api/drills/d-1/revert/1' }).flush(testDrill({ currentVersion: 3 }));
  });

  // spec: ui.drill-editor
  it('saves a hand edit as a new version and goes back to playing it', async () => {
    const fixture = await render(testDrill());

    el(fixture, '.edit-button').click();
    fixture.detectChanges();
    const editor = fixture.debugElement.query(By.directive(DrillEditor)).componentInstance as DrillEditor;
    editor.saved.emit({ script: { stages: [testStage('Handarbeit')], assumptions: [] }, changeSummary: 'Tempo angepasst' });

    const request = httpMock.expectOne('/api/drills/d-1/script');
    expect(request.request.body.changeSummary).toBe('Tempo angepasst');
    expect(request.request.body.script.stages[0].name).toBe('Handarbeit');
    request.flush(testDrill({ currentVersion: 2 }));
    fixture.detectChanges();

    expect(el(fixture, 'app-drill-editor')).toBeNull();
    expect(el(fixture, 'app-rink-player')).not.toBeNull();
  });

  it('keeps the editor open with the reason when a hand edit is refused', async () => {
    const fixture = await render(testDrill());
    el(fixture, '.edit-button').click();
    fixture.detectChanges();
    const editor = fixture.debugElement.query(By.directive(DrillEditor)).componentInstance as DrillEditor;

    editor.saved.emit({ script: { stages: [testStage()], assumptions: [] }, changeSummary: 'x' });
    httpMock
      .expectOne('/api/drills/d-1/script')
      .flush({ error: 'bad_request', message: 'script is not playable: off the rink' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(el(fixture, 'app-drill-editor .error').textContent).toContain('off the rink');
  });

  // spec: ui.drill-player-draws
  it('lets a Player watch and edit by hand, but offers nothing that starts Claude', async () => {
    const question = { id: 'q1', text: 'Wer ist das?', options: ['A'], sketch: 1, symbolIds: [] };
    const fixture = await render(testDrill({ status: 'NEEDS_INPUT', openQuestions: [question] }), 'PLAYER');

    expect(el(fixture, 'app-rink-player')).not.toBeNull();
    expect(el(fixture, '.player-questions').textContent).toContain('Wer ist das?');
    for (const selector of ['app-drill-questions', '.chat-input', '.retry', '.work-in-photos']) {
      expect(el(fixture, selector)).toBeNull();
    }
    for (const selector of ['.edit-button', '.rename-button', '.delete-button', '.add-photos', '.remove-sketch']) {
      expect(el(fixture, selector)).not.toBeNull();
    }
  });

  // spec: ui.drill-player-draws
  it('lets a Player go back to an earlier version', async () => {
    const fixture = await render(
      testDrill({
        currentVersion: 2,
        versions: [
          { version: 2, source: 'EDIT', changeSummary: 'Von Hand angepasst', createdAt: '' },
          { version: 1, source: 'AI', changeSummary: null, createdAt: '' },
        ],
      }),
      'PLAYER',
    );

    el(fixture, '.undo').click();

    httpMock.expectOne({ method: 'POST', url: '/api/drills/d-1/revert/1' }).flush(testDrill({ currentVersion: 3 }));
  });

  // spec: ui.drill-photos
  it('removes a photo, lists it under the removed ones, and restores it', async () => {
    const fixture = await render(testDrill());

    el(fixture, '.remove-sketch').click();
    httpMock.expectOne({ method: 'DELETE', url: '/api/drills/d-1/sketches/1' }).flush(
      testDrill({ sketches: [], deletedSketches: [testDrill().sketches[0]] }),
    );
    fixture.detectChanges();

    expect(el(fixture, '.no-photos')).not.toBeNull();
    expect(el(fixture, '.removed-sketches summary').textContent).toContain('Gelöschte Fotos (1)');
    el(fixture, '.restore-sketch').click();
    httpMock.expectOne({ method: 'POST', url: '/api/drills/d-1/sketches/1/restore' }).flush(testDrill());
    fixture.detectChanges();

    expect(el(fixture, '.removed-sketches')).toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.sketch')).toHaveLength(1);
  });

  // spec: ui.drill-photos
  it('adds photos without starting Claude, then offers a Coach to have Claude work them in', async () => {
    const fixture = await render(testDrill());
    const input = el(fixture, '.file-input') as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [new File(['x'], 'neu.jpg', { type: 'image/jpeg' })], configurable: true });

    input.dispatchEvent(new Event('change'));
    await new Promise((resolve) => setTimeout(resolve));
    const request = httpMock.expectOne({ method: 'POST', url: '/api/drills/d-1/sketches' });
    expect((request.request.body as FormData).getAll('sketches')).toHaveLength(1);
    request.flush(testDrill({ sketches: [...testDrill().sketches, { position: 2, note: null, reading: null }] }));
    fixture.detectChanges();

    el(fixture, '.work-in-photos').click();
    const chat = httpMock.expectOne('/api/drills/d-1/chat');
    expect(chat.request.body.message).toContain('das neue Foto 2');
    chat.flush(testDrill({ status: 'PENDING' }));
  });

  // spec: ui.drill-list
  it('says a Drill without photos was drawn by hand', async () => {
    const fixture = await render(testDrill({ sketches: [], deletedSketches: [] }));

    expect(el(fixture, '.tag--drawn').textContent).toContain('gezeichnet');
    expect(el(fixture, '.no-photos')).not.toBeNull();
  });

  it('says so when the Drill does not exist', async () => {
    await TestBed.configureTestingModule({
      imports: [DrillView],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: 'gone' }) } } },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DrillView);
    fixture.detectChanges();
    httpMock.expectOne('/api/drills/gone').flush({ error: 'not_found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Übung nicht gefunden');
  });
});
