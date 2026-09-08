import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LineOverview } from './line-overview';
import { LineDetail } from '../line.model';

const LINE_A = { id: 'line-a', name: 'Kiwi', playerCount: 1 };
const LINE_B = { id: 'line-b', name: 'Bäri', playerCount: 0 };
const PLAYERS = [
  { id: 'player-1', name: 'Carmela' },
  { id: 'player-2', name: 'Debi' },
];
const SKILL = { id: 'skill-1', name: 'Passgenauigkeit', color: '#2c7a68' };
const GOAL = { id: 'goal-1', name: 'Ballverluste im eigenen Drittel reduzieren', color: '#c8722e' };
const GOAL_2 = { id: 'goal-2', name: 'Kompakte Defensive aufbauen', color: '#16a085' };
const FOCUS = { id: 'focus-1', name: 'Cross-Pässe unter Druck', goalIds: ['goal-1', 'goal-2'] };

function detailFor(line: { id: string; name: string }): LineDetail {
  return {
    id: line.id,
    name: line.name,
    players: [{ id: 'player-1', name: 'Carmela' }],
    skills: [{ skillId: SKILL.id, name: SKILL.name, color: SKILL.color, rating: 60 }],
    developmentGoals: [GOAL],
    focuses: [FOCUS],
  };
}

describe('LineOverview', () => {
  let httpMock: HttpTestingController;

  async function render() {
    await TestBed.configureTestingModule({
      imports: [LineOverview],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);

    const fixture = TestBed.createComponent(LineOverview);
    fixture.detectChanges();

    httpMock.expectOne('/api/lines').flush([LINE_A, LINE_B]);
    httpMock.expectOne('/api/players').flush(PLAYERS);
    httpMock.expectOne('/api/skills').flush([SKILL]);
    httpMock.expectOne('/api/development-goals').flush([GOAL, GOAL_2]);
    httpMock.expectOne('/api/focuses').flush([FOCUS]);
    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));

    await fixture.whenStable();
    return fixture;
  }

  afterEach(() => httpMock.verify());

  it('given lines exist, selects the first one and renders its roster/skills/goals/focuses', async () => {
    const fixture = await render();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Kiwi');
    expect(text).toContain('Carmela');
    expect(text).toContain('Passgenauigkeit');
    expect(text).toContain('Ballverluste im eigenen Drittel reduzieren');
    expect(text).toContain('Cross-Pässe unter Druck');
  });

  it('when switching to another line, fetches and renders that line’s detail', async () => {
    const fixture = await render();

    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.line-switch button'));
    buttons[1].click();

    httpMock.expectOne('/api/lines/line-b').flush(detailFor(LINE_B));
    await fixture.whenStable();

    expect((fixture.nativeElement.textContent as string)).toContain('Bäri');
  });

  it('when a rating segment is clicked, PUTs the new rating and refreshes the line', async () => {
    const fixture = await render();
    fixture.detectChanges();

    const segment: HTMLButtonElement = fixture.nativeElement.querySelector('app-rating-bar .seg:nth-child(5)');
    segment.click();

    const req = httpMock.expectOne('/api/lines/line-a/skills/skill-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rating: 100 });
    req.flush({ ...SKILL, skillId: SKILL.id, rating: 100 });

    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));
    await fixture.whenStable();
  });

  it('when associating a team player via the roster dialog, PUTs the full player id array', async () => {
    const fixture = await render();

    // player-1 is already on the roster; player-2 ("Debi") is in the team pool but not associated.
    const chipAddButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.roster-dialog .chip button.x'),
    );
    const debiChip = chipAddButtons.find((b) => (b.getAttribute('aria-label') ?? '').startsWith('Debi'));
    debiChip!.click();

    const req = httpMock.expectOne('/api/lines/line-a');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.playerIds).toEqual(['player-1', 'player-2']);
    req.flush(detailFor(LINE_A));

    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));
    await fixture.whenStable();
  });

  it('when toggling an unassociated goal via manage, PUTs the full goal id array', async () => {
    const fixture = await render();

    const manageButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.card.goals .btn'),
    );
    manageButtons[0].click();
    await fixture.whenStable();

    const addButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.card.goals .chip button.x'),
    );
    // Only "goal-1" is in the catalog and it's already associated (shows "×", not "+"),
    // so this exercises the remove path instead — still a full-array PUT.
    addButtons[0].click();

    const req = httpMock.expectOne('/api/lines/line-a');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.developmentGoalIds).toEqual([]);
    req.flush(detailFor(LINE_A));

    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));
    await fixture.whenStable();
  });

  it('when creating a skill, POSTs it with the chosen color and auto-associates it', async () => {
    const fixture = await render();

    (fixture.nativeElement.querySelector('.card.skills .section-title .btn') as HTMLButtonElement).click();
    await fixture.whenStable();

    const input: HTMLInputElement = fixture.nativeElement.querySelector('.card.skills .create-row input');
    input.value = 'Bully-Kontrolle';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    (
      fixture.nativeElement.querySelector('.card.skills .create-row button[type="submit"]') as HTMLButtonElement
    ).click();

    const post = httpMock.expectOne('/api/skills');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'Bully-Kontrolle', color: '#2c7a68' });
    post.flush({ id: 'skill-new', name: 'Bully-Kontrolle', color: '#2c7a68' });

    const assoc = httpMock.expectOne('/api/lines/line-a/skills/skill-new');
    expect(assoc.request.method).toBe('PUT');
    expect(assoc.request.body).toEqual({ rating: 50 });
    assoc.flush({ skillId: 'skill-new', name: 'Bully-Kontrolle', color: '#2c7a68', rating: 50 });

    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));
    await fixture.whenStable();
  });

  it('when creating a focus, POSTs it with every chosen goal id and auto-associates it', async () => {
    const fixture = await render();

    (fixture.nativeElement.querySelector('.card.focus .section-title .btn') as HTMLButtonElement).click();
    await fixture.whenStable();

    const nameInput: HTMLInputElement = fixture.nativeElement.querySelector('.card.focus .create-focus input');
    nameInput.value = 'Zonenpressing';
    nameInput.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    const goalChipButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.card.focus .create-focus app-manage-chips .chip button.x'),
    );
    goalChipButtons[0].click();
    goalChipButtons[1].click();
    await fixture.whenStable();

    (fixture.nativeElement.querySelector('.card.focus .create-focus .btn') as HTMLButtonElement).click();

    const post = httpMock.expectOne('/api/focuses');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'Zonenpressing', goalIds: ['goal-1', 'goal-2'] });
    post.flush({ id: 'focus-new', name: 'Zonenpressing', goalIds: ['goal-1', 'goal-2'] });

    const assoc = httpMock.expectOne('/api/lines/line-a');
    expect(assoc.request.method).toBe('PUT');
    expect(assoc.request.body.focusIds).toEqual(['focus-1', 'focus-new']);
    assoc.flush(detailFor(LINE_A));

    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));
    await fixture.whenStable();
  });

  it('when a swatch is picked for a skill, PUTs the new color to the catalog', async () => {
    const fixture = await render();

    (fixture.nativeElement.querySelector('.card.skills .skill-row .dot') as HTMLButtonElement).click();
    await fixture.whenStable();

    const swatches: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.card.skills .skill-row app-color-picker .swatch'),
    );
    swatches[1].click(); // second palette color: #c8722e

    const put = httpMock.expectOne('/api/skills/skill-1');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ color: '#c8722e' });
    put.flush({ id: 'skill-1', name: SKILL.name, color: '#c8722e' });

    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));
    await fixture.whenStable();
  });
});
