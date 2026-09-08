import { TestBed } from '@angular/core/testing';
import { RatingBar } from './rating-bar';

describe('RatingBar', () => {
  async function render(rating: number, color = '#2c7a68') {
    await TestBed.configureTestingModule({ imports: [RatingBar] }).compileComponents();
    const fixture = TestBed.createComponent(RatingBar);
    fixture.componentRef.setInput('rating', rating);
    fixture.componentRef.setInput('color', color);
    await fixture.whenStable();
    return fixture;
  }

  it('given a rating of 72, shows 4 of 5 segments filled', async () => {
    const fixture = await render(72);
    const filled = fixture.nativeElement.querySelectorAll('.seg').length;
    expect(filled).toBe(5);

    const segments: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.seg'));
    const filledCount = segments.filter((seg) => seg.style.background !== 'var(--line-soft)').length;
    expect(filledCount).toBe(4);
  });

  it('when clicking the third segment, emits a rating of 60', async () => {
    const fixture = await render(20);
    const levels: number[] = [];
    fixture.componentInstance.levelChange.subscribe((level) => levels.push(level));

    const segments: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.seg'));
    segments[2].click();

    expect(levels).toEqual([60]);
  });
});
