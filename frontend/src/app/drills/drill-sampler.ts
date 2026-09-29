import { ActorKind, Point, Side, Stage } from './drill.model';
import {
  Cast,
  Flight,
  RunSchedule,
  along,
  initialCast,
  positionFromMovements,
  schedule,
  startingLineup,
} from './drill-schedule';

/**
 * Where everything is at a moment of the animation (ADR-0018). `sampleRun` answers for one run;
 * `Playback` strings runs into the loop the coach watches: `REPLAY` pauses and starts over,
 * `SEAMLESS` walks every Actor to the start of its next Part and hands the Parts on, and
 * `mirrored` plays every other run from the other side. Pure, like `drill-schedule.ts`.
 */

export interface ActorFrame {
  id: string;
  label: string;
  kind: ActorKind;
  side: Side;
  x: number;
  y: number;
}

export interface BallFrame {
  id: string;
  x: number;
  y: number;
  inFlight: boolean;
}

export interface Frame {
  actors: ActorFrame[];
  balls: BallFrame[];
}

/** How far from its holder's centre a carried ball sits, in metres. */
const CARRY_OFFSET = { x: 0.35, y: 0.25 };
/** A dribble swings the ball this far to either side, this many times a second. */
const DRIBBLE_SWING = 0.35;
const DRIBBLE_HZ = 2.5;

export function sampleRun(stage: Stage, run: RunSchedule, time: number): Frame {
  const positions = new Map<string, Point>();
  const actors: ActorFrame[] = stage.actors.map((actor) => {
    const point = positionFromMovements(run.movements.get(actor.id) ?? [], run.startPositions.get(actor.id) ?? actor.start, time);
    positions.set(actor.id, point);
    return { id: actor.id, label: actor.label, kind: actor.kind, side: actor.side, x: point.x, y: point.y };
  });
  return { actors, balls: ballsAt(run, positions, time) };
}

function ballsAt(run: RunSchedule, positions: Map<string, Point>, time: number): BallFrame[] {
  const balls: BallFrame[] = [];
  const flights = [...run.flights].sort((a, b) => a.start - b.start);
  for (const ballId of run.ballsAtStart) {
    let holder: string | null = ballId;
    let rest: Point | null = null;
    let frame: BallFrame | null = null;
    for (const flight of flights.filter((f) => f.ballId === ballId)) {
      if (time < flight.start) break;
      if (time < flight.end) {
        frame = inFlight(ballId, flight, time);
        break;
      }
      holder = flight.receiverId;
      rest = holder ? null : flight.points[flight.points.length - 1];
    }
    if (!frame) {
      const at = holder ? carried(run, holder, positions.get(holder)!, time) : rest!;
      frame = { id: ballId, x: at.x, y: at.y, inFlight: false };
    }
    balls.push(frame);
  }
  // A pass by someone without a ball still shows, so the coach sees what the script says.
  for (const flight of run.flights.filter((f) => f.ballId === null && time >= f.start && time < f.end)) {
    balls.push(inFlight(`stray-${flight.stepId}`, flight, time));
  }
  return balls;
}

function inFlight(id: string, flight: Flight, time: number): BallFrame {
  const point = along(flight.points, (time - flight.start) / (flight.end - flight.start || 1));
  return { id, x: point.x, y: point.y, inFlight: true };
}

/** At the holder's stick; swinging side to side while they dribble. */
function carried(run: RunSchedule, holder: string, at: Point, time: number): Point {
  const move = (run.movements.get(holder) ?? []).find((m) => m.dribble && time >= m.start && time < m.end);
  if (!move) return { x: at.x + CARRY_OFFSET.x, y: at.y + CARRY_OFFSET.y };
  const ahead = along(move.points, Math.min(1, (time - move.start) / (move.end - move.start) + 0.02));
  const dx = ahead.x - at.x;
  const dy = ahead.y - at.y;
  const norm = Math.hypot(dx, dy) || 1;
  const swing = Math.sin(time * 2 * Math.PI * DRIBBLE_HZ) * DRIBBLE_SWING;
  // In front of the player, swung across the direction of travel.
  return { x: at.x + (dx / norm) * 0.4 - (dy / norm) * swing, y: at.y + (dy / norm) * 0.4 + (dx / norm) * swing };
}

// --- the loop --------------------------------------------------------------------

/** After a REPLAY run, the last frame holds this long before starting over. */
export const REPLAY_PAUSE = 1;
/** A SEAMLESS hand-over walks everyone to their next start in this long. */
export const HANDOVER = 1.5;
/** A seamless rotation can take many runs to come back round; the loop stops growing here. */
const MAX_RUNS = 12;

export interface RunSlot {
  index: number;
  start: number;
  /** When the run's own steps end; the hand-over or pause follows until `end`. */
  playEnd: number;
  end: number;
  run: RunSchedule;
}

export class Playback {
  readonly runs: RunSlot[] = [];
  /** One full loop: the runs until the cast and the side come back to how they started. */
  readonly duration: number;

  constructor(readonly stage: Stage) {
    const seamless = stage.repetition?.mode === 'SEAMLESS';
    const mirrored = !!stage.repetition?.mirrored;
    const first = initialCast(stage);
    let cast: Cast = first;
    let start = 0;
    for (let index = 0; index < MAX_RUNS; index++) {
      const run = schedule(stage, cast, mirrored && index % 2 === 1);
      const playEnd = start + run.duration;
      const end = playEnd + (seamless ? HANDOVER : REPLAY_PAUSE);
      this.runs.push({ index, start, playEnd, end, run });
      start = end;
      const next = seamless ? handOn(stage, cast) : cast;
      const backToStart = sameCast(next, first) && (!mirrored || index % 2 === 1);
      if (backToStart) break;
      cast = next;
    }
    this.duration = start;
  }

  /** Which run a time falls in, wrapping round the loop. */
  slotAt(time: number): RunSlot {
    const t = this.wrap(time);
    return this.runs.find((slot) => t < slot.end) ?? this.runs[this.runs.length - 1];
  }

  wrap(time: number): number {
    return this.duration > 0 ? ((time % this.duration) + this.duration) % this.duration : 0;
  }

  frameAt(time: number): Frame {
    const t = this.wrap(time);
    const slot = this.slotAt(t);
    if (t <= slot.playEnd) {
      return sampleRun(this.stage, slot.run, t - slot.start);
    }
    const last = sampleRun(this.stage, slot.run, slot.run.duration);
    if (this.stage.repetition?.mode !== 'SEAMLESS') {
      return last;
    }
    // The hand-over: everyone walks from where the run left them to their next start.
    const nextSlot = this.runs[(slot.index + 1) % this.runs.length];
    const next = nextSlot.run.startPositions;
    const progress = Math.min(1, (t - slot.playEnd) / HANDOVER);
    const eased = progress * progress * (3 - 2 * progress);
    const actors = last.actors.map((actor) => {
      const target = next.get(actor.id) ?? actor;
      return { ...actor, x: actor.x + (target.x - actor.x) * eased, y: actor.y + (target.y - actor.y) * eased };
    });
    const balls = nextSlot.run.ballsAtStart.map((holder) => {
      const at = actors.find((actor) => actor.id === holder)!;
      return { id: holder, x: at.x + CARRY_OFFSET.x, y: at.y + CARRY_OFFSET.y, inFlight: false };
    });
    return { actors, balls };
  }

  /** The start of the step after `time` in the current run, or the next run's start; for stepping through. */
  nextStepTime(time: number): number {
    const t = this.wrap(time);
    const base = time - t;
    const slot = this.slotAt(t);
    const local = t - slot.start;
    const next = slot.run.steps.map((s) => s.start).find((start) => start > local + 1e-6);
    return base + (next !== undefined ? slot.start + next : slot.end >= this.duration ? this.duration : slot.end);
  }

  /** Where each Actor starts the first run, with who holds a ball; for the still picture and the editor. */
  lineup(): { positions: Map<string, Point>; balls: string[] } {
    return startingLineup(this.stage, initialCast(this.stage), false);
  }
}

/** The actor playing part P now plays P's next part; parts without a valid next keep their actor. */
export function handOn(stage: Stage, cast: Cast): Map<string, string> {
  const next = new Map(cast);
  const partIds = new Set(stage.parts.map((part) => part.id));
  const valid = stage.parts.every((part) => part.nextPartId && partIds.has(part.nextPartId));
  if (!valid) return next;
  for (const part of stage.parts) {
    const actor = cast.get(part.id);
    if (actor) next.set(part.nextPartId!, actor);
  }
  return next;
}

function sameCast(a: Cast, b: Cast): boolean {
  if (a.size !== b.size) return false;
  for (const [part, actor] of a) {
    if (b.get(part) !== actor) return false;
  }
  return true;
}
