import { Component, ElementRef, computed, input, output, viewChild } from '@angular/core';
import { Area, Point, Prop, SketchSymbol } from '../drill.model';
import { Frame } from '../drill-sampler';
import {
  CORNER_RADIUS,
  FACE_OFF,
  GOAL_AREA,
  GOAL_LINE_Y,
  GOAL_WIDTH,
  RINK_LENGTH,
  RINK_WIDTH,
  clampToRink,
  fromSvg,
  toSvg,
  viewBox,
} from '../rink';
import { RinkPath, arrowHead, offsetLine, svgPoints, zigzag } from './rink-paths';

/** Something the editor lets the coach drag: an Actor's start, a waypoint, a Prop. */
export interface RinkHandle {
  id: string;
  point: Point;
  kind: 'actor' | 'waypoint' | 'prop';
  label?: string;
  selected?: boolean;
}

/**
 * The small-court rink as SVG, in metres (ADR-0018), and whatever sits on it: a frame of the
 * animation, the Step paths, the symbols read off a sketch, or the editor's handles. Draws only;
 * the player and the editor own the state.
 */
@Component({
  selector: 'app-drill-rink',
  templateUrl: './drill-rink.html',
  styleUrl: './drill-rink.scss',
})
export class DrillRink {
  readonly area = input<Area>('FULL');
  readonly frame = input<Frame | null>(null);
  readonly props = input<Prop[]>([]);
  readonly paths = input<RinkPath[]>([]);
  readonly symbols = input<SketchSymbol[]>([]);
  readonly highlightedSymbols = input<string[]>([]);
  readonly handles = input<RinkHandle[]>([]);

  readonly handleMoved = output<{ id: string; point: Point }>();
  readonly handleSelected = output<string>();

  private readonly svg = viewChild<ElementRef<SVGSVGElement>>('svg');
  private dragging: string | null = null;

  protected readonly box = computed(() => {
    const b = viewBox(this.area());
    return `${b.x} ${b.y} ${b.width} ${b.height}`;
  });

  // The rink markings, in SVG space (y down).
  protected readonly boards = { x: -RINK_WIDTH / 2, y: -RINK_LENGTH / 2, width: RINK_WIDTH, height: RINK_LENGTH, r: CORNER_RADIUS };
  protected readonly goalEnds = [1, -1].map((end) => {
    const line = -end * GOAL_LINE_Y;
    return {
      end,
      area: { x: -GOAL_AREA.width / 2, y: end === 1 ? line - 0.5 : line - GOAL_AREA.depth + 0.5, width: GOAL_AREA.width, height: GOAL_AREA.depth },
      keeper: { x: -1.25, y: end === 1 ? line : line - 1, width: 2.5, height: 1 },
      goal: { x1: -GOAL_WIDTH / 2, x2: GOAL_WIDTH / 2, y: line },
      goalBack: { x: -GOAL_WIDTH / 2, y: end === 1 ? line - 0.65 : line, width: GOAL_WIDTH, height: 0.65 },
    };
  });
  protected readonly faceOffs = [
    [1, 1],
    [-1, 1],
    [1, -1],
    [-1, -1],
  ].map(([sx, sy]) => toSvg({ x: sx * FACE_OFF.x, y: sy * FACE_OFF.y }));

  protected readonly drawnPaths = computed(() =>
    this.paths().map((path) => {
      const strokes =
        path.kind === 'SHOT'
          ? [svgPoints(offsetLine(path.points, 0.1)), svgPoints(offsetLine(path.points, -0.1))]
          : [svgPoints(path.kind === 'DRIBBLE' ? zigzag(path.points) : path.points)];
      return {
        id: path.id,
        kind: path.kind,
        highlight: !!path.highlight,
        strokes,
        head: path.kind === 'ROAM' ? '' : arrowHead(path.points),
      };
    }),
  );

  protected readonly drawnSymbols = computed(() => {
    const highlighted = new Set(this.highlightedSymbols());
    return this.symbols().map((symbol) => ({
      symbol,
      highlight: highlighted.has(symbol.id),
      isLine: symbol.points.length > 1,
      points: svgPoints(symbol.points),
      at: toSvg(symbol.points[0] ?? { x: 0, y: 0 }),
      head: symbol.points.length > 1 && /LINE|ARROW/.test(symbol.kind) ? arrowHead(symbol.points) : '',
    }));
  });

  protected svgPoint(point: Point): Point {
    return toSvg(point);
  }

  protected startDrag(event: PointerEvent, id: string): void {
    event.preventDefault();
    this.dragging = id;
    (event.target as Element).setPointerCapture?.(event.pointerId);
    this.handleSelected.emit(id);
  }

  protected drag(event: PointerEvent): void {
    if (!this.dragging) return;
    const point = this.toRink(event);
    if (point) this.handleMoved.emit({ id: this.dragging, point });
  }

  protected endDrag(): void {
    this.dragging = null;
  }

  /** A pointer position in rink metres, rounded to 10 cm; null where the browser can't say (tests). */
  private toRink(event: PointerEvent): Point | null {
    const svg = this.svg()?.nativeElement;
    const matrix = svg?.getScreenCTM?.();
    if (!svg || !matrix) return null;
    const p = new DOMPoint(event.clientX, event.clientY).matrixTransform(matrix.inverse());
    const rink = clampToRink(fromSvg({ x: p.x, y: p.y }));
    return { x: Math.round(rink.x * 10) / 10, y: Math.round(rink.y * 10) / 10 };
  }
}
