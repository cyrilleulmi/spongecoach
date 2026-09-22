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

  // spec: ui.first-line-selected
  it('given lines exist, selects the first one and renders its roster/skills/goals/focuses', async () => {
    const fixture = await render();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Kiwi');
    expect(text).toContain('Carmela');
    expect(text).toContain('Passgenauigkeit');
    expect(text).toContain('Ballverluste im eigenen Drittel reduzieren');
    expect(text).toContain('Cross-Pässe unter Druck');
  });

  // spec: ui.switch-line-refetches
  it('when switching to another line, fetches and renders that line’s detail', async () => {
    const fixture = await render();

    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.line-switch button'));
    buttons[1].click();

    httpMock.expectOne('/api/lines/line-b').flush(detailFor(LINE_B));
    await fixture.whenStable();

    expect((fixture.nativeElement.textContent as string)).toContain('Bäri');
  });

  // spec: ui.rating-is-optimistic
  it('when a rating segment is clicked, applies it immediately and PUTs in the background with no refetch', async () => {
    const fixture = await render();
    fixture.detectChanges();

    const segment: HTMLButtonElement = fixture.nativeElement.querySelector('app-rating-bar .seg:nth-child(5)');
    segment.click();
    fixture.detectChanges();

    // Rating reflects the click before the PUT resolves — no round trip to wait on.
    expect((fixture.nativeElement.textContent as string)).toContain('5/5');

    const req = httpMock.expectOne('/api/lines/line-a/skills/skill-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rating: 100 });
    req.flush({ ...SKILL, skillId: SKILL.id, rating: 100 });

    // Happy path: no follow-up GET of the line — the optimistic state already matches the server.
    httpMock.expectNone('/api/lines/line-a');
  });

  // spec: ui.rating-rolls-back
  it('when the rating PUT fails, rolls back to the previous rating', async () => {
    const fixture = await render();
    fixture.detectChanges();

    const segment: HTMLButtonElement = fixture.nativeElement.querySelector('app-rating-bar .seg:nth-child(5)');
    segment.click();
    fixture.detectChanges();
    expect((fixture.nativeElement.textContent as string)).toContain('5/5');

    const req = httpMock.expectOne('/api/lines/line-a/skills/skill-1');
    req.flush('boom', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement.textContent as string)).toContain('3/5'); // back to the seeded 60 rating
  });

  // spec: ui.roster-dialog-commits-once
  it('when associating a team player via the roster dialog, PUTs the full player id array', async () => {
    const fixture = await render();

    // "Verwalten" seeds the dialog's draft from the current roster; toggles only land on "Fertig".
    const manage: HTMLButtonElement = fixture.nativeElement.querySelector('.card.roster .section-title .btn');
    manage.click();
    fixture.detectChanges();

    // player-1 is already on the roster; player-2 ("Debi") is in the team pool but not associated.
    const chipAddButtons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.roster-dialog .chip button.x'),
    );
    const debiChip = chipAddButtons.find((b) => (b.getAttribute('aria-label') ?? '').startsWith('Debi'));
    debiChip!.click();
    fixture.detectChanges();

    httpMock.expectNone('/api/lines/line-a'); // staged only — nothing sent until "Fertig"

    const done: HTMLButtonElement = fixture.nativeElement.querySelector('.roster-dialog .section-title .btn');
    done.click();

    const req = httpMock.expectOne('/api/lines/line-a');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.playerIds).toEqual(['player-1', 'player-2']);
    req.flush(detailFor(LINE_A));

    httpMock.expectOne('/api/lines/line-a').flush(detailFor(LINE_A));
    await fixture.whenStable();
  });

  // spec: ui.goal-toggle-saves-full-set
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

  // spec: ui.create-skill-auto-associates
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

    fixture.detectChanges();
    expect((fixture.nativeElement.textContent as string)).toContain('Bully-Kontrolle');

    const assoc = httpMock.expectOne('/api/lines/line-a/skills/skill-new');
    expect(assoc.request.method).toBe('PUT');
    expect(assoc.request.body).toEqual({ rating: 50 });
    assoc.flush({ skillId: 'skill-new', name: 'Bully-Kontrolle', color: '#2c7a68', rating: 50 });

    // No follow-up GET — the optimistically-added skill already matches the server response.
    httpMock.expectNone('/api/lines/line-a');
  });

  // spec: ui.create-focus-auto-associates
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

  function openMenu(fixture: { nativeElement: HTMLElement }): void {
    (fixture.nativeElement.querySelector('.line-menu .icon-btn') as HTMLButtonElement).click();
  }

  // spec: ui.line-lifecycle-menu
  it('when creating a line via the "..." menu, POSTs the name and selects the new line', async () => {
    const fixture = await render();

    openMenu(fixture);
    fixture.detectChanges();
    (
      Array.from(fixture.nativeElement.querySelectorAll('.menu button')).find((b) =>
        (b.textContent ?? '').includes('Neuen Block anlegen'),
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    const input: HTMLInputElement = fixture.nativeElement.querySelector('.small-dialog input');
    input.value = 'Lama';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    (fixture.nativeElement.querySelector('.small-dialog button[type="submit"]') as HTMLButtonElement).click();

    const post = httpMock.expectOne('/api/lines');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'Lama' });
    const LINE_C = { id: 'line-c', name: 'Lama', playerCount: 0 };
    post.flush(LINE_C);

    httpMock.expectOne('/api/lines/line-c').flush(detailFor(LINE_C));
    await fixture.whenStable();

    expect((fixture.nativeElement.textContent as string)).toContain('Lama');
  });

  it('when deleting the selected line via the "..." menu, asks for confirmation before DELETEing', async () => {
    const fixture = await render();

    openMenu(fixture);
    fixture.detectChanges();
    (
      Array.from(fixture.nativeElement.querySelectorAll('.menu button')).find((b) =>
        (b.textContent ?? '').includes('löschen'),
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    // Confirmation dialog is open; deleting must not fire until confirmed.
    httpMock.expectNone('/api/lines/line-a');

    (
      Array.from(fixture.nativeElement.querySelectorAll('.small-dialog .dialog-actions button')).find((b) =>
        (b.textContent ?? '').trim() === 'Löschen',
      ) as HTMLButtonElement
    ).click();

    const del = httpMock.expectOne('/api/lines/line-a');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);

    httpMock.expectOne('/api/lines/line-b').flush(detailFor(LINE_B));
    await fixture.whenStable();

    const remainingTabs: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('.line-switch button'),
    );
    expect(remainingTabs.map((b) => b.textContent?.trim())).toEqual(['Bäri']);
    expect((fixture.nativeElement.textContent as string)).toContain('Bäri');
  });

  it('when cancelling the delete confirmation, no DELETE is sent', async () => {
    const fixture = await render();

    openMenu(fixture);
    fixture.detectChanges();
    (
      Array.from(fixture.nativeElement.querySelectorAll('.menu button')).find((b) =>
        (b.textContent ?? '').includes('löschen'),
      ) as HTMLButtonElement
    ).click();
    fixture.detectChanges();

    (
      Array.from(fixture.nativeElement.querySelectorAll('.small-dialog .dialog-actions button')).find((b) =>
        (b.textContent ?? '').trim() === 'Abbrechen',
      ) as HTMLButtonElement
    ).click();

    httpMock.expectNone('/api/lines/line-a');
  });

  it('when opening "Gelöschte Blöcke verwalten", lists and restores a deleted line', async () => {
    const fixture = await render();

    openMenu(fixture);
    fixture.detectChanges();
    (
      Array.from(fixture.nativeElement.querySelectorAll('.menu button')).find((b) =>
        (b.textContent ?? '').includes('Gelöschte Blöcke'),
      ) as HTMLButtonElement
    ).click();

    const DELETED_LINE = { id: 'line-deleted', name: 'Lama', playerCount: 4 };
    httpMock.expectOne('/api/lines/deleted').flush([DELETED_LINE]);
    await fixture.whenStable();

    expect((fixture.nativeElement.textContent as string)).toContain('Lama');

    (fixture.nativeElement.querySelector('.small-dialog .roster-row .btn') as HTMLButtonElement).click();

    const restore = httpMock.expectOne('/api/lines/line-deleted/restore');
    expect(restore.request.method).toBe('POST');
    restore.flush(DELETED_LINE);

    httpMock.expectOne('/api/lines/line-deleted').flush(detailFor(DELETED_LINE));
    await fixture.whenStable();

    const tabs: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.line-switch button'));
    expect(tabs.map((b) => b.textContent?.trim())).toEqual(['Kiwi', 'Bäri', 'Lama']);
  });

  // spec: ui.recolor-from-line-screen
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
