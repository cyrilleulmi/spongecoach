import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { addActor, blankScript } from '../drill-authoring';
import { DrillRink } from '../drill-rink/drill-rink';
import { testStage } from '../drill-fixtures';
import { DrillScript, Point } from '../drill.model';
import { DrillEditor, ScriptEdit } from './drill-editor';

describe('DrillEditor', () => {
  let saved: ScriptEdit[];
  let changes: DrillScript[];

  async function render(script: DrillScript = { stages: [testStage()], assumptions: [] }, creating = false): Promise<ComponentFixture<DrillEditor>> {
    await TestBed.configureTestingModule({ imports: [DrillEditor] }).compileComponents();
    const fixture = TestBed.createComponent(DrillEditor);
    fixture.componentRef.setInput('script', script);
    fixture.componentRef.setInput('creating', creating);
    saved = [];
    changes = [];
    fixture.componentInstance.saved.subscribe((edit) => saved.push(edit));
    fixture.componentInstance.changed.subscribe((next) => changes.push(next));
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  /** Two players of side A on a blank script, to draw on. */
  function twoPlayers(): DrillScript {
    const script = blankScript();
    addActor(script, script.stages[0], 'PLAYER', 'A', { x: -4, y: -6 });
    addActor(script, script.stages[0], 'PLAYER', 'A', { x: 4, y: -6 });
    return script;
  }

  function rink(fixture: ComponentFixture<DrillEditor>): DrillRink {
    return fixture.debugElement.query(By.directive(DrillRink)).componentInstance as DrillRink;
  }

  function click(fixture: ComponentFixture<DrillEditor>, selector: string): void {
    (fixture.nativeElement.querySelector(selector) as HTMLElement).click();
    fixture.detectChanges();
  }

  function tap(fixture: ComponentFixture<DrillEditor>, ...points: Point[]): void {
    for (const point of points) {
      rink(fixture).rinkTapped.emit(point);
      fixture.detectChanges();
    }
  }

  function pointer(type: string, clientX: number): Event {
    const Constructor = (window as unknown as { PointerEvent?: typeof MouseEvent }).PointerEvent ?? MouseEvent;
    return new Constructor(type, { bubbles: true, clientX, clientY: 0 });
  }

  function select(fixture: ComponentFixture<DrillEditor>, name: string, value: string): void {
    const element = fixture.nativeElement.querySelector(`select[name="${name}"]`) as HTMLSelectElement;
    element.value = value;
    element.dispatchEvent(new Event('change'));
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
    click(fixture, '.delete-step');
    click(fixture, '.save');

    const steps = saved[0].script.stages[0].steps;
    expect(steps.map((s) => s.id)).toEqual(['shot']);
    expect(steps[0].after).toBe('');
  });

  // spec: ui.drill-draw-place
  it('draws a new drill: figures are placed by tapping, and there is nothing to save until there is one', async () => {
    const fixture = await render(blankScript(), true);
    const saveButton = () => fixture.nativeElement.querySelector('.save') as HTMLButtonElement;
    expect(saveButton().disabled).toBe(true);

    tap(fixture, { x: -4, y: -6 });
    click(fixture, '.tool--actor-b');
    tap(fixture, { x: 4, y: -6 });
    click(fixture, '.tool--prop-cone');
    tap(fixture, { x: 0, y: 0 });
    saveButton().click();

    const stage = saved[0].script.stages[0];
    expect(stage.actors.map((a) => [a.label, a.side])).toEqual([['A1', 'A'], ['B1', 'B']]);
    expect(stage.props.map((p) => p.kind)).toEqual(['CONE']);
    expect(saved[0].changeSummary).toBe('Gezeichnet');
    expect(changes.length).toBeGreaterThan(0);
  });

  // spec: ui.drill-draw-path
  it('draws a path from a figure freehand, and picks the Step it made', async () => {
    const fixture = await render(twoPlayers(), true);

    click(fixture, '.tool--step-run');
    rink(fixture).strokeDrawn.emit([{ x: -4, y: -6 }, { x: -4, y: -1 }, { x: -4.05, y: 4 }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.timeline-bar--run.timeline-bar--selected')).not.toBeNull();
    expect(rink(fixture).ghosts().map((g) => [g.label, g.point])).toEqual([['A1', { x: -4, y: 4 }]]);
    click(fixture, '.save');
    expect(saved[0].script.stages[0].steps[0]).toMatchObject({ type: 'RUN', partId: 'p1', sketch: 0 });
  });

  // spec: ui.drill-draw-mobile
  it('lights up what drawing made without opening its form over the rink, until its bar is tapped', async () => {
    const fixture = await render(twoPlayers(), true);
    click(fixture, '.tool--step-run');
    rink(fixture).strokeDrawn.emit([{ x: -4, y: -6 }, { x: -4, y: 4 }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.timeline-bar--selected')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.sheet')).toBeNull();

    click(fixture, '.timeline-bar--run');
    expect(fixture.nativeElement.querySelector('.sheet .step-form')).not.toBeNull();
    click(fixture, '.sheet-close');
    expect(fixture.nativeElement.querySelector('.sheet')).toBeNull();
  });

  // spec: ui.drill-draw-path
  it('draws a path point by point and ends it with "Fertig"', async () => {
    const fixture = await render(twoPlayers(), true);

    click(fixture, '.tool--step-dribble');
    tap(fixture, { x: -4, y: -6 }, { x: -2, y: -3 }, { x: 0, y: -6 });
    expect(rink(fixture).draft()).toHaveLength(3);
    click(fixture, '.finish-draft');

    expect(rink(fixture).draft()).toHaveLength(0);
    click(fixture, '.save');
    expect(saved[0].script.stages[0].steps[0]).toMatchObject({ type: 'DRIBBLE', path: [{ x: -2, y: -3 }, { x: 0, y: -6 }] });
  });

  // spec: ui.drill-pass-during-run
  it('draws a pass from the middle of a run, so it happens while the runner runs', async () => {
    const fixture = await render(twoPlayers(), true);
    click(fixture, '.tool--step-run');
    rink(fixture).strokeDrawn.emit([{ x: -4, y: -6 }, { x: -4, y: 4 }]);
    fixture.detectChanges();

    click(fixture, '.tool--step-pass');
    tap(fixture, { x: -4, y: -1 }, { x: 4, y: -1 });
    click(fixture, '.save');

    expect(saved[0].script.stages[0].steps[1]).toMatchObject({ type: 'PASS', after: 'st1', afterEdge: 'DURING', afterFraction: 0.5 });
  });

  // spec: ui.drill-pass-during-run
  it('sets a step to start during another from its sheet', async () => {
    const fixture = await render();
    click(fixture, '.timeline-bar--shot');

    select(fixture, 'edge', 'DURING');
    const fraction = fixture.nativeElement.querySelector('input[name="fraction"]') as HTMLInputElement;
    fraction.value = '25';
    fraction.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    click(fixture, '.save');

    expect(saved[0].script.stages[0].steps[1]).toMatchObject({ after: 'pass', afterEdge: 'DURING', afterFraction: 0.25 });
  });

  // spec: ui.drill-draw-ball-hint
  it('says so when a pass starts from a figure without the ball, and still draws it', async () => {
    const fixture = await render(twoPlayers(), true);

    click(fixture, '.tool--step-pass');
    tap(fixture, { x: -4, y: -6 }, { x: 0, y: 0 });

    expect((fixture.nativeElement.querySelector('.hint-note') as HTMLElement).textContent).toContain('A1 hat den Ball nicht');
    click(fixture, '.save');
    expect(saved[0].script.stages[0].steps).toHaveLength(1);
  });

  // spec: ui.drill-draw-path
  it('asks for a figure when a path does not start on one', async () => {
    const fixture = await render(twoPlayers(), true);

    click(fixture, '.tool--step-run');
    rink(fixture).strokeDrawn.emit([{ x: 0, y: 5 }, { x: 2, y: 5 }]);
    fixture.detectChanges();

    expect((fixture.nativeElement.querySelector('.hint-note') as HTMLElement).textContent).toContain('Beginne bei einer Figur');
    expect(fixture.nativeElement.querySelector('.timeline-bar')).toBeNull();
  });

  // spec: ui.drill-timeline-retime
  it('moves a bar of the timeline to re-time its Step, and a bar has to be picked first', async () => {
    const fixture = await render();
    const bar = () => fixture.nativeElement.querySelector('.timeline-bar--shot') as HTMLElement;

    bar().dispatchEvent(pointer('pointerdown', 100));
    bar().dispatchEvent(pointer('pointermove', 300));
    bar().dispatchEvent(pointer('pointerup', 300));
    fixture.detectChanges();
    expect((fixture.nativeElement.querySelector('.save') as HTMLButtonElement).disabled).toBe(true);

    click(fixture, '.timeline-bar--shot');
    bar().dispatchEvent(pointer('pointerdown', 100));
    bar().dispatchEvent(pointer('pointermove', 196));
    bar().dispatchEvent(pointer('pointerup', 196));
    fixture.detectChanges();
    click(fixture, '.save');

    // The shot began half a second after the pass; two seconds further on it begins 2.5 s after it.
    expect(saved[0].script.stages[0].steps[1]).toMatchObject({ after: 'pass', afterEdge: 'END', delay: 2.5 });
  });

  // spec: ui.drill-draw-stages
  it('adds, renames and removes Stages, and moves them with buttons', async () => {
    const fixture = await render();

    click(fixture, '.stage-add');
    const name = fixture.nativeElement.querySelector('input[name="stageName"]') as HTMLInputElement;
    name.value = 'Steigerung';
    name.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    select(fixture, 'area', 'HALF');
    click(fixture, '.stage-up');

    expect(Array.from(fixture.nativeElement.querySelectorAll('.stage-tab:not(.stage-add)')).map((t) => (t as HTMLElement).textContent?.trim())).toEqual([
      '1. Steigerung',
      '2. Pass und Schuss',
    ]);
    click(fixture, '.save');
    expect(saved[0].script.stages.map((s) => [s.name, s.area])).toEqual([['Steigerung', 'HALF'], ['Pass und Schuss', 'FULL']]);

    click(fixture, '.delete-stage');
    expect(Array.from(fixture.nativeElement.querySelectorAll('.stage-tab:not(.stage-add)')).map((t) => (t as HTMLElement).textContent?.trim())).toEqual(['1. Pass und Schuss']);
    expect((fixture.nativeElement.querySelector('.delete-stage') as HTMLButtonElement).disabled).toBe(true);
  });

  // spec: ui.drill-draw-place
  it('removes a figure with its Steps, and undoes and redoes what it did', async () => {
    const fixture = await render();
    rink(fixture).handleSelected.emit('actor:a1');
    fixture.detectChanges();

    click(fixture, '.delete-actor');
    click(fixture, '.save');
    expect(saved[0].script.stages[0].actors.map((a) => a.id)).toEqual(['a2']);
    expect(saved[0].script.stages[0].steps.map((s) => s.id)).toEqual(['shot']);

    click(fixture, '.undo');
    expect(rink(fixture).handles().some((h) => h.id === 'actor:a1')).toBe(true);
    click(fixture, '.redo');
    expect(rink(fixture).handles().some((h) => h.id === 'actor:a1')).toBe(false);
  });

  // spec: ui.drill-draw-mobile
  it('is a tool bar of buttons with words on them, so a phone needs no menu', async () => {
    const fixture = await render(blankScript(), true);

    const labels = Array.from(fixture.nativeElement.querySelectorAll('.toolbar .tool')).map((t) => (t as HTMLElement).textContent?.trim());

    expect(labels).toEqual(['Auswahl', 'A', 'B', 'G', 'T', 'Hütchen', 'Stange', 'Minitor', 'Laufweg', 'Dribbling', 'Pass', 'Schuss', 'Bewegung', 'Warten']);
    expect(fixture.nativeElement.querySelector('.tool--actor-a.tool--on')).not.toBeNull();
  });
});
