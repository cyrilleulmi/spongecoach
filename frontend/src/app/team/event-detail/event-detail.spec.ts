import { TestBed } from '@angular/core/testing';
import { EventDetail } from './event-detail';
import { FocusRef } from '../../lines/line.model';
import { DialLine } from '../dial-palette';
import { PlayerAttendance, TimelineEvent } from '../iteration.model';

const LINES: DialLine[] = [
  { id: 'line-a', name: 'Kiwi', color: '#4c8c3d' },
  { id: 'line-b', name: 'Bäri', color: '#8b5e34' },
];

const ATTENDANCE: PlayerAttendance[] = [
  { playerId: 'p-1', playerName: 'Carmela', lineIds: ['line-a'], status: 'ATTENDING', declineMessage: null },
  { playerId: 'p-2', playerName: 'Rahel', lineIds: ['line-b'], status: 'DECLINED', declineMessage: 'Verletzt' },
  { playerId: 'p-3', playerName: 'Debi', lineIds: ['line-a'], status: 'PENDING', declineMessage: null },
];

const FOCUSES = new Map<string, FocusRef[]>([
  [
    'line-a',
    [
      { id: 'f-1', name: 'Fokus A', goalIds: [] },
      { id: 'f-2', name: 'Fokus B', goalIds: [] },
    ],
  ],
  ['line-b', [{ id: 'f-3', name: 'Fokus C', goalIds: [] }]],
]);

// Far-future dates keep these events "not done" for as long as this suite is maintained.
const OTHER_TRAINING: TimelineEvent = {
  id: 'ev-0',
  typeId: 'tt',
  type: 'Training',
  name: null,
  scheduledOn: '2099-09-01T18:00',
  focusAttachments: [],
  lines: LINES,
  attendance: [],
};

const TRAINING: TimelineEvent = {
  id: 'ev-1',
  typeId: 'tt',
  type: 'Training',
  name: null,
  scheduledOn: '2099-09-08T18:00',
  focusAttachments: [{ lineId: 'line-a', lineName: 'Kiwi', focusId: 'f-1', focusName: 'Fokus A' }],
  lines: LINES,
  attendance: ATTENDANCE,
};

const MATCH: TimelineEvent = {
  id: 'ev-2',
  typeId: 'mt',
  type: 'Match',
  name: 'Testspiel',
  scheduledOn: '2099-09-12T15:00',
  focusAttachments: [],
  lines: LINES,
  attendance: [],
};

const PAST_TRAINING: TimelineEvent = {
  id: 'ev-3',
  typeId: 'tt',
  type: 'Training',
  name: null,
  scheduledOn: '2020-01-06T18:00',
  focusAttachments: [],
  lines: LINES,
  attendance: ATTENDANCE,
};

const ALL_EVENTS: TimelineEvent[] = [OTHER_TRAINING, TRAINING, MATCH, PAST_TRAINING];

describe('EventDetail', () => {
  async function render(event: TimelineEvent, isNext = false) {
    await TestBed.configureTestingModule({ imports: [EventDetail] }).compileComponents();
    const fixture = TestBed.createComponent(EventDetail);
    fixture.componentRef.setInput('event', event);
    fixture.componentRef.setInput('events', ALL_EVENTS);
    fixture.componentRef.setInput('focusesByLine', FOCUSES);
    fixture.componentRef.setInput('isNext', isNext);
    await fixture.whenStable();
    return fixture;
  }

  it('shows the training number (its rank among Trainings) and a focus dropdown per line, preselecting the set focus', async () => {
    const fixture = await render(TRAINING);
    expect((fixture.nativeElement.textContent as string)).toContain('Training 2');

    const selects = fixture.nativeElement.querySelectorAll('.focus-select') as NodeListOf<HTMLSelectElement>;
    expect(selects.length).toBe(2);
    expect(selects[0].value).toBe('f-1'); // line-a
    expect(selects[1].value).toBe(''); // line-b, not set
  });

  // spec: ui.set-focus-from-detail
  it('emits a focusChange with the chosen focus id when a dropdown changes', async () => {
    const fixture = await render(TRAINING);
    const changes: unknown[] = [];
    fixture.componentInstance.focusChange.subscribe((c) => changes.push(c));

    const lineBSelect = fixture.nativeElement.querySelectorAll('.focus-select')[1] as HTMLSelectElement;
    lineBSelect.value = 'f-3';
    lineBSelect.dispatchEvent(new Event('change'));

    expect(changes).toEqual([{ lineId: 'line-b', focusId: 'f-3' }]);
  });

  // spec: ui.clear-focus-from-detail
  it('emits a focusChange with a null focus when "nicht gesetzt" is chosen', async () => {
    const fixture = await render(TRAINING);
    const changes: unknown[] = [];
    fixture.componentInstance.focusChange.subscribe((c) => changes.push(c));

    const lineASelect = fixture.nativeElement.querySelectorAll('.focus-select')[0] as HTMLSelectElement;
    lineASelect.value = '';
    lineASelect.dispatchEvent(new Event('change'));

    expect(changes).toEqual([{ lineId: 'line-a', focusId: null }]);
  });

  it('emits deleteEvent with the event id', async () => {
    const fixture = await render(TRAINING);
    const deleted: string[] = [];
    fixture.componentInstance.deleteEvent.subscribe((id) => deleted.push(id));

    (fixture.nativeElement.querySelector('.delete-btn') as HTMLButtonElement).click();

    expect(deleted).toEqual(['ev-1']);
  });

  // spec: ui.event-readonly-when-done
  it('disables the focus selects, date, and delete button for a past event', async () => {
    const fixture = await render(PAST_TRAINING);

    const selects = fixture.nativeElement.querySelectorAll('.focus-select') as NodeListOf<HTMLSelectElement>;
    expect(selects[0].disabled).toBe(true);
    expect((fixture.nativeElement.querySelector('.detail-date input') as HTMLInputElement).disabled).toBe(true);
    expect((fixture.nativeElement.querySelector('.delete-btn') as HTMLButtonElement).disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('liegt in der Vergangenheit');
  });

  // spec: ui.edit-anyway
  it('lifts read-only for a past event via "trotzdem bearbeiten", and can re-lock it', async () => {
    const fixture = await render(PAST_TRAINING);

    (fixture.nativeElement.querySelector('.link-btn') as HTMLButtonElement).click();
    fixture.detectChanges();
    let selects = fixture.nativeElement.querySelectorAll('.focus-select') as NodeListOf<HTMLSelectElement>;
    expect(selects[0].disabled).toBe(false);

    (fixture.nativeElement.querySelector('.link-btn') as HTMLButtonElement).click();
    fixture.detectChanges();
    selects = fixture.nativeElement.querySelectorAll('.focus-select') as NodeListOf<HTMLSelectElement>;
    expect(selects[0].disabled).toBe(true);
  });

  // spec: ui.future-event-editable
  it('does not disable controls for a future event', async () => {
    const fixture = await render(TRAINING);

    const selects = fixture.nativeElement.querySelectorAll('.focus-select') as NodeListOf<HTMLSelectElement>;
    expect(selects[0].disabled).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain('liegt in der Vergangenheit');
  });

  // spec: ui.attending-counter
  it('shows an attending/total counter per line, and one row per player with their line badges', async () => {
    const fixture = await render(TRAINING);

    const counts = fixture.nativeElement.querySelectorAll('.attendance-count');
    // line-a (Kiwi): Carmela attending, Debi pending -> 1/2. line-b (Bäri): Rahel declined -> 0/1.
    expect((counts[0].textContent as string).trim()).toBe('1/2');
    expect((counts[1].textContent as string).trim()).toBe('0/1');

    // grouped by line (Kiwi's Players before Bäri's), name-sorted within each group.
    const rows = fixture.nativeElement.querySelectorAll('.attendance-row');
    expect(rows.length).toBe(3);
    expect(rows[0].textContent).toContain('Carmela');
    expect(rows[0].querySelector('.line-badge').textContent).toContain('Kiwi');
    expect(rows[1].textContent).toContain('Debi');
    expect(rows[2].textContent).toContain('Rahel');
  });

  it('color-codes each row by attendance status', async () => {
    const fixture = await render(TRAINING);
    const rows = fixture.nativeElement.querySelectorAll('.attendance-row');
    expect(rows[0].classList).toContain('status-attending'); // Carmela
    expect(rows[1].classList).toContain('status-pending'); // Debi
    expect(rows[2].classList).toContain('status-declined'); // Rahel
  });

  // spec: ui.attendance-from-detail
  it('emits an attendanceChange when a status is set, and shows a decline message input only when declined', async () => {
    const fixture = await render(TRAINING);
    const changes: unknown[] = [];
    fixture.componentInstance.attendanceChange.subscribe((c) => changes.push(c));

    // sorted rows: Carmela (attending), Debi (pending), Rahel (declined).
    const rows = fixture.nativeElement.querySelectorAll('.attendance-row');
    const debiSelect = rows[1].querySelector('.attendance-select') as HTMLSelectElement;
    expect(rows[1].querySelector('.decline-message-input')).toBeNull();

    debiSelect.value = 'DECLINED';
    debiSelect.dispatchEvent(new Event('change'));

    expect(changes).toEqual([{ playerId: 'p-3', status: 'DECLINED', declineMessage: null }]);
  });

  it('emits an attendanceChange with the trimmed decline message on change', async () => {
    const fixture = await render(TRAINING);
    const changes: unknown[] = [];
    fixture.componentInstance.attendanceChange.subscribe((c) => changes.push(c));

    // sorted rows: Carmela (attending), Debi (pending), Rahel (declined, has the message input).
    const rows = fixture.nativeElement.querySelectorAll('.attendance-row');
    const messageInput = rows[2].querySelector('.decline-message-input') as HTMLInputElement;
    messageInput.value = '  Krank  ';
    messageInput.dispatchEvent(new Event('change'));

    expect(changes).toEqual([{ playerId: 'p-2', status: 'DECLINED', declineMessage: 'Krank' }]);
  });

  it('disables attendance controls for a past event', async () => {
    const fixture = await render(PAST_TRAINING);
    const select = fixture.nativeElement.querySelector('.attendance-select') as HTMLSelectElement;
    expect(select.disabled).toBe(true);
  });

  it('for a match, shows an editable name that emits rename on change', async () => {
    const fixture = await render(MATCH);
    const renames: string[] = [];
    fixture.componentInstance.rename.subscribe((n) => renames.push(n));

    const input = fixture.nativeElement.querySelector('.detail-title-input') as HTMLInputElement;
    expect(input.value).toBe('Testspiel');
    input.value = 'Testspiel gegen Bern';
    input.dispatchEvent(new Event('change'));

    expect(renames).toEqual(['Testspiel gegen Bern']);
  });
});
