import { Area, Point } from './drill.model';

/**
 * The Swiss small court, in metres, mirroring `Rink.java` — change both together. The origin is
 * the centre spot; x runs across (left to right), y along, with the goals at ±GOAL_LINE_Y. A
 * half-rink Stage shows only y ≥ 0.
 */
export const RINK_WIDTH = 14;
export const RINK_LENGTH = 24;
export const GOAL_LINE_Y = RINK_LENGTH / 2 - 2.35;
export const CORNER_RADIUS = 2.5;
export const GOAL_WIDTH = 1.6;
export const GOAL_AREA = { width: 4.5, depth: 3 };
export const FACE_OFF = { x: 5.5, y: GOAL_LINE_Y };

/** The metre rectangle an Area shows, with a small margin so figures at the boards stay visible. */
export function viewBox(area: Area): { x: number; y: number; width: number; height: number } {
  const margin = 0.8;
  const top = RINK_LENGTH / 2 + margin;
  const bottom = area === 'HALF' ? -margin : -top;
  return { x: -RINK_WIDTH / 2 - margin, y: -top, width: RINK_WIDTH + 2 * margin, height: top - bottom };
}

/** SVG y grows downwards; rink y grows towards the +y goal, which is drawn at the top. */
export function toSvg(point: Point): Point {
  return { x: point.x, y: -point.y };
}

export function fromSvg(point: Point): Point {
  return { x: point.x, y: -point.y };
}

/** Reflects across the rink's long axis: a mirrored repetition runs from the other side. */
export function mirror(point: Point): Point {
  return { x: -point.x, y: point.y };
}

/** Keeps a dragged point on the rink. */
export function clampToRink(point: Point): Point {
  return {
    x: Math.max(-RINK_WIDTH / 2, Math.min(RINK_WIDTH / 2, point.x)),
    y: Math.max(-RINK_LENGTH / 2, Math.min(RINK_LENGTH / 2, point.y)),
  };
}
