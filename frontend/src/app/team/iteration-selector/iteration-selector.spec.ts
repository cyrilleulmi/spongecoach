import { TestBed } from '@angular/core/testing';
import { IterationSelector } from './iteration-selector';
import { Iteration } from '../iteration.model';

const ITERATIONS: Iteration[] = [
  { id: 'it-1', name: 'Vorbereitung', position: 1, events: [] },
  { id: 'it-2', name: 'Hinrunde', position: 2, events: [] },
];

describe('IterationSelector', () => {
  async function render(index: number) {
    await TestBed.configureTestingModule({ imports: [IterationSelector] }).compileComponents();
    const fixture = TestBed.createComponent(IterationSelector);
    fixture.componentRef.setInput('iterations', ITERATIONS);
    fixture.componentRef.setInput('index', index);
    await fixture.whenStable();
    return fixture;
  }

  it('shows the current iteration name and position, with prev disabled on the first', async () => {
    const fixture = await render(0);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Vorbereitung');
    expect(text).toContain('Iteration 1 von 2');

    const prev = fixture.nativeElement.querySelector(
      'button[aria-label="Vorherige Iteration"]',
    ) as HTMLButtonElement;
    expect(prev.disabled).toBe(true);
  });

  it('emits the next index when the forward button is clicked', async () => {
    const fixture = await render(0);
    const emitted: number[] = [];
    fixture.componentInstance.indexChange.subscribe((i) => emitted.push(i));

    (
      fixture.nativeElement.querySelector(
        'button[aria-label="Nächste Iteration"]',
      ) as HTMLButtonElement
    ).click();

    expect(emitted).toEqual([1]);
  });

  it('emits the target index when a position dot is clicked', async () => {
    const fixture = await render(0);
    const emitted: number[] = [];
    fixture.componentInstance.indexChange.subscribe((i) => emitted.push(i));

    const dots = fixture.nativeElement.querySelectorAll('.dot-btn');
    (dots[1] as HTMLButtonElement).click();

    expect(emitted).toEqual([1]);
  });
});
