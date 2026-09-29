/**
 * A Drill and its animation, as the backend hands them out (ADR-0018). The script types mirror
 * `DrillScript.java`; "no value" in its references is an empty string, and older or hand-made
 * documents may carry null instead, so readers treat both alike.
 */

export type DrillStatus = 'PENDING' | 'NEEDS_INPUT' | 'READY' | 'FAILED';
export type SketchRelation = 'PROGRESSION' | 'CONTINUOUS' | 'MIXED';

export interface DrillTag {
  id: string;
  name: string;
}

export interface DrillSummary {
  id: string;
  name: string;
  status: DrillStatus;
  tags: DrillTag[];
  sketchCount: number;
  updatedAt: string;
}

export interface DrillDetail {
  id: string;
  name: string;
  status: DrillStatus;
  error: string | null;
  sketchRelation: SketchRelation;
  tags: DrillTag[];
  sketches: DrillSketch[];
  currentVersion: number | null;
  script: DrillScript | null;
  versions: DrillVersion[];
  messages: DrillMessage[];
  openQuestions: Question[];
}

export interface DrillSketch {
  position: number;
  note: string | null;
  reading: Reading | null;
}

export interface DrillVersion {
  version: number;
  source: 'AI' | 'EDIT' | 'REVERT';
  changeSummary: string | null;
  createdAt: string;
}

export interface DrillMessage {
  position: number;
  author: 'COACH' | 'INTERPRETER';
  content: string | null;
  questions: Question[] | null;
  answers: { questionId: string; question: string; answer: string }[] | null;
  scriptVersion: number | null;
  createdAt: string;
}

export interface Question {
  id: string;
  text: string;
  options: string[];
  /** 1-based; 0 for the whole Drill. */
  sketch: number;
  symbolIds: string[];
}

export interface Reading {
  sketch: number;
  boardSide: Area;
  orientation: string;
  symbols: SketchSymbol[];
}

export interface SketchSymbol {
  id: string;
  kind: string;
  color: string;
  points: Point[];
  label: string;
  confidence: number;
  meaning: string;
}

// --- the script -------------------------------------------------------------------

export interface Point {
  x: number;
  y: number;
}

export type Area = 'FULL' | 'HALF';
export type ActorKind = 'PLAYER' | 'GOALIE' | 'COACH';
export type Side = 'A' | 'B' | 'NEUTRAL';
export type StepType = 'RUN' | 'DRIBBLE' | 'PASS' | 'SHOT' | 'ROAM' | 'WAIT';
export type Edge = 'START' | 'END';
export type RepetitionMode = 'REPLAY' | 'SEAMLESS';
export type PropKind = 'CONE' | 'POLE' | 'SMALL_GOAL' | 'OTHER';

export interface DrillScript {
  stages: Stage[];
  assumptions: string[];
}

export interface Stage {
  id: string;
  name: string;
  sketches: number[];
  area: Area;
  actors: Actor[];
  parts: Part[];
  props: Prop[];
  steps: Step[];
  repetition: Repetition;
}

export interface Actor {
  id: string;
  kind: ActorKind;
  side: Side;
  label: string;
  start: Point;
  hasBall: boolean;
}

export interface Part {
  id: string;
  name: string;
  actorId: string;
  nextPartId: string | null;
}

export interface Prop {
  id: string;
  kind: PropKind;
  position: Point;
}

export interface Step {
  id: string;
  partId: string;
  type: StepType;
  path: Point[];
  targetPartId: string | null;
  after: string | null;
  afterEdge: Edge | null;
  delay: number;
  speed: number;
  duration: number;
  sketch: number;
  label: string;
}

export interface Repetition {
  mode: RepetitionMode;
  mirrored: boolean;
}

/** An empty-or-null reference counts as none. */
export function isSet(ref: string | null | undefined): ref is string {
  return !!ref && ref.trim() !== '';
}
