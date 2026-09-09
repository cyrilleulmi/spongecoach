/**
 * Colors assigned to Lines (in listing order) for the timeline dials. A dial is one quadrant
 * per Line; a quadrant is lit in the Line's color when that Line has a Focus set for the event.
 * These are fixed and independent of the Skill/Development-goal palette.
 */
export const LINE_DIAL_COLORS: readonly string[] = [
  '#4c8c3d',
  '#8b5e34',
  '#6a4c93',
  '#c9702c',
  '#3b6ea5',
  '#b1467a',
  '#8a7a2e',
  '#2c7a68',
];

export interface DialLine {
  id: string;
  name: string;
  color: string;
}
