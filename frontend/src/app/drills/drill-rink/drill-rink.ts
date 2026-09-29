import { Component, ElementRef, computed, input, output, signal, viewChild } from '@angular/core';
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

/** Where a figure stands once its Steps are done, drawn faintly: where the next path would start. */
export interface RinkGhost {
  id: string;
  point: Point;
  label: string;
  side: 'A' | 'B' | 'NEUTRAL';
}

/** How far a finger may wander, in screen pixels, and still be a tap and not the start of a stroke. */
export const TAP_SLOP = 10;
/** Stroke points closer than this, in metres, to the last one are dropped. */
const STROKE_STEP = 0.15;
/** Handles get an invisible touch target this big, in metres: about 44 px across on a phone. */
export const HANDLE_HIT = { actor: 1.1, other: 0.9 };

/**
 * The small-court rink as SVG, in metres (ADR-0018), and whatever sits on it: a frame of the
 * animation, the Step paths, the symbols read off a sketch, or the editor's handles. Draws only;
 * the player and the editor own the state.
 *
 * With `drawing` on, a touch that stays put is a tap (`rinkTapped`) and one that moves is a stroke
 * (`strokeDrawn`, every point in rink metres); handles are then out of the way. With it off, handles
 * are dragged and a tap on the empty rink is still reported.
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
  readonly ghosts = input<RinkGhost[]>([]);
  /** A path being tapped out point by point, not yet a Step. */
  readonly draft = input<Point[]>([]);
  readonly drawing = input(false);

  readonly handleMoved = output<{ id: string; point: Point }>();
  readonly handleSelected = output<string>();
  readonly rinkTapped = output<Point>();
  readonly strokeDrawn = output<Point[]>();

  private readonly svg = viewChild<ElementRef<SVGSVGElement>>('svg');
  private dragging: string | null = null;
  private press: { clientX: number; clientY: number; points: Point[]; moved: boolean } | null = null;

  protected readonly hit = HANDLE_HIT;
  /** The stroke under the finger right now. */
  protected readonly live = signal<Point[]>([]);
  protected readonly livePoints = computed(() => svgPoints(this.live()));
  protected readonly draftPoints = computed(() => svgPoints(this.draft()));

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
    event.stopPropagation();
    this.dragging = id;
    (event.target as Element).setPointerCapture?.(event.pointerId);
    this.handleSelected.emit(id);
  }

  /** A touch on the rink itself: the start of a tap, or of a stroke when drawing. */
  protected down(event: PointerEvent): void {
    if (this.dragging) return;
    const point = this.toRink(event);
    if (!point) return;
    this.press = { clientX: event.clientX, clientY: event.clientY, points: [point], moved: false };
    if (this.drawing()) {
      event.preventDefault();
      this.svg()?.nativeElement.setPointerCapture?.(event.pointerId);
    }
  }

  protected drag(event: PointerEvent): void {
    if (this.dragging) {
      const point = this.toRink(event);
      if (point) this.handleMoved.emit({ id: this.dragging, point });
      return;
    }
    const press = this.press;
    if (!press) return;
    if (!press.moved && Math.hypot(event.clientX - press.clientX, event.clientY - press.clientY) > TAP_SLOP) {
      press.moved = true;
    }
    if (!press.moved || !this.drawing()) return;
    const point = this.toRink(event);
    const last = press.points[press.points.length - 1];
    if (point && Math.hypot(point.x - last.x, point.y - last.y) >= STROKE_STEP) {
      press.points.push(point);
      this.live.set([...press.points]);
    }
  }

  protected up(): void {
    const press = this.press;
    this.press = null;
    this.live.set([]);
    if (this.dragging) {
      this.dragging = null;
      return;
    }
    if (!press) return;
    if (!press.moved) this.rinkTapped.emit(press.points[0]);
    else if (this.drawing() && press.points.length > 1) this.strokeDrawn.emit(press.points);
  }

  protected endDrag(): void {
    this.dragging = null;
    this.press = null;
    this.live.set([]);
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
