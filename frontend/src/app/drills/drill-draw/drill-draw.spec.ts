import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { addActor, blankScript } from '../drill-authoring';
import { DrillEditor } from '../drill-editor/drill-editor';
import { DrillRink } from '../drill-rink/drill-rink';
import { DRAFT_KEY, DrillDraw } from './drill-draw';

const TAGS = [
  { id: 't-pass', name: 'Passen' },
  { id: 't-goalie', name: 'Mit Goalie' },
];

describe('DrillDraw', () => {
  let httpMock: HttpTestingController;

  async function render(): Promise<ComponentFixture<DrillDraw>> {
    await TestBed.configureTestingModule({
      imports: [DrillDraw],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(DrillDraw);
    fixture.detectChanges();
    httpMock.expectOne('/api/drill-tags').flush(TAGS);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  function rink(fixture: ComponentFixture<DrillDraw>): DrillRink {
    return fixture.debugElement.query(By.directive(DrillRink)).componentInstance as DrillRink;
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => httpMock.verify());

  // spec: ui.drill-draw-save
  it('asks for a name and tags only when saving, then creates the Drill and opens it', async () => {
    const fixture = await render();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    rink(fixture).rinkTapped.emit({ x: -4, y: -6 });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.save') as HTMLButtonElement).click();
    fixture.detectChanges();

    const name = fixture.nativeElement.querySelector('.name-input') as HTMLInputElement;
    name.value = 'Doppelpass';
    name.dispatchEvent(new Event('input'));
    (Array.from(fixture.nativeElement.querySelectorAll('.tag-chip')) as HTMLButtonElement[])[0].click();
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.create') as HTMLButtonElement).click();

    const request = httpMock.expectOne({ method: 'POST', url: '/api/drills/drawn' });
    expect(request.request.body).toMatchObject({ name: 'Doppelpass', tagIds: ['t-pass'], changeSummary: 'Gezeichnet' });
    expect(request.request.body.script.stages[0].actors).toHaveLength(1);
    request.flush({ id: 'd-7' });
    expect(navigate).toHaveBeenCalledWith(['/uebungen', 'd-7']);
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
  });

  // spec: ui.drill-draw-save
  it('needs a name before it creates anything', async () => {
    const fixture = await render();
    rink(fixture).rinkTapped.emit({ x: 0, y: 0 });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.save') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect((fixture.nativeElement.querySelector('.create') as HTMLButtonElement).disabled).toBe(true);
    httpMock.expectNone('/api/drills/drawn');
  });

  // spec: ui.drill-draw-save
  it('shows why the server refused the script, and keeps the drawing', async () => {
    const fixture = await render();
    rink(fixture).rinkTapped.emit({ x: 0, y: 0 });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.save') as HTMLButtonElement).click();
    const name = fixture.nativeElement.querySelector('.name-input') as HTMLInputElement;
    name.value = 'X';
    name.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.create') as HTMLButtonElement).click();

    httpMock.expectOne('/api/drills/drawn').flush({ error: 'bad_request', message: 'script is not playable: off the rink' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.save-form .error').textContent).toContain('off the rink');
    expect(localStorage.getItem(DRAFT_KEY)).not.toBeNull();
  });

  // spec: ui.drill-draw-draft
  it('keeps the drawing as a draft in the browser, and offers it again after a reload', async () => {
    const fixture = await render();
    rink(fixture).rinkTapped.emit({ x: -4, y: -6 });
    fixture.detectChanges();

    const stored = JSON.parse(localStorage.getItem(DRAFT_KEY)!);
    expect(stored.stages[0].actors).toHaveLength(1);
    TestBed.resetTestingModule();

    const again = await render();
    expect(again.nativeElement.querySelector('.draft-banner')).not.toBeNull();
    expect(again.debugElement.query(By.directive(DrillEditor)).componentInstance.script()).toEqual(stored);
    (again.nativeElement.querySelector('.discard-draft') as HTMLButtonElement).click();
    again.detectChanges();
    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
    expect(again.nativeElement.querySelector('.draft-banner')).toBeNull();
  });

  // spec: ui.drill-draw-draft
  it('ignores a draft it cannot read, and does not keep an empty drawing', async () => {
    localStorage.setItem(DRAFT_KEY, '{not json');
    const fixture = await render();

    expect(fixture.nativeElement.querySelector('.draft-banner')).toBeNull();
    const script = blankScript();
    addActor(script, script.stages[0], 'PLAYER', 'A', { x: 0, y: 0 });
    rink(fixture).rinkTapped.emit({ x: 0, y: 0 });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.undo') as HTMLButtonElement).click();

    expect(localStorage.getItem(DRAFT_KEY)).toBeNull();
  });
});
