import { ComponentFixture, TestBed } from '@angular/core/testing';
import { testStage } from '../drill-fixtures';
import { RinkPlayer } from './rink-player';

describe('RinkPlayer', () => {
  async function render(): Promise<ComponentFixture<RinkPlayer>> {
    await TestBed.configureTestingModule({ imports: [RinkPlayer] }).compileComponents();
    const fixture = TestBed.createComponent(RinkPlayer);
    fixture.componentRef.setInput('stage', testStage());
    fixture.componentRef.setInput('autoplay', false);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  function ballX(fixture: ComponentFixture<RinkPlayer>): number {
    return Number((fixture.nativeElement.querySelector('.ball') as SVGCircleElement).getAttribute('cx'));
  }

  // spec: ui.drill-plays
  it('scrubs through the loop: the ball flies with the pass', async () => {
    const fixture = await render();
    const start = ballX(fixture);

    const scrub = fixture.nativeElement.querySelector('.scrub') as HTMLInputElement;
    scrub.value = '0.5';
    scrub.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(ballX(fixture)).toBeGreaterThan(start + 3);
    expect(fixture.nativeElement.querySelector('.ball--flying')).not.toBeNull();
  });

  // spec: ui.drill-plays
  it('steps from one Step to the next', async () => {
    const fixture = await render();

    (fixture.nativeElement.querySelector('.step') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();

    // The shot starts half a second after the 10 m pass at 10 m/s lands.
    expect(Number((fixture.nativeElement.querySelector('.scrub') as HTMLInputElement).value)).toBeCloseTo(1.5);
  });

  // spec: ui.drill-plays
  it('plays and pauses, and offers three speeds', async () => {
    const fixture = await render();
    const toggle = fixture.nativeElement.querySelector('.play-toggle') as HTMLButtonElement;

    expect(toggle.getAttribute('aria-label')).toBe('Abspielen');
    toggle.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-label')).toBe('Pause');
    toggle.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-label')).toBe('Abspielen');

    const options = Array.from(fixture.nativeElement.querySelectorAll('.speed option')).map((o) => (o as HTMLOptionElement).textContent?.trim());
    expect(options).toEqual(['0.5×', '1×', '2×']);
  });
});
