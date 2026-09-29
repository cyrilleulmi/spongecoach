import { Point } from '../drill.model';
import { toSvg } from '../rink';

/**
 * Draws Step paths the way coaches draw them on the board (a solid run, a wavy dribble, a dashed
 * pass, a double-line shot), so the animation reads like the sketch it came from.
 */

export type PathKind = 'RUN' | 'DRIBBLE' | 'PASS' | 'SHOT' | 'ROAM';

export interface RinkPath {
  id: string;
  kind: PathKind;
  /** Rink metres, start first. */
  points: Point[];
  highlight?: boolean;
}

/** An SVG polyline `points` attribute, in SVG space. */
export function svgPoints(points: Point[]): string {
  return points
    .map(toSvg)
    .map((p) => `${round(p.x)},${round(p.y)}`)
    .join(' ');
}

/** The polyline with a zigzag laid over it, like a dribble drawn on the board. */
export function zigzag(points: Point[], wavelength = 0.8, amplitude = 0.22): Point[] {
  const out: Point[] = [];
  let side = 1;
  for (let i = 1; i < points.length; i++) {
    const a = points[i - 1];
    const b = points[i];
    const length = Math.hypot(b.x - a.x, b.y - a.y);
    if (length === 0) continue;
    const nx = -(b.y - a.y) / length;
    const ny = (b.x - a.x) / length;
    const steps = Math.max(1, Math.round(length / (wavelength / 2)));
    if (i === 1) out.push(a);
    for (let s = 1; s < steps; s++) {
      const f = s / steps;
      out.push({ x: a.x + (b.x - a.x) * f + nx * amplitude * side, y: a.y + (b.y - a.y) * f + ny * amplitude * side });
      side = -side;
    }
    out.push(b);
  }
  return out.length > 0 ? out : points;
}

/** The polyline moved sideways by `offset` metres, for the two strokes of a shot. */
export function offsetLine(points: Point[], offset: number): Point[] {
  return points.map((p, i) => {
    const a = points[Math.max(0, i - 1)];
    const b = points[Math.min(points.length - 1, i + 1)];
    const length = Math.hypot(b.x - a.x, b.y - a.y) || 1;
    return { x: p.x - ((b.y - a.y) / length) * offset, y: p.y + ((b.x - a.x) / length) * offset };
  });
}

/** An arrowhead at the end of a polyline, as an SVG polygon `points` attribute. */
export function arrowHead(points: Point[], size = 0.45): string {
  if (points.length < 2) return '';
  const tip = points[points.length - 1];
  const from = points[points.length - 2];
  const length = Math.hypot(tip.x - from.x, tip.y - from.y) || 1;
  const ux = (tip.x - from.x) / length;
  const uy = (tip.y - from.y) / length;
  const left = { x: tip.x - ux * size - uy * size * 0.55, y: tip.y - uy * size + ux * size * 0.55 };
  const right = { x: tip.x - ux * size + uy * size * 0.55, y: tip.y - uy * size - ux * size * 0.55 };
  return svgPoints([tip, left, right]);
}

function round(value: number): number {
  return Math.round(value * 1000) / 1000;
}
