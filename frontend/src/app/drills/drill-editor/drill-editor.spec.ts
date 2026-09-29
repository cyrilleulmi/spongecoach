import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DrillRink } from '../drill-rink/drill-rink';
import { testStage } from '../drill-fixtures';
import { DrillScript } from '../drill.model';
import { DrillEditor, ScriptEdit } from './drill-editor';

describe('DrillEditor', () => {
  let saved: ScriptEdit[];

  async function render(): Promise<ComponentFixture<DrillEditor>> {
    await TestBed.configureTestingModule({ imports: [DrillEditor] }).compileComponents();
    const fixture = TestBed.createComponent(DrillEditor);
    const script: DrillScript = { stages: [testStage()], assumptions: [] };
    fixture.componentRef.setInput('script', script);
    saved = [];
    fixture.componentInstance.saved.subscribe((edit) => saved.push(edit));
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  function rink(fixture: ComponentFixture<DrillEditor>): DrillRink {
    return fixture.debugElement.query(By.directive(DrillRink)).componentInstance as DrillRink;
  }

  function click(fixture: ComponentFixture<DrillEditor>, selector: string): void {
    (fixture.nativeElement.querySelector(selector) as HTMLElement).click();
    fixture.detectChanges();
  }

  // spec: ui.drill-editor
  it('drags a start point and saves the moved position', async () => {
    const fixture = await render();

    rink(fixture).handleSelected.emit('actor:a1');
    rink(fixture).handleMoved.emit({ id: 'actor:a1', point: { x: -2, y: 3 } });
    fixture.detectChanges();
    click(fixture, '.save');

    expect(saved[0].script.stages[0].actors[0].start).toEqual({ x: -2, y: 3 });
  });

  // spec: ui.drill-editor
  it('shows the run as a timeline, and changes the speed of the Step picked there', async () => {
    const fixture = await render();

    const rows = Array.from(fixture.nativeElement.querySelectorAll('.timeline-row')) as HTMLElement[];
    expect(rows.map((r) => r.querySelector('.timeline-label')?.textContent?.trim())).toEqual(['A1 · Passgeberin', 'A2 · Schützin']);
    click(fixture, '.timeline-bar--pass');

    const speed = fixture.nativeElement.querySelector('input[name="speed"]') as HTMLInputElement;
    speed.value = '6';
    speed.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click(fixture, '.save');

    expect(saved[0].script.stages[0].steps[0].speed).toBe(6);
  });

  // spec: ui.drill-editor
  it('drags a waypoint of the picked Step', async () => {
    const fixture = await render();
    click(fixture, '.timeline-bar--shot');

    expect(rink(fixture).handles().some((h) => h.id === 'waypoint:shot:0')).toBe(true);
    rink(fixture).handleSelected.emit('waypoint:shot:0');
    rink(fixture).handleMoved.emit({ id: 'waypoint:shot:0', point: { x: 1, y: 9.6 } });
    fixture.detectChanges();
    click(fixture, '.save');

    expect(saved[0].script.stages[0].steps[1].path).toEqual([{ x: 1, y: 9.6 }]);
  });

  // spec: ui.drill-editor
  it('undoes a drag as one step', async () => {
    const fixture = await render();

    rink(fixture).handleSelected.emit('actor:a2');
    rink(fixture).handleMoved.emit({ id: 'actor:a2', point: { x: 4, y: 1 } });
    rink(fixture).handleMoved.emit({ id: 'actor:a2', point: { x: 3, y: 2 } });
    fixture.detectChanges();
    click(fixture, '.undo');

    expect(rink(fixture).handles().find((h) => h.id === 'actor:a2')?.point).toEqual({ x: 5, y: 0 });
    expect((fixture.nativeElement.querySelector('.save') as HTMLButtonElement).disabled).toBe(true);
  });

  // spec: ui.drill-editor
  it('previews the edited Stage in the player', async () => {
    const fixture = await render();

    click(fixture, '.mode:nth-child(2)');

    expect(fixture.nativeElement.querySelector('app-rink-player')).not.toBeNull();
  });

  it('deletes a Step, and whatever waited for it now waits for what it waited for', async () => {
    const fixture = await render();
    click(fixture, '.timeline-bar--pass');
    click(fixture, '.button--danger');
    click(fixture, '.save');

    const steps = saved[0].script.stages[0].steps;
    expect(steps.map((s) => s.id)).toEqual(['shot']);
    expect(steps[0].after).toBe('');
  });
});
