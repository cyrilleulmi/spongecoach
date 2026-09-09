import { TestBed } from '@angular/core/testing';
import { EventDetail } from './event-detail';
import { FocusRef } from '../../lines/line.model';
import { DialLine } from '../dial-palette';
import { TimelineEvent } from '../iteration.model';

const LINES: DialLine[] = [
  { id: 'line-a', name: 'Kiwi', color: '#4c8c3d' },
  { id: 'line-b', name: 'Bäri', color: '#8b5e34' },
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

const OTHER_TRAINING: TimelineEvent = {
  id: 'ev-0',
  typeId: 'tt',
  type: 'Training',
  name: null,
  scheduledOn: '2026-09-01T18:00',
  focusAttachments: [],
};

const TRAINING: TimelineEvent = {
  id: 'ev-1',
  typeId: 'tt',
  type: 'Training',
  name: null,
  scheduledOn: '2026-09-08T18:00',
  focusAttachments: [{ lineId: 'line-a', lineName: 'Kiwi', focusId: 'f-1', focusName: 'Fokus A' }],
};

const MATCH: TimelineEvent = {
  id: 'ev-2',
  typeId: 'mt',
  type: 'Match',
  name: 'Testspiel',
  scheduledOn: '2026-09-12T15:00',
  focusAttachments: [],
};

const ALL_EVENTS: TimelineEvent[] = [OTHER_TRAINING, TRAINING, MATCH];

describe('EventDetail', () => {
  async function render(event: TimelineEvent, isNext = false) {
    await TestBed.configureTestingModule({ imports: [EventDetail] }).compileComponents();
    const fixture = TestBed.createComponent(EventDetail);
    fixture.componentRef.setInput('event', event);
    fixture.componentRef.setInput('events', ALL_EVENTS);
    fixture.componentRef.setInput('lines', LINES);
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

  it('emits a focusChange with the chosen focus id when a dropdown changes', async () => {
    const fixture = await render(TRAINING);
    const changes: unknown[] = [];
    fixture.componentInstance.focusChange.subscribe((c) => changes.push(c));

    const lineBSelect = fixture.nativeElement.querySelectorAll('.focus-select')[1] as HTMLSelectElement;
    lineBSelect.value = 'f-3';
    lineBSelect.dispatchEvent(new Event('change'));

    expect(changes).toEqual([{ lineId: 'line-b', focusId: 'f-3' }]);
  });

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
