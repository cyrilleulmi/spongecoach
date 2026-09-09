import { TestBed } from '@angular/core/testing';
import { DialTimeline } from './dial-timeline';
import { DialLine } from '../dial-palette';
import { TimelineEvent } from '../iteration.model';

const LINES: DialLine[] = [
  { id: 'line-a', name: 'Kiwi', color: '#4c8c3d' },
  { id: 'line-b', name: 'Bäri', color: '#8b5e34' },
];

const EVENTS: TimelineEvent[] = [
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
    name: 'Testspiel gegen Rotweiss',
    position: 2,
    scheduledOn: '2026-09-12',
    focusAttachments: [],
  },
];

describe('DialTimeline', () => {
  async function render(selectedEventId: string | null, nextEventId: string | null) {
    await TestBed.configureTestingModule({ imports: [DialTimeline] }).compileComponents();
    const fixture = TestBed.createComponent(DialTimeline);
    fixture.componentRef.setInput('events', EVENTS);
    fixture.componentRef.setInput('lines', LINES);
    fixture.componentRef.setInput('selectedEventId', selectedEventId);
    fixture.componentRef.setInput('nextEventId', nextEventId);
    await fixture.whenStable();
    return fixture;
  }

  it('renders one node per event, numbering trainings and naming matches', async () => {
    const fixture = await render(null, null);
    const nodes = fixture.nativeElement.querySelectorAll('.d-node');
    expect(nodes.length).toBe(2);
    expect(nodes[0].textContent).toContain('Training 1');
    expect(nodes[1].textContent).toContain('Testspiel gegen Rotweiss');
  });

  it('marks the next event with the NÄCHSTES tag and the current class', async () => {
    const fixture = await render(null, 'ev-1');
    const first = fixture.nativeElement.querySelector('.d-node') as HTMLElement;
    expect(first.classList).toContain('current');
    expect(first.textContent).toContain('NÄCHSTES');
  });

  it('lights a quadrant in the line color where that line has a focus set', async () => {
    const fixture = await render(null, null);
    const dial = fixture.nativeElement.querySelector('.d-node .dial') as HTMLElement;
    // line-a (index 0) is lit -> its color appears; line-b (index 1) is unlit -> dial-track var.
    const bg = dial.style.background.toLowerCase();
    expect(bg).toContain('conic-gradient');
    expect(bg).toMatch(/#4c8c3d|rgb\(76, 140, 61\)/); // line-a color, lit
    expect(bg).toContain('var(--dial-track)'); // line-b, unlit
  });

  it('emits the event id when a node is clicked', async () => {
    const fixture = await render(null, null);
    const emitted: string[] = [];
    fixture.componentInstance.select.subscribe((id) => emitted.push(id));

    (fixture.nativeElement.querySelectorAll('.d-node')[1] as HTMLButtonElement).click();

    expect(emitted).toEqual(['ev-2']);
  });
});
