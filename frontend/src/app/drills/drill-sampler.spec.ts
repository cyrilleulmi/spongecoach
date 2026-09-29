import { Stage, Step } from './drill.model';
import { schedule } from './drill-schedule';
import { HANDOVER, Playback, REPLAY_PAUSE, handOn, sampleRun } from './drill-sampler';

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

/** A1 (with the ball) passes 10 m to A2 at 10 m/s, who shoots. */
function passAndShot(): Stage {
  return {
    id: 's1',
    name: 'Pass und Schuss',
    sketches: [1],
    area: 'FULL',
    actors: [
      { id: 'a1', kind: 'PLAYER', side: 'A', label: 'A1', start: { x: -5, y: 0 }, hasBall: true },
      { id: 'a2', kind: 'PLAYER', side: 'A', label: 'A2', start: { x: 5, y: 0 }, hasBall: false },
    ],
    parts: [
      { id: 'p1', name: 'Passgeberin', actorId: 'a1', nextPartId: 'p2' },
      { id: 'p2', name: 'Schützin', actorId: 'a2', nextPartId: 'p1' },
    ],
    props: [],
    steps: [
      step({ id: 'pass', partId: 'p1', type: 'PASS', path: [{ x: 5, y: 0 }], targetPartId: 'p2', speed: 10 }),
      step({ id: 'shot', partId: 'p2', type: 'SHOT', path: [{ x: 0, y: 9.65 }], after: 'pass', speed: 25 }),
    ],
    repetition: { mode: 'REPLAY', mirrored: false },
  };
}

describe('sampling a run', () => {
  it('flies the ball along the pass, then hands it to the receiver', () => {
    const stage = passAndShot();
    stage.steps[1].delay = 0.5;
    const run = schedule(stage);

    const midPass = sampleRun(stage, run, 0.5).balls[0];
    expect(midPass).toMatchObject({ id: 'a1', inFlight: true });
    expect(midPass.x).toBeCloseTo(0);

    const received = sampleRun(stage, run, 1.2).balls[0];
    expect(received.inFlight).toBe(false);
    expect(received.x).toBeCloseTo(5.35);
  });

  it('leaves a shot ball in the goal', () => {
    const stage = passAndShot();
    const run = schedule(stage);

    const ball = sampleRun(stage, run, run.duration + 5).balls[0];

    expect(ball).toMatchObject({ x: 0, y: 9.65, inFlight: false });
  });

  it('moves a runner along the path and keeps the ball at their stick', () => {
    const stage = passAndShot();
    stage.steps = [step({ id: 'run', partId: 'p1', type: 'RUN', path: [{ x: -5, y: 8 }], speed: 4 })];
    const run = schedule(stage);

    const frame = sampleRun(stage, run, 1);

    expect(frame.actors[0]).toMatchObject({ x: -5, y: 4 });
    expect(frame.balls[0]).toMatchObject({ x: -4.65, y: 4.25 });
  });

  it('swings the ball from side to side during a dribble', () => {
    const stage = passAndShot();
    stage.steps = [step({ id: 'dribble', partId: 'p1', type: 'DRIBBLE', path: [{ x: -5, y: 8 }], speed: 4 })];
    const run = schedule(stage);

    const xs = [0.1, 0.2, 0.3].map((t) => sampleRun(stage, run, t).balls[0].x);

    expect(new Set(xs.map((x) => x.toFixed(2))).size).toBeGreaterThan(1);
  });

  it('brings a roaming Actor back to where it started', () => {
    const stage = passAndShot();
    stage.steps = [step({ id: 'roam', partId: 'p2', type: 'ROAM', path: [{ x: 5, y: 2 }], duration: 4 })];
    const run = schedule(stage);

    expect(sampleRun(stage, run, 4).actors[1]).toMatchObject({ x: 5, y: 0 });
    expect(sampleRun(stage, run, 1).actors[1].y).toBeGreaterThan(0);
  });

  it('shows a pass by someone without a ball as a stray ball, rather than hiding it', () => {
    const stage = passAndShot();
    stage.actors[0].hasBall = false;
    const run = schedule(stage);

    expect(sampleRun(stage, run, 0.5).balls).toEqual([expect.objectContaining({ id: 'stray-pass', inFlight: true })]);
  });
});

describe('the loop', () => {
  it('replays one run, holding the last frame before starting over', () => {
    const playback = new Playback(passAndShot());
    const run = playback.runs[0].run;

    expect(playback.runs).toHaveLength(1);
    expect(playback.duration).toBeCloseTo(run.duration + REPLAY_PAUSE);
    expect(playback.frameAt(playback.duration + 0.5).balls[0].inFlight).toBe(true);
  });

  it('plays every other run from the other side when mirrored', () => {
    const stage = passAndShot();
    stage.repetition.mirrored = true;
    const playback = new Playback(stage);

    expect(playback.runs).toHaveLength(2);
    expect(playback.frameAt(playback.runs[1].start).actors[0]).toMatchObject({ x: 5, y: 0 });
  });

  it('hands the Parts on in a seamless loop, walking everyone to their next start', () => {
    const stage = passAndShot();
    stage.repetition.mode = 'SEAMLESS';
    const playback = new Playback(stage);
    const first = playback.runs[0];

    expect(playback.runs).toHaveLength(2);
    expect(playback.runs[1].run.cast.get('p1')).toBe('a2');

    const handedOver = playback.frameAt(first.playEnd + HANDOVER);
    expect(handedOver.actors.find((a) => a.id === 'a2')).toMatchObject({ x: -5, y: 0 });
    expect(handedOver.balls).toEqual([expect.objectContaining({ id: 'a2' })]);
  });

  it('keeps the cast when the hand-over is not a complete rotation', () => {
    const stage = passAndShot();
    stage.parts[1].nextPartId = '';

    expect(handOn(stage, new Map([['p1', 'a1'], ['p2', 'a2']])).get('p1')).toBe('a1');
  });

  it('steps from one step start to the next', () => {
    const playback = new Playback(passAndShot());
    const shotStart = playback.runs[0].run.steps[1].start;

    expect(playback.nextStepTime(0)).toBeCloseTo(shotStart);
    expect(playback.nextStepTime(shotStart)).toBeCloseTo(playback.duration);
  });
});
