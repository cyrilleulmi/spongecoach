import { Stage, Step } from './drill.model';
import { along, initialCast, length, schedule } from './drill-schedule';

function step(overrides: Partial<Step> & Pick<Step, 'id' | 'partId' | 'type'>): Step {
  return {
    path: [],
    targetPartId: '',
    after: '',
    afterEdge: 'END',
    delay: 0,
    speed: 0,
    duration: 0,
    sketch: 1,
    label: '',
    ...overrides,
  };
}

/** A1 (with the ball) passes to A2, who shoots at the top goal. */
function passAndShot(): Stage {
  return {
    id: 's1',
    name: 'Pass und Schuss',
    sketches: [1],
    area: 'FULL',
    actors: [
      { id: 'a1', kind: 'PLAYER', side: 'A', label: 'A1', start: { x: -3, y: -2 }, hasBall: true },
      { id: 'a2', kind: 'PLAYER', side: 'A', label: 'A2', start: { x: 3, y: 2 }, hasBall: false },
    ],
    parts: [
      { id: 'p1', name: 'Passgeberin', actorId: 'a1', nextPartId: '' },
      { id: 'p2', name: 'Schützin', actorId: 'a2', nextPartId: '' },
    ],
    props: [],
    steps: [
      step({ id: 'pass', partId: 'p1', type: 'PASS', path: [{ x: 3, y: 2 }], targetPartId: 'p2', speed: 10 }),
      step({ id: 'shot', partId: 'p2', type: 'SHOT', path: [{ x: 0, y: 9.65 }], after: 'pass', delay: 0.2, speed: 25 }),
    ],
    repetition: { mode: 'REPLAY', mirrored: false },
  };
}

describe('drill schedule', () => {
  it('times a step from its anchor: after its end, plus the delay', () => {
    const run = schedule(passAndShot());
    const [pass, shot] = run.steps;

    expect(pass.start).toBe(0);
    expect(pass.end).toBeCloseTo(Math.hypot(6, 4) / 10);
    expect(shot.start).toBeCloseTo(pass.end + 0.2);
    expect(run.duration).toBeCloseTo(shot.end);
  });

  it('starts a step together with its anchor when it waits for the anchor to start', () => {
    const stage = passAndShot();
    stage.steps.push(step({ id: 'run', partId: 'p2', type: 'RUN', path: [{ x: 3, y: 6 }], after: 'pass', afterEdge: 'START', speed: 4 }));

    const run = schedule(stage).steps.find((s) => s.step.id === 'run')!;

    expect(run.start).toBe(0);
    expect(run.end).toBeCloseTo(1);
  });

  it('measures a run from where the Actor stands when it starts', () => {
    const stage = passAndShot();
    stage.steps = [
      step({ id: 'r1', partId: 'p1', type: 'RUN', path: [{ x: -3, y: 2 }], speed: 4 }),
      step({ id: 'r2', partId: 'p1', type: 'RUN', path: [{ x: 5, y: 2 }], after: 'r1', speed: 4 }),
    ];

    const [r1, r2] = schedule(stage).steps;

    expect(r1.end).toBeCloseTo(1);
    expect(r2.end - r2.start).toBeCloseTo(2);
  });

  it('knows which ball each pass carries, and who holds it afterwards', () => {
    const run = schedule(passAndShot());
    const [pass, shot] = run.flights;

    expect(run.ballsAtStart).toEqual(['a1']);
    expect(pass).toMatchObject({ ballId: 'a1', passerId: 'a1', receiverId: 'a2', shot: false });
    expect(shot).toMatchObject({ ballId: 'a1', passerId: 'a2', receiverId: null, shot: true });
  });

  it('marks a pass by an Actor without a ball as carrying none', () => {
    const stage = passAndShot();
    stage.actors[0].hasBall = false;

    expect(schedule(stage).flights[0].ballId).toBeNull();
  });

  it('plays a cycle of waiting steps from the start, with a warning, and leaves the script alone', () => {
    const stage = passAndShot();
    stage.steps[0].after = 'shot';

    const run = schedule(stage);

    expect(run.warnings.join()).toContain('cycle');
    expect(run.steps).toHaveLength(2);
    expect(stage.steps[0].after).toBe('shot');
  });

  it('mirrors a run across the long axis', () => {
    const run = schedule(passAndShot(), initialCast(passAndShot()), true);

    expect(run.startPositions.get('a1')).toEqual({ x: 3, y: -2 });
    expect(run.flights[0].points[1]).toEqual({ x: -3, y: 2 });
  });

  it('places each Actor at the start of the Part it plays', () => {
    const swapped = new Map([
      ['p1', 'a2'],
      ['p2', 'a1'],
    ]);

    const run = schedule(passAndShot(), swapped);

    expect(run.startPositions.get('a2')).toEqual({ x: -3, y: -2 });
    expect(run.ballsAtStart).toEqual(['a2']);
    expect(run.flights[0].passerId).toBe('a2');
  });

  it('uses the default speed when a step leaves it at 0', () => {
    const stage = passAndShot();
    stage.steps[0].speed = 0;

    expect(schedule(stage).steps[0].end).toBeCloseTo(Math.hypot(6, 4) / 12);
  });
});

describe('polylines', () => {
  it('measures and walks along a polyline', () => {
    const points = [
      { x: 0, y: 0 },
      { x: 3, y: 0 },
      { x: 3, y: 4 },
    ];

    expect(length(points)).toBe(7);
    expect(along(points, 0.5)).toEqual({ x: 3, y: 0.5 });
    expect(along(points, 2)).toEqual({ x: 3, y: 4 });
  });
});
