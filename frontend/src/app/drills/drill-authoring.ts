import { Actor, ActorKind, DrillScript, Part, Point, PropKind, Side, Stage, Step, StepType, isSet } from './drill.model';
import { DEFAULT_SPEED, RunSchedule, length, positionFromMovements, schedule } from './drill-schedule';
import { clampToRink } from './rink';

/**
 * Everything the animator does to a script, as plain functions with no DOM (ADR-0020): placing
 * figures and objects, turning a stroke into a Step, snapping a pass to a receiver, re-timing a
 * Step from the timeline, and managing Stages. Like `drill-schedule.ts` and `paint-engine.ts` it is
 * unit-tested on its own.
 *
 * Functions change the Stage (or script) they are given; the editor passes them a copy.
 */

/**
 * How close, in metres, a touch must land to count as being on a figure or a waypoint. A fingertip
 * is about 10 mm, and on a phone the rink is drawn about 22 px per metre, so 1.1 m is a target of
 * roughly 44 px across.
 */
export const HIT_RADIUS = 1.1;
/** How close to a drawn path a touch must land to count as being on it. */
const PATH_HIT = 0.9;
const MIN_SPEED = 0.5;
const MAX_SPEED = 40;
/** A stroke shorter than this is a tap that wobbled, not a path. */
const MIN_STROKE = 0.5;

export const MOVING_TYPES: StepType[] = ['RUN', 'DRIBBLE', 'PASS', 'SHOT'];

// --- blank things -----------------------------------------------------------------

export function blankStage(id: string, name: string): Stage {
  return {
    id,
    name,
    sketches: [],
    area: 'FULL',
    actors: [],
    parts: [],
    props: [],
    steps: [],
    repetition: { mode: 'REPLAY', mirrored: false },
  };
}

export function blankScript(): DrillScript {
  return { stages: [blankStage('s1', 'Stufe 1')], assumptions: [] };
}

/** `prefix` plus the smallest number not taken yet: "st1", "st2", … */
export function nextId(prefix: string, taken: Iterable<string>): string {
  const used = new Set(taken);
  for (let n = 1; ; n++) {
    if (!used.has(prefix + n)) return prefix + n;
  }
}

// --- geometry ---------------------------------------------------------------------

export function distance(a: Point, b: Point): number {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

export function round1(value: number): number {
  return Math.round(value * 10) / 10;
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}

/** A point on the rink, rounded to 10 cm like a dragged handle. */
function onRink(point: Point): Point {
  const clamped = clampToRink(point);
  return { x: round1(clamped.x), y: round1(clamped.y) };
}

export function closestOnSegment(p: Point, a: Point, b: Point): { point: Point; t: number } {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  const squared = dx * dx + dy * dy;
  const t = squared === 0 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / squared));
  return { point: { x: a.x + dx * t, y: a.y + dy * t }, t };
}

/** The closest point of a polyline, with how far along it lies as a fraction of its length. */
export function closestOnPolyline(p: Point, points: Point[]): { point: Point; distance: number; fraction: number; segment: number } {
  let best = { point: points[0], distance: Infinity, fraction: 0, segment: 0 };
  const total = length(points);
  let travelled = 0;
  for (let i = 1; i < points.length; i++) {
    const segment = distance(points[i - 1], points[i]);
    const { point, t } = closestOnSegment(p, points[i - 1], points[i]);
    const d = distance(p, point);
    if (d < best.distance) {
      best = { point, distance: d, fraction: total === 0 ? 0 : (travelled + t * segment) / total, segment: i - 1 };
    }
    travelled += segment;
  }
  if (points.length === 1) best = { point: points[0], distance: distance(p, points[0]), fraction: 0, segment: 0 };
  return best;
}

/**
 * Ramer–Douglas–Peucker: a freehand stroke, hundreds of points, becomes the few corners a coach
 * would have drawn. `tolerance` is how far, in metres, a dropped point may lie from the result.
 */
export function simplify(points: Point[], tolerance = 0.4): Point[] {
  if (points.length <= 2) return points;
  const first = points[0];
  const last = points[points.length - 1];
  let farthest = 0;
  let index = 0;
  for (let i = 1; i < points.length - 1; i++) {
    const d = distance(points[i], closestOnSegment(points[i], first, last).point);
    if (d > farthest) {
      farthest = d;
      index = i;
    }
  }
  if (farthest <= tolerance) return [first, last];
  return [...simplify(points.slice(0, index + 1), tolerance).slice(0, -1), ...simplify(points.slice(index), tolerance)];
}

// --- reading the run --------------------------------------------------------------

/** Where an Actor stands at a time of the first run. */
export function actorPositionAt(run: RunSchedule, actorId: string, time: number): Point {
  return positionFromMovements(run.movements.get(actorId) ?? [], run.startPositions.get(actorId) ?? { x: 0, y: 0 }, time);
}

/** Where every Actor stands when its Steps are done: the "ghost" a next path would start from. */
export function endPositions(stage: Stage): Map<string, Point> {
  const run = schedule(stage);
  return new Map(stage.actors.map((actor) => [actor.id, actorPositionAt(run, actor.id, run.duration)]));
}

export function partOf(stage: Stage, actorId: string): Part | undefined {
  return stage.parts.find((part) => part.actorId === actorId);
}

/** The Step of this Actor that ends last, if it has any. */
function lastStepOf(stage: Stage, run: RunSchedule, actorId: string): Step | undefined {
  const partId = partOf(stage, actorId)?.id;
  const own = run.steps.filter((scheduled) => scheduled.step.partId === partId);
  return own.length === 0 ? undefined : own.reduce((a, b) => (b.end >= a.end ? b : a)).step;
}

export type Origin =
  | { kind: 'actor'; actorId: string }
  | { kind: 'path'; stepId: string; actorId: string; fraction: number };

/**
 * What a stroke that starts at `point` starts on: a figure (where it stands when its Steps are
 * done), or, if none is near, a run or dribble drawn on the rink, at the fraction of it the point
 * lies along. A pass drawn off the middle of a run is what lets it happen mid-run.
 */
export function findOrigin(stage: Stage, point: Point, allowPath: boolean): Origin | null {
  const ghosts = endPositions(stage);
  let best: { actorId: string; d: number } | null = null;
  for (const [actorId, at] of ghosts) {
    const d = distance(point, at);
    if (d <= HIT_RADIUS && (!best || d < best.d)) best = { actorId, d };
  }
  if (best) return { kind: 'actor', actorId: best.actorId };
  if (!allowPath) return null;

  const run = schedule(stage);
  let hit: { stepId: string; actorId: string; fraction: number; d: number } | null = null;
  for (const [actorId, movements] of run.movements) {
    for (const move of movements) {
      if (move.kind !== 'MOVE') continue;
      const close = closestOnPolyline(point, move.points);
      if (close.distance <= PATH_HIT && (!hit || close.distance < hit.d)) {
        hit = { stepId: move.stepId, actorId, fraction: close.fraction, d: close.distance };
      }
    }
  }
  return hit ? { kind: 'path', stepId: hit.stepId, actorId: hit.actorId, fraction: Math.min(0.95, Math.max(0.05, round2(hit.fraction))) } : null;
}

/** The figure nearest to a touch, where it stands when its Steps are done. */
export function findActorAt(stage: Stage, point: Point): string | null {
  const origin = findOrigin(stage, point, false);
  return origin ? origin.actorId : null;
}

// --- figures and objects ----------------------------------------------------------

function allActorIds(script: DrillScript): string[] {
  return script.stages.flatMap((stage) => stage.actors.map((actor) => actor.id));
}

function nextLabel(stage: Stage, kind: ActorKind, side: Side): string {
  const base = kind === 'GOALIE' ? 'G' : kind === 'COACH' ? 'T' : side === 'NEUTRAL' ? 'S' : side;
  const taken = new Set(stage.actors.map((actor) => actor.label));
  if (kind !== 'PLAYER' && !taken.has(base)) return base;
  for (let n = 1; ; n++) {
    if (!taken.has(base + n)) return base + n;
  }
}

/** Places a figure and gives it the one Part it plays; the coach never has to think about Parts. */
export function addActor(script: DrillScript, stage: Stage, kind: ActorKind, side: Side, at: Point): Actor {
  const actor: Actor = {
    id: nextId('a', allActorIds(script)),
    kind,
    side,
    label: nextLabel(stage, kind, side),
    start: onRink(at),
    hasBall: false,
  };
  stage.actors.push(actor);
  const partId = nextId('p', script.stages.flatMap((s) => s.parts.map((part) => part.id)));
  stage.parts.push({ id: partId, name: `Rolle ${stage.parts.length + 1}`, actorId: actor.id, nextPartId: '' });
  return actor;
}

/** Removes a figure with its Part and Steps, and whatever pointed at them. */
export function removeActor(stage: Stage, actorId: string): void {
  const partIds = new Set(stage.parts.filter((part) => part.actorId === actorId).map((part) => part.id));
  for (const step of stage.steps.filter((s) => partIds.has(s.partId))) {
    removeStep(stage, step.id);
  }
  stage.parts = stage.parts.filter((part) => !partIds.has(part.id));
  stage.actors = stage.actors.filter((actor) => actor.id !== actorId);
  for (const part of stage.parts) {
    if (partIds.has(part.nextPartId ?? '')) part.nextPartId = '';
  }
  for (const step of stage.steps) {
    if (partIds.has(step.targetPartId ?? '')) step.targetPartId = '';
  }
}

/** Kind, side and label are one figure across every Stage, so a change applies to all of them. */
export function updateActor(script: DrillScript, actorId: string, patch: Partial<Pick<Actor, 'kind' | 'side' | 'label'>>): void {
  for (const stage of script.stages) {
    const actor = stage.actors.find((a) => a.id === actorId);
    if (actor) Object.assign(actor, patch);
  }
}

export function addProp(stage: Stage, kind: PropKind, at: Point): string {
  const id = nextId('c', stage.props.map((prop) => prop.id));
  stage.props.push({ id, kind, position: onRink(at) });
  return id;
}

export function removeProp(stage: Stage, propId: string): void {
  stage.props = stage.props.filter((prop) => prop.id !== propId);
}

// --- Steps ------------------------------------------------------------------------

export interface StrokeResult {
  stepId: string | null;
  /** Why nothing was added, or something the coach should know about what was. */
  hint: string | null;
  /** The Step is a pass to a receiver, aimed ahead of them; the editor keeps it aimed. */
  lead: boolean;
}

/**
 * Turns what the coach drew into a Step. The stroke starts on a figure, so the Step is that
 * figure's Part's, timed after its last Step; or, for a pass or shot, on a run drawn on the rink,
 * so it starts `DURING` that run, at the fraction where the stroke began. `stroke[0]` is where it
 * started, the rest is the path (simplified when it was drawn freehand). A pass that ends on
 * another figure, or on its path, goes to that figure, aimed ahead of it.
 */
export function addStepFromStroke(
  stage: Stage,
  type: Exclude<StepType, 'WAIT'>,
  stroke: Point[],
  options: { simplify?: boolean } = {},
): StrokeResult {
  const flies = type === 'PASS' || type === 'SHOT';
  const origin = stroke.length > 0 ? findOrigin(stage, stroke[0], flies) : null;
  if (!origin) {
    return { stepId: null, hint: flies ? 'Beginne bei einer Figur oder auf einem Laufweg.' : 'Beginne bei einer Figur.', lead: false };
  }
  const points = options.simplify ? simplify(stroke) : stroke;
  const path = points.slice(1).map(onRink);
  if (path.length === 0 || distance(points[0], points[points.length - 1]) < MIN_STROKE) {
    return { stepId: null, hint: 'Der Weg ist zu kurz.', lead: false };
  }
  const part = partOf(stage, origin.actorId);
  if (!part) return { stepId: null, hint: null, lead: false };

  const run = schedule(stage);
  const step: Step = {
    id: nextId('st', stage.steps.map((s) => s.id)),
    partId: part.id,
    type,
    path,
    targetPartId: '',
    after: '',
    afterEdge: 'END',
    afterFraction: 0,
    delay: 0,
    speed: type === 'ROAM' ? 0 : DEFAULT_SPEED[type],
    duration: type === 'ROAM' ? 3 : 0,
    sketch: 0,
    label: String(stage.steps.length + 1),
  };
  if (origin.kind === 'path') {
    step.after = origin.stepId;
    step.afterEdge = 'DURING';
    step.afterFraction = origin.fraction;
  } else {
    const last = lastStepOf(stage, run, origin.actorId);
    step.after = last?.id ?? '';
  }
  stage.steps.push(step);

  let lead = false;
  if (type === 'PASS') {
    const receiver = receiverNear(stage, step, path[path.length - 1]);
    if (receiver) {
      step.targetPartId = receiver.id;
      aimPass(stage, step.id);
      lead = true;
    }
  }
  return { stepId: step.id, hint: ballHint(stage, step), lead };
}

/** A WAIT for a figure, after its last Step. */
export function addWaitStep(stage: Stage, actorId: string, seconds = 2): string | null {
  const part = partOf(stage, actorId);
  if (!part) return null;
  const run = schedule(stage);
  const step: Step = {
    id: nextId('st', stage.steps.map((s) => s.id)),
    partId: part.id,
    type: 'WAIT',
    path: [],
    targetPartId: '',
    after: lastStepOf(stage, run, actorId)?.id ?? '',
    afterEdge: 'END',
    afterFraction: 0,
    delay: 0,
    speed: 0,
    duration: seconds,
    sketch: 0,
    label: String(stage.steps.length + 1),
  };
  stage.steps.push(step);
  return step.id;
}

/** The Part of the figure a pass ends on: at its start, where it ends up, or along its path. */
function receiverNear(stage: Stage, pass: Step, end: Point): Part | undefined {
  const passer = stage.parts.find((part) => part.id === pass.partId)?.actorId;
  const run = schedule(stage);
  const ends = endPositions(stage);
  let best: { part: Part; d: number } | null = null;
  for (const part of stage.parts) {
    if (part.actorId === passer) continue;
    const near = [
      distance(end, run.startPositions.get(part.actorId) ?? end),
      distance(end, ends.get(part.actorId) ?? end),
      ...(run.movements.get(part.actorId) ?? []).filter((m) => m.kind === 'MOVE').map((m) => closestOnPolyline(end, m.points).distance),
    ];
    const d = Math.min(...near);
    if (d <= PATH_HIT && (!best || d < best.d)) best = { part, d };
  }
  return best?.part;
}

/**
 * Aims a pass ahead of a moving receiver: the ball ends where they will be when it lands. Landing
 * time depends on where it lands, so this settles it by trying a few times, which converges quickly
 * because a pass is much faster than a run.
 */
export function aimPass(stage: Stage, stepId: string): void {
  const step = stage.steps.find((s) => s.id === stepId);
  const receiver = step && stage.parts.find((part) => part.id === step.targetPartId)?.actorId;
  if (!step || step.type !== 'PASS' || !receiver) return;
  let guess = step.path[step.path.length - 1];
  for (let i = 0; i < 6; i++) {
    step.path = [onRink(guess)];
    const run = schedule(stage);
    const flight = run.flights.find((f) => f.stepId === stepId);
    if (!flight) return;
    const next = actorPositionAt(run, receiver, flight.end);
    const settled = distance(next, guess) < 0.05;
    guess = next;
    if (settled) break;
  }
  step.path = [onRink(guess)];
}

/** Says so when the figure that passes or shoots does not have the ball by then; it is allowed. */
function ballHint(stage: Stage, step: Step): string | null {
  if (step.type !== 'PASS' && step.type !== 'SHOT') return null;
  const flight = schedule(stage).flights.find((f) => f.stepId === step.id);
  if (!flight || flight.ballId !== null) return null;
  const actorId = stage.parts.find((part) => part.id === step.partId)?.actorId;
  const label = stage.actors.find((actor) => actor.id === actorId)?.label ?? '?';
  return `Hinweis: ${label} hat den Ball nicht. Wähle bei der Figur mit dem Ball „startet mit Ball“.`;
}

/**
 * Removes a Step. Whatever waited for it now waits for what it waited for, so nothing is left
 * hanging on a Step that is gone.
 */
export function removeStep(stage: Stage, stepId: string): void {
  const step = stage.steps.find((s) => s.id === stepId);
  if (!step) return;
  for (const other of stage.steps) {
    if (other.after === step.id) {
      other.after = step.after;
      other.afterEdge = step.afterEdge;
      other.afterFraction = step.afterFraction;
      other.delay += step.after ? 0 : step.delay;
    }
  }
  stage.steps = stage.steps.filter((s) => s.id !== step.id);
}

/** Whether `candidate` waits, however indirectly, for `stepId`: it must not become its anchor. */
export function dependsOn(stage: Stage, candidate: string, stepId: string): boolean {
  const seen = new Set<string>();
  let current = stage.steps.find((s) => s.id === candidate);
  while (current && isSet(current.after) && !seen.has(current.id)) {
    if (current.after === stepId) return true;
    seen.add(current.id);
    current = stage.steps.find((s) => s.id === current!.after);
  }
  return false;
}

/**
 * Moves a Step to start at `seconds` of the run, as when a bar is dragged on the timeline. Its
 * anchor stays the same Step, but how it waits changes with where it lands: inside the anchor it
 * starts `DURING` it, after the anchor it starts after its end plus a delay, and before the anchor
 * it no longer waits for it, so it starts with the run plus a delay. Whatever waits for this Step
 * moves along on its own.
 */
export function retimeStep(stage: Stage, stepId: string, seconds: number): void {
  const step = stage.steps.find((s) => s.id === stepId);
  if (!step) return;
  const run = schedule(stage);
  const t = Math.max(0, round1(seconds));
  const anchor = isSet(step.after) ? run.steps.find((s) => s.step.id === step.after) : undefined;
  const slack = 0.05;
  if (!anchor || t < anchor.start - slack) {
    step.after = '';
    step.afterEdge = 'END';
    step.afterFraction = 0;
    step.delay = t;
  } else if (t <= anchor.start + slack) {
    step.afterEdge = 'START';
    step.afterFraction = 0;
    step.delay = 0;
  } else if (t < anchor.end - slack) {
    step.afterEdge = 'DURING';
    step.afterFraction = Math.min(0.98, Math.max(0.02, round2((t - anchor.start) / (anchor.end - anchor.start))));
    step.delay = 0;
  } else {
    step.afterEdge = 'END';
    step.afterFraction = 0;
    step.delay = Math.max(0, round1(t - anchor.end));
  }
}

/**
 * Gives a Step a new length in seconds, as when the grip of a bar is dragged: a moving Step keeps
 * its path and changes its speed; a ROAM or WAIT changes its duration.
 */
export function resizeStep(stage: Stage, stepId: string, seconds: number): void {
  const step = stage.steps.find((s) => s.id === stepId);
  if (!step) return;
  if (step.type === 'ROAM' || step.type === 'WAIT') {
    step.duration = Math.max(0.5, round1(seconds));
    return;
  }
  const run = schedule(stage);
  const points =
    run.flights.find((f) => f.stepId === stepId)?.points ??
    [...run.movements.values()].flat().find((m) => m.stepId === stepId)?.points ??
    [];
  const metres = length(points);
  if (metres <= 0 || seconds <= 0) return;
  step.speed = Math.min(MAX_SPEED, Math.max(MIN_SPEED, round1(metres / seconds)));
}

/** Where a Step's path starts: where its figure stands when it begins. */
export function pathStart(stage: Stage, stepId: string): Point | null {
  const run = schedule(stage);
  const flight = run.flights.find((f) => f.stepId === stepId);
  if (flight) return flight.points[0];
  const move = [...run.movements.values()].flat().find((m) => m.stepId === stepId);
  return move ? move.points[0] : null;
}

/** Adds a waypoint on the segment of a Step's path that a tap landed nearest to; false if none is near. */
export function insertWaypoint(stage: Stage, stepId: string, point: Point): boolean {
  const step = stage.steps.find((s) => s.id === stepId);
  const from = pathStart(stage, stepId);
  if (!step || !from || step.type === 'WAIT') return false;
  const close = closestOnPolyline(point, [from, ...step.path]);
  if (close.distance > PATH_HIT) return false;
  step.path.splice(close.segment, 0, onRink(point));
  return true;
}

export function removeWaypoint(step: Step, index: number): void {
  const least = step.type === 'WAIT' ? 0 : 1;
  if (step.path.length > least && index >= 0 && index < step.path.length) step.path.splice(index, 1);
}

// --- Stages -----------------------------------------------------------------------

/** A new Stage after `from`: the same figures and objects where they start, and no Steps yet. */
export function addStage(script: DrillScript, from: number): number {
  const source = script.stages[from] ?? script.stages[script.stages.length - 1];
  const stage = blankStage(nextId('s', script.stages.map((s) => s.id)), `Stufe ${script.stages.length + 1}`);
  if (source) {
    stage.area = source.area;
    stage.actors = source.actors.map((actor) => ({ ...actor, start: { ...actor.start }, hasBall: actor.hasBall }));
    stage.parts = source.parts.map((part) => ({ ...part, nextPartId: '' }));
    stage.props = source.props.map((prop) => ({ ...prop, position: { ...prop.position } }));
  }
  const at = Math.max(0, from) + 1;
  script.stages.splice(at, 0, stage);
  return at;
}

/** Removes a Stage unless it is the only one; returns the index to show afterwards. */
export function removeStage(script: DrillScript, index: number): number {
  if (script.stages.length > 1) script.stages.splice(index, 1);
  return Math.min(index, script.stages.length - 1);
}

/** Moves a Stage up (-1) or down (+1); returns its new index. */
export function moveStage(script: DrillScript, index: number, by: -1 | 1): number {
  const to = index + by;
  if (to < 0 || to >= script.stages.length) return index;
  [script.stages[index], script.stages[to]] = [script.stages[to], script.stages[index]];
  return to;
}

export function setStageSketches(stage: Stage, position: number, on: boolean): void {
  const set = new Set(stage.sketches);
  if (on) set.add(position);
  else set.delete(position);
  stage.sketches = [...set].sort((a, b) => a - b);
}

/** A script worth saving as a new Drill: it has at least one figure. */
export function isDrawn(script: DrillScript): boolean {
  return script.stages.some((stage) => stage.actors.length > 0);
}
