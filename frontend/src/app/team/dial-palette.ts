/**
 * A Line as shown on the timeline dials: id, name, and its persisted color (assigned once at
 * Line creation — see backend `Line.color` — so it survives that Line's own later deletion).
 */
export interface DialLine {
  id: string;
  name: string;
  color: string;
}
