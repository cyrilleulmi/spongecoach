import {
  addActor,
  addProp,
  addStage,
  addStepFromStroke,
  addWaitStep,
  aimPass,
  actorPositionAt,
  blankScript,
  dependsOn,
  endPositions,
  findActorAt,
  insertWaypoint,
  moveStage,
  nextId,
  removeActor,
  removeStage,
  removeStep,
  removeWaypoint,
  resizeStep,
  retimeStep,
  simplify,
  updateActor,
} from './drill-authoring';
import { DrillScript, Point, Stage } from './drill.model';
import { schedule } from './drill-schedule';

/** A blank script with two players of side A: a1 at (-4, -6) and a2 at (4, -6). */
function twoPlayers(): { script: DrillScript; stage: Stage } {
  const script = blankScript();
  const stage = script.stages[0];
  addActor(script, stage, 'PLAYER', 'A', { x: -4, y: -6 });
  addActor(script, stage, 'PLAYER', 'A', { x: 4, y: -6 });
  return { script, stage };
}

/** A straight stroke of points a metre apart, wobbling a little sideways like a finger does. */
function stroke(from: Point, to: Point): Point[] {
  const steps = Math.max(2, Math.round(Math.hypot(to.x - from.x, to.y - from.y)));
  return Array.from({ length: steps + 1 }, (_, i) => {
    const f = i / steps;
    const wobble = i === 0 || i === steps ? 0 : (i % 2 ? 0.05 : -0.05);
    return { x: from.x + (to.x - from.x) * f + wobble, y: from.y + (to.y - from.y) * f };
  });
}

describe('drill authoring', () => {
  it('numbers ids from the smallest one free', () => {
    expect(nextId('st', ['st1', 'st3'])).toBe('st2');
    expect(nextId('a', [])).toBe('a1');
  });

  // spec: ui.drill-draw-path
  it('simplifies a wobbly stroke to its corners', () => {
    expect(simplify(stroke({ x: 0, y: 0 }, { x: 0, y: 8 }))).toHaveLength(2);
    const corner = [...stroke({ x: 0, y: 0 }, { x: 0, y: 5 }), ...stroke({ x: 0, y: 5 }, { x: 5, y: 5 }).slice(1)];
    expect(simplify(corner).map((p) => [Math.round(p.x), Math.round(p.y)])).toEqual([
      [0, 0],
      [0, 5],
      [5, 5],
    ]);
  });

  // spec: ui.drill-draw-place
  it('places figures, each with the one Part it plays, and labels them by side', () => {
    const { script, stage } = twoPlayers();
    const b = addActor(script, stage, 'PLAYER', 'B', { x: 0, y: 3 });
    const goalie = addActor(script, stage, 'GOALIE', 'NEUTRAL', { x: 0, y: 10 });

    expect(stage.actors.map((a) => a.label)).toEqual(['A1', 'A2', 'B1', 'G']);
    expect(stage.parts.map((p) => p.actorId)).toEqual([...stage.actors.map((a) => a.id)]);
    expect(new Set(stage.parts.map((p) => p.id)).size).toBe(4);
    expect(b.side).toBe('B');
    expect(goalie.kind).toBe('GOALIE');
  });

  // spec: ui.drill-draw-place
  it('places objects on the rink and keeps them on it', () => {
    const { stage } = twoPlayers();
    const id = addProp(stage, 'CONE', { x: 30, y: 0 });

    expect(stage.props.find((p) => p.id === id)!.position.x).toBe(7);
  });

  // spec: ui.drill-draw-path
  it('turns a stroke from a figure into a run after that figure\'s last Step', () => {
    const { stage } = twoPlayers();

    const first = addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: -6 }, { x: -4, y: 4 }), { simplify: true });
    const second = addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: 4 }, { x: 0, y: 4 }), { simplify: true });

    expect(first.stepId).toBe('st1');
    expect(stage.steps[0]).toMatchObject({ partId: 'p1', type: 'RUN', after: '', path: [{ x: -4, y: 4 }], sketch: 0 });
    expect(second.stepId).toBe('st2');
    expect(stage.steps[1]).toMatchObject({ partId: 'p1', after: 'st1', afterEdge: 'END', path: [{ x: 0, y: 4 }] });
  });

  // spec: ui.drill-draw-path
  it('draws point by point without simplifying, and asks for a figure to start from', () => {
    const { stage } = twoPlayers();

    const result = addStepFromStroke(stage, 'DRIBBLE', [{ x: -4, y: -6 }, { x: -2, y: -3 }, { x: 0, y: -6 }]);
    const nowhere = addStepFromStroke(stage, 'RUN', [{ x: 0, y: 5 }, { x: 2, y: 5 }]);
    const short = addStepFromStroke(stage, 'RUN', [{ x: 4, y: -6 }, { x: 4.1, y: -6 }]);

    expect(stage.steps[0].path).toEqual([{ x: -2, y: -3 }, { x: 0, y: -6 }]);
    expect(result.stepId).toBe('st1');
    expect(nowhere).toMatchObject({ stepId: null, hint: 'Beginne bei einer Figur.' });
    expect(short.stepId).toBeNull();
    expect(stage.steps).toHaveLength(1);
  });

  // spec: ui.drill-pass-during-run, ui.drill-draw-path
  it('lets a pass start on the middle of a run, so it happens while the runner runs', () => {
    const { stage } = twoPlayers();
    stage.actors[0].hasBall = true;
    addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: -6 }, { x: -4, y: 4 }), { simplify: true });

    const pass = addStepFromStroke(stage, 'PASS', [{ x: -4, y: -1 }, { x: 4, y: -1 }]);

    expect(pass.stepId).toBe('st2');
    expect(stage.steps[1]).toMatchObject({ partId: 'p1', type: 'PASS', after: 'st1', afterEdge: 'DURING', afterFraction: 0.5 });
    const run = schedule(stage);
    expect(run.flights[0].start).toBeCloseTo(run.steps.find((s) => s.step.id === 'st1')!.end / 2);
    expect(run.flights[0].points[0].y).toBeCloseTo(-1);
    expect(pass.hint).toBeNull();
  });

  // spec: ui.drill-draw-pass-target
  it('aims a pass at a receiver who is running, ahead of where they start', () => {
    const { stage } = twoPlayers();
    stage.actors[0].hasBall = true;
    addStepFromStroke(stage, 'RUN', stroke({ x: 4, y: -6 }, { x: 4, y: 6 }), { simplify: true });

    const pass = addStepFromStroke(stage, 'PASS', [{ x: -4, y: -6 }, { x: 4, y: -6 }]);

    const step = stage.steps.find((s) => s.id === pass.stepId)!;
    expect(pass.lead).toBe(true);
    expect(step.targetPartId).toBe('p2');
    expect(step.path[0].y).toBeGreaterThan(-5);
    const run = schedule(stage);
    const landing = actorPositionAt(run, 'a2', run.flights[0].end);
    expect(Math.hypot(landing.x - step.path[0].x, landing.y - step.path[0].y)).toBeLessThan(0.2);
  });

  // spec: ui.drill-draw-pass-target
  it('keeps a pass aimed when the receiver\'s run changes', () => {
    const { stage } = twoPlayers();
    stage.actors[0].hasBall = true;
    addStepFromStroke(stage, 'RUN', stroke({ x: 4, y: -6 }, { x: 4, y: 6 }), { simplify: true });
    const pass = addStepFromStroke(stage, 'PASS', [{ x: -4, y: -6 }, { x: 4, y: -6 }]);
    const before = stage.steps.find((s) => s.id === pass.stepId)!.path[0].y;

    stage.steps[0].speed = 2;
    aimPass(stage, pass.stepId!);

    expect(stage.steps.find((s) => s.id === pass.stepId)!.path[0].y).toBeLessThan(before);
  });

  // spec: ui.drill-draw-ball-hint
  it('allows a pass from a figure without the ball, with a hint', () => {
    const { stage } = twoPlayers();

    const pass = addStepFromStroke(stage, 'PASS', [{ x: -4, y: -6 }, { x: 0, y: 0 }]);

    expect(pass.stepId).toBe('st1');
    expect(pass.hint).toContain('A1 hat den Ball nicht');
  });

  it('waits after a figure\'s last Step, and finds a figure where it ends up', () => {
    const { stage } = twoPlayers();
    addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: -6 }, { x: -4, y: 4 }), { simplify: true });

    const wait = addWaitStep(stage, 'a1');

    expect(stage.steps.find((s) => s.id === wait)).toMatchObject({ type: 'WAIT', after: 'st1', duration: 2 });
    expect(findActorAt(stage, { x: -4, y: 4 })).toBe('a1');
    expect(findActorAt(stage, { x: -4, y: -6 })).toBeNull();
    expect(endPositions(stage).get('a1')).toEqual({ x: -4, y: 4 });
  });

  it('removes a Step so that whatever waited for it waits for what it waited for', () => {
    const { stage } = twoPlayers();
    addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: -6 }, { x: -4, y: 4 }), { simplify: true });
    addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: 4 }, { x: 0, y: 4 }), { simplify: true });
    addStepFromStroke(stage, 'RUN', stroke({ x: 0, y: 4 }, { x: 0, y: 0 }), { simplify: true });

    removeStep(stage, 'st2');

    expect(stage.steps.map((s) => [s.id, s.after])).toEqual([
      ['st1', ''],
      ['st3', 'st1'],
    ]);
    expect(dependsOn(stage, 'st3', 'st1')).toBe(true);
    expect(dependsOn(stage, 'st1', 'st3')).toBe(false);
  });

  it('removes a figure with its Steps, and what pointed at it', () => {
    const { stage } = twoPlayers();
    stage.actors[0].hasBall = true;
    addStepFromStroke(stage, 'PASS', [{ x: -4, y: -6 }, { x: 4, y: -6 }]);
    addStepFromStroke(stage, 'RUN', stroke({ x: 4, y: -6 }, { x: 4, y: 0 }), { simplify: true });

    removeActor(stage, 'a2');

    expect(stage.actors.map((a) => a.id)).toEqual(['a1']);
    expect(stage.parts.map((p) => p.id)).toEqual(['p1']);
    expect(stage.steps.map((s) => [s.id, s.targetPartId])).toEqual([['st1', '']]);
  });

  // spec: ui.drill-timeline-retime
  it('re-times a Step from where its bar was dropped', () => {
    const { stage } = twoPlayers();
    addWaitStep(stage, 'a1', 2);
    addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: -6 }, { x: -4, y: 4 }), { simplify: true });
    addStepFromStroke(stage, 'RUN', stroke({ x: 4, y: -6 }, { x: 4, y: 0 }), { simplify: true });
    const other = stage.steps[2];
    other.after = 'st2';
    const run = schedule(stage);
    const anchor = run.steps.find((s) => s.step.id === 'st2')!;

    retimeStep(stage, other.id, anchor.start + (anchor.end - anchor.start) / 2);
    expect(other).toMatchObject({ after: 'st2', afterEdge: 'DURING', delay: 0 });
    expect(other.afterFraction).toBeCloseTo(0.5, 1);

    retimeStep(stage, other.id, anchor.end + 1.5);
    expect(other).toMatchObject({ after: 'st2', afterEdge: 'END', afterFraction: 0, delay: 1.5 });

    retimeStep(stage, other.id, 1);
    expect(other).toMatchObject({ after: '', delay: 1 });
    expect(schedule(stage).steps.find((s) => s.step.id === other.id)!.start).toBeCloseTo(1);
  });

  // spec: ui.drill-timeline-retime
  it('resizes a moving Step by its speed and a wait by its duration', () => {
    const { stage } = twoPlayers();
    addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: -6 }, { x: -4, y: 4 }), { simplify: true });
    const wait = addWaitStep(stage, 'a2')!;

    resizeStep(stage, 'st1', 5);
    resizeStep(stage, wait, 4.04);

    expect(stage.steps[0].speed).toBe(2);
    expect(stage.steps[1].duration).toBe(4);
  });

  // spec: ui.drill-draw-path
  it('inserts a waypoint into the segment a tap landed on, and removes one', () => {
    const { stage } = twoPlayers();
    addStepFromStroke(stage, 'RUN', [{ x: -4, y: -6 }, { x: -4, y: 0 }, { x: 0, y: 0 }]);

    const inserted = insertWaypoint(stage, 'st1', { x: -4.2, y: -3 });
    const missed = insertWaypoint(stage, 'st1', { x: 6, y: 6 });

    expect(inserted).toBe(true);
    expect(missed).toBe(false);
    expect(stage.steps[0].path).toEqual([{ x: -4.2, y: -3 }, { x: -4, y: 0 }, { x: 0, y: 0 }]);
    removeWaypoint(stage.steps[0], 0);
    expect(stage.steps[0].path).toEqual([{ x: -4, y: 0 }, { x: 0, y: 0 }]);
  });

  it('changes a figure everywhere it appears', () => {
    const { script, stage } = twoPlayers();
    addStage(script, 0);

    updateActor(script, stage.actors[0].id, { side: 'B', label: 'B9' });

    expect(script.stages.map((s) => s.actors[0])).toMatchObject([{ side: 'B', label: 'B9' }, { side: 'B', label: 'B9' }]);
  });

  // spec: ui.drill-draw-stages
  it('adds a Stage with the same figures and objects, and no Steps', () => {
    const { script, stage } = twoPlayers();
    addProp(stage, 'CONE', { x: 0, y: 0 });
    addStepFromStroke(stage, 'RUN', stroke({ x: -4, y: -6 }, { x: -4, y: 4 }), { simplify: true });

    const at = addStage(script, 0);

    expect(at).toBe(1);
    const added = script.stages[1];
    expect(added.name).toBe('Stufe 2');
    expect(added.id).toBe('s2');
    expect(added.actors.map((a) => a.id)).toEqual(['a1', 'a2']);
    expect(added.actors[0].start).toEqual({ x: -4, y: -6 });
    expect(added.props).toHaveLength(1);
    expect(added.steps).toEqual([]);
  });

  // spec: ui.drill-draw-stages
  it('moves and removes Stages, but never the last one', () => {
    const { script } = twoPlayers();
    addStage(script, 0);
    addStage(script, 1);
    script.stages[0].name = 'Erste';
    script.stages[1].name = 'Zweite';
    script.stages[2].name = 'Dritte';

    expect(moveStage(script, 0, 1)).toBe(1);
    expect(script.stages.map((s) => s.name)).toEqual(['Zweite', 'Erste', 'Dritte']);
    expect(moveStage(script, 0, -1)).toBe(0);
    expect(removeStage(script, 2)).toBe(1);
    removeStage(script, 0);
    expect(removeStage(script, 0)).toBe(0);
    expect(script.stages).toHaveLength(1);
  });
});
