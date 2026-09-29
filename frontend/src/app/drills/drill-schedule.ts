import { Point, Stage, Step, isSet } from './drill.model';
import { mirror } from './rink';

/**
 * Turns one run of a Stage into times (ADR-0018). Steps are ordered by "after Step X, at its
 * start or end, plus a delay", so a step's start depends on its anchor, and its length on where
 * its Actor stands when it starts. Everything here is pure: no canvas, no DOM, so it's unit-tested
 * on its own like `paint-engine.ts`.
 *
 * A run is played by a *cast* — which Actor plays which Part — and may be mirrored. Positions come
 * from the Parts, so a seamless loop can hand a Part to another Actor and the geometry stays put.
 */

/** Defaults when the script leaves a number at 0, in m/s or seconds. */
export const DEFAULT_SPEED: Record<Step['type'], number> = {
  RUN: 4.5,
  DRIBBLE: 3.5,
  PASS: 12,
  SHOT: 25,
  ROAM: 2,
  WAIT: 0,
};
const DEFAULT_DURATION = 2;
/** Loose movement inside a ROAM goes about this fast. */
const ROAM_SPEED = 2;

export type Cast = ReadonlyMap<string, string>; // partId -> actorId

export interface Movement {
  stepId: string;
  kind: 'MOVE' | 'ROAM' | 'WAIT';
  dribble: boolean;
  start: number;
  end: number;
  /** Polyline, starting where the Actor stood. A ROAM goes out along it and back, `cycles` times. */
  points: Point[];
  cycles: number;
}

export interface Flight {
  stepId: string;
  shot: boolean;
  start: number;
  end: number;
  points: Point[];
  passerId: string;
  /** Who holds the ball afterwards; null when it comes to rest (a shot, or a pass to nobody). */
  receiverId: string | null;
  /** Which ball flies; null when the passer had none — the script is inconsistent, show a stray ball. */
  ballId: string | null;
}

export interface ScheduledStep {
  step: Step;
  actorId: string;
  start: number;
  end: number;
}

export interface RunSchedule {
  duration: number;
  steps: ScheduledStep[];
  movements: Map<string, Movement[]>;
  flights: Flight[];
  startPositions: Map<string, Point>;
  /** Ball ids are the ids of the Actors that start with one. */
  ballsAtStart: string[];
  cast: Cast;
  mirrored: boolean;
  /** Problems the scheduler worked around: a missing anchor, a cycle. The validator should prevent them. */
  warnings: string[];
}

/** The cast the script describes: every Part played by its own Actor. */
export function initialCast(stage: Stage): Map<string, string> {
  return new Map(stage.parts.map((part) => [part.id, part.actorId]));
}

/** Where each Actor starts and whether it has a ball, given who plays which Part. */
export function startingLineup(stage: Stage, cast: Cast, mirrored: boolean): { positions: Map<string, Point>; balls: string[] } {
  const actorsById = new Map(stage.actors.map((actor) => [actor.id, actor]));
  const positions = new Map<string, Point>();
  const balls: string[] = [];
  for (const actor of stage.actors) {
    positions.set(actor.id, actor.start);
    if (!stage.parts.some((part) => cast.get(part.id) === actor.id) && actor.hasBall) {
      balls.push(actor.id);
    }
  }
  for (const part of stage.parts) {
    const player = cast.get(part.id);
    const original = actorsById.get(part.actorId);
    if (!player || !original) continue;
    positions.set(player, original.start);
    if (original.hasBall) balls.push(player);
  }
  if (mirrored) {
    for (const [id, point] of positions) positions.set(id, mirror(point));
  }
  return { positions, balls };
}

export function schedule(stage: Stage, cast: Cast = initialCast(stage), mirrored = false): RunSchedule {
  const warnings: string[] = [];
  const { positions: startPositions, balls } = startingLineup(stage, cast, mirrored);
  const place = (point: Point) => (mirrored ? mirror(point) : point);
  const movements = new Map<string, Movement[]>();
  const flights: Flight[] = [];
  const resolved = new Map<string, ScheduledStep>();
  const stepIds = new Set(stage.steps.map((step) => step.id));
  /** Steps whose anchor is ignored: part of a cycle, so they start with the run instead. */
  const detached = new Set<string>();
  const anchorOf = (step: Step) =>
    isSet(step.after) && stepIds.has(step.after) && !detached.has(step.id) ? step.after : null;

  let pending = stage.steps.filter((step) => {
    if (cast.get(step.partId)) return true;
    warnings.push(`step ${step.id}: nobody plays part ${step.partId}`);
    return false;
  });

  const positionAt = (actorId: string, time: number): Point =>
    positionFromMovements(movements.get(actorId) ?? [], startPositions.get(actorId) ?? { x: 0, y: 0 }, time);

  while (pending.length > 0) {
    const ready = pending.filter((step) => {
      const anchor = anchorOf(step);
      return anchor === null || resolved.has(anchor);
    });
    if (ready.length === 0) {
      warnings.push(`steps ${pending.map((s) => s.id).join(', ')} wait for each other in a cycle`);
      pending.forEach((step) => detached.add(step.id));
      continue;
    }
    const withStart = ready.map((step) => {
      if (isSet(step.after) && !stepIds.has(step.after)) {
        warnings.push(`step ${step.id}: waits for unknown step ${step.after}`);
      }
      const anchorId = anchorOf(step);
      const anchor = anchorId ? resolved.get(anchorId) : undefined;
      const anchorTime = anchor ? (step.afterEdge === 'START' ? anchor.start : anchor.end) : 0;
      return { step, start: anchorTime + Math.max(0, step.delay || 0) };
    });
    withStart.sort((a, b) => a.start - b.start);

    for (const { step, start } of withStart) {
      const actorId = cast.get(step.partId)!;
      const from = positionAt(actorId, start);
      const path = step.path.map(place);
      const end = start + duration(step, from, path);
      resolved.set(step.id, { step, actorId, start, end });

      if (step.type === 'PASS' || step.type === 'SHOT') {
        const receiverId = step.type === 'PASS' && isSet(step.targetPartId) ? cast.get(step.targetPartId) ?? null : null;
        flights.push({ stepId: step.id, shot: step.type === 'SHOT', start, end, points: [from, ...path], passerId: actorId, receiverId, ballId: null });
      } else {
        const list = movements.get(actorId) ?? [];
        list.push(movement(step, from, path, start, end));
        list.sort((a, b) => a.start - b.start);
        movements.set(actorId, list);
      }
    }
    pending = pending.filter((step) => !resolved.has(step.id));
  }

  assignBalls(flights, balls);
  const steps = [...resolved.values()].sort((a, b) => a.start - b.start || a.end - b.end);
  const duration_ = steps.reduce((max, s) => Math.max(max, s.end), 0);
  return { duration: duration_, steps, movements, flights, startPositions, ballsAtStart: balls, cast, mirrored, warnings };
}

function duration(step: Step, from: Point, path: Point[]): number {
  switch (step.type) {
    case 'RUN':
    case 'DRIBBLE':
    case 'PASS':
    case 'SHOT': {
      const speed = step.speed > 0 ? step.speed : DEFAULT_SPEED[step.type];
      return length([from, ...path]) / speed;
    }
    case 'ROAM':
    case 'WAIT':
      return step.duration > 0 ? step.duration : DEFAULT_DURATION;
  }
}

function movement(step: Step, from: Point, path: Point[], start: number, end: number): Movement {
  if (step.type === 'WAIT') {
    return { stepId: step.id, kind: 'WAIT', dribble: false, start, end, points: [from], cycles: 0 };
  }
  if (step.type === 'ROAM') {
    const points = [from, ...path];
    const outAndBack = 2 * length(points);
    const cycles = outAndBack === 0 ? 0 : Math.max(1, Math.round(((end - start) * ROAM_SPEED) / outAndBack));
    return { stepId: step.id, kind: 'ROAM', dribble: false, start, end, points, cycles };
  }
  return { stepId: step.id, kind: 'MOVE', dribble: step.type === 'DRIBBLE', start, end, points: [from, ...path], cycles: 0 };
}

/** Follows each ball from holder to holder, so every flight knows which ball it carries. */
function assignBalls(flights: Flight[], ballsAtStart: string[]): void {
  const holderOf = new Map<string, string | null>(ballsAtStart.map((id) => [id, id]));
  const events = [...flights].sort((a, b) => a.start - b.start);
  // A ball changes hands when its flight ends; process starts and ends in time order.
  const landings: Flight[] = [];
  for (const flight of events) {
    for (const landed of landings.filter((l) => l.end <= flight.start)) {
      if (landed.ballId) holderOf.set(landed.ballId, landed.receiverId);
      landings.splice(landings.indexOf(landed), 1);
    }
    const ball = [...holderOf.entries()].find(([, holder]) => holder === flight.passerId)?.[0] ?? null;
    flight.ballId = ball;
    if (ball) holderOf.set(ball, null); // in the air
    landings.push(flight);
  }
}

/** Where an Actor stands at a time, from its movements so far. */
export function positionFromMovements(list: Movement[], start: Point, time: number): Point {
  let position = start;
  for (const move of list) {
    if (move.start > time) break;
    if (time >= move.end) {
      position = move.kind === 'ROAM' ? move.points[0] : move.points[move.points.length - 1];
      continue;
    }
    const progress = move.end === move.start ? 1 : (time - move.start) / (move.end - move.start);
    if (move.kind === 'WAIT') return move.points[0];
    if (move.kind === 'ROAM') return alongOutAndBack(move.points, progress * move.cycles);
    return along(move.points, progress);
  }
  return position;
}

export function length(points: Point[]): number {
  let total = 0;
  for (let i = 1; i < points.length; i++) {
    total += Math.hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y);
  }
  return total;
}

/** The point a fraction (0..1) of the way along a polyline. */
export function along(points: Point[], fraction: number): Point {
  if (points.length === 1) return points[0];
  const total = length(points);
  if (total === 0) return points[points.length - 1];
  let remaining = Math.min(1, Math.max(0, fraction)) * total;
  for (let i = 1; i < points.length; i++) {
    const a = points[i - 1];
    const b = points[i];
    const segment = Math.hypot(b.x - a.x, b.y - a.y);
    if (remaining <= segment) {
      const f = segment === 0 ? 1 : remaining / segment;
      return { x: a.x + (b.x - a.x) * f, y: a.y + (b.y - a.y) * f };
    }
    remaining -= segment;
  }
  return points[points.length - 1];
}

/** Out along the polyline and back, `turns` times; the fractional part is how far into a turn. */
function alongOutAndBack(points: Point[], turns: number): Point {
  const phase = turns - Math.floor(turns);
  // Ease at the turning points, like someone shuffling side to side.
  const eased = (1 - Math.cos(phase * 2 * Math.PI)) / 2;
  return along(points, eased);
}
