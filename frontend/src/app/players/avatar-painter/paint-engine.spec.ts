import { History, Pixels, brushWidth, floodFill, hexToRgba, sprayDots } from './paint-engine';

function pixels(width: number, height: number, rows: string[]): Pixels {
  // '#' is a black pixel, '.' a white one.
  const data = new Uint8ClampedArray(width * height * 4);
  rows.forEach((row, y) =>
    [...row].forEach((cell, x) => data.set(cell === '#' ? [0, 0, 0, 255] : [255, 255, 255, 255], (y * width + x) * 4)),
  );
  return { data, width, height };
}

function colorAt(p: Pixels, x: number, y: number): number[] {
  const i = (y * p.width + x) * 4;
  return Array.from(p.data.slice(i, i + 4));
}

describe('paint engine', () => {
  it('parses hex colors to opaque RGBA', () => {
    expect(hexToRgba('#2c7a68')).toEqual([44, 122, 104, 255]);
  });

  describe('floodFill', () => {
    it('fills the connected area and stops at borders', () => {
      const p = pixels(5, 3, ['..#..', '..#..', '..#..']);

      expect(floodFill(p, 0, 0, [255, 0, 0, 255])).toBe(true);

      expect(colorAt(p, 1, 2)).toEqual([255, 0, 0, 255]);
      expect(colorAt(p, 2, 1)).toEqual([0, 0, 0, 255]);
      expect(colorAt(p, 4, 0)).toEqual([255, 255, 255, 255]);
    });

    it('reaches around corners', () => {
      const p = pixels(4, 3, ['.#..', '.#.#', '...#']);

      floodFill(p, 0, 0, [255, 0, 0, 255]);

      expect(colorAt(p, 2, 0)).toEqual([255, 0, 0, 255]);
      expect(colorAt(p, 3, 0)).toEqual([255, 0, 0, 255]);
    });

    it('swallows near-matching pixels within the tolerance', () => {
      const p = pixels(3, 1, ['...']);
      p.data.set([240, 240, 240, 255], 4);

      floodFill(p, 0, 0, [0, 0, 255, 255], 20);

      expect(colorAt(p, 1, 0)).toEqual([0, 0, 255, 255]);
    });

    it('does nothing when the area already has the fill color or the point is outside', () => {
      const p = pixels(2, 1, ['..']);

      expect(floodFill(p, 0, 0, [255, 255, 255, 255])).toBe(false);
      expect(floodFill(p, 5, 0, [255, 0, 0, 255])).toBe(false);
    });
  });

  it('thins the brush on fast strokes and swells it on slow ones', () => {
    const slow = brushWidth(10, 10, { x: 0, y: 0 }, { x: 1, y: 0 });
    const fast = brushWidth(10, 10, { x: 0, y: 0 }, { x: 80, y: 0 });

    expect(slow).toBeGreaterThan(10);
    expect(fast).toBeLessThan(10);
  });

  it('scatters spray dots within the radius', () => {
    const dots = sprayDots({ x: 50, y: 50 }, 10, 40);

    expect(dots).toHaveLength(40);
    for (const dot of dots) {
      expect(Math.hypot(dot.x - 50, dot.y - 50)).toBeLessThanOrEqual(10);
    }
  });

  describe('History', () => {
    it('undoes and redoes in order', () => {
      const history = new History<string>();
      history.record('a');
      history.record('b');

      expect(history.undo('c')).toBe('b');
      expect(history.undo('b')).toBe('a');
      expect(history.canUndo).toBe(false);
      expect(history.redo('a')).toBe('b');
      expect(history.redo('b')).toBe('c');
      expect(history.canRedo).toBe(false);
    });

    it('forgets the redo trail on a new change', () => {
      const history = new History<string>();
      history.record('a');
      history.undo('b');

      history.record('a');

      expect(history.canRedo).toBe(false);
    });

    it('keeps at most the limit of snapshots', () => {
      const history = new History<number>(2);
      [1, 2, 3].forEach((n) => history.record(n));

      expect(history.undo(4)).toBe(3);
      expect(history.undo(3)).toBe(2);
      expect(history.undo(2)).toBeNull();
    });
  });
});
