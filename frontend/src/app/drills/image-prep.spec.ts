import { fitWithin, normaliseTurns, turnedSize } from './image-prep';

describe('image prep', () => {
  it('scales a phone photo down so its long edge fits', () => {
    expect(fitWithin(3000, 4000)).toEqual({ width: 1176, height: 1568 });
  });

  it('never scales a small photo up', () => {
    expect(fitWithin(800, 600)).toEqual({ width: 800, height: 600 });
  });

  it('swaps width and height on an odd number of quarter turns', () => {
    expect(turnedSize(4000, 3000, 1)).toEqual({ width: 3000, height: 4000 });
    expect(turnedSize(4000, 3000, 2)).toEqual({ width: 4000, height: 3000 });
  });

  it('keeps turns within a full circle, either way round', () => {
    expect(normaliseTurns(5)).toBe(1);
    expect(normaliseTurns(-1)).toBe(3);
  });
});
