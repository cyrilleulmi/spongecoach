/**
 * The canvas-independent half of the Avatar painter: pixel and geometry math that works on plain
 * arrays, so it is unit-testable without a real canvas (jsdom has none).
 */

/** Side of the painted square in pixels — what gets saved; the backend caps it at 512 (ADR-0016). */
export const CANVAS_SIZE = 512;

export type Rgba = readonly [number, number, number, number];

/** The subset of `ImageData` the engine touches. */
export interface Pixels {
  readonly data: Uint8ClampedArray;
  readonly width: number;
  readonly height: number;
}

export interface Point {
  x: number;
  y: number;
}

export function hexToRgba(hex: string): Rgba {
  const value = hex.replace('#', '');
  return [
    parseInt(value.slice(0, 2), 16),
    parseInt(value.slice(2, 4), 16),
    parseInt(value.slice(4, 6), 16),
    255,
  ];
}

/**
 * Bucket fill from (x, y): recolors the connected area whose pixels are within `tolerance` of the
 * start pixel on every channel. The tolerance lets a fill swallow the soft anti-aliased edge of a
 * brush stroke instead of leaving a halo. Returns false when there was nothing to change.
 */
export function floodFill(pixels: Pixels, x: number, y: number, fill: Rgba, tolerance = 48): boolean {
  const { data, width, height } = pixels;
  const startX = Math.floor(x);
  const startY = Math.floor(y);
  if (startX < 0 || startY < 0 || startX >= width || startY >= height) {
    return false;
  }
  const start = (startY * width + startX) * 4;
  const target: Rgba = [data[start], data[start + 1], data[start + 2], data[start + 3]];
  if (sameColor(target, fill, 0)) {
    return false;
  }

  const visited = new Uint8Array(width * height);
  const matches = (pixel: number) =>
    !visited[pixel] &&
    Math.abs(data[pixel * 4] - target[0]) <= tolerance &&
    Math.abs(data[pixel * 4 + 1] - target[1]) <= tolerance &&
    Math.abs(data[pixel * 4 + 2] - target[2]) <= tolerance &&
    Math.abs(data[pixel * 4 + 3] - target[3]) <= tolerance;

  // Scanline fill: paint a whole horizontal run, then queue the rows above and below it.
  const stack = [startY * width + startX];
  while (stack.length > 0) {
    const seed = stack.pop()!;
    if (!matches(seed)) {
      continue;
    }
    const row = Math.floor(seed / width);
    let left = seed;
    while (left % width > 0 && matches(left - 1)) {
      left--;
    }
    let right = seed;
    while (right % width < width - 1 && matches(right + 1)) {
      right++;
    }
    for (let pixel = left; pixel <= right; pixel++) {
      visited[pixel] = 1;
      data.set(fill, pixel * 4);
      if (row > 0 && matches(pixel - width)) {
        stack.push(pixel - width);
      }
      if (row < height - 1 && matches(pixel + width)) {
        stack.push(pixel + width);
      }
    }
  }
  return true;
}

function sameColor(a: Rgba, b: Rgba, tolerance: number): boolean {
  return a.every((channel, i) => Math.abs(channel - b[i]) <= tolerance);
}

/**
 * Brush width for a stroke segment: fast strokes thin out and slow ones swell, like a real brush,
 * eased against the previous width so the line never jumps.
 */
export function brushWidth(base: number, previousWidth: number, from: Point, to: Point): number {
  const distance = Math.hypot(to.x - from.x, to.y - from.y);
  const target = base * clamp(1.3 - distance / (base * 3), 0.55, 1.3);
  return previousWidth + (target - previousWidth) * 0.35;
}

/** Evenly scattered dots within a circle, like an airbrush. `random` is injectable for tests. */
export function sprayDots(center: Point, radius: number, count: number, random = Math.random): Point[] {
  return Array.from({ length: count }, () => {
    const angle = random() * Math.PI * 2;
    // sqrt keeps the density even instead of clumping in the middle.
    const distance = Math.sqrt(random()) * radius;
    return { x: center.x + Math.cos(angle) * distance, y: center.y + Math.sin(angle) * distance };
  });
}

/** Undo/redo over whole-canvas snapshots, capped so a long session doesn't hoard memory. */
export class History<T> {
  private readonly undoStack: T[] = [];
  private readonly redoStack: T[] = [];

  constructor(private readonly limit = 20) {}

  /** Records the state before a change. A new change forgets anything that was undone. */
  record(before: T): void {
    this.undoStack.push(before);
    if (this.undoStack.length > this.limit) {
      this.undoStack.shift();
    }
    this.redoStack.length = 0;
  }

  /** Returns the state to restore, or null; `current` becomes what redo goes back to. */
  undo(current: T): T | null {
    const previous = this.undoStack.pop();
    if (previous === undefined) {
      return null;
    }
    this.redoStack.push(current);
    return previous;
  }

  redo(current: T): T | null {
    const next = this.redoStack.pop();
    if (next === undefined) {
      return null;
    }
    this.undoStack.push(current);
    return next;
  }

  get canUndo(): boolean {
    return this.undoStack.length > 0;
  }

  get canRedo(): boolean {
    return this.redoStack.length > 0;
  }

  clear(): void {
    this.undoStack.length = 0;
    this.redoStack.length = 0;
  }
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}
