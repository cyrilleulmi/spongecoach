import { Component, computed, effect, input, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  MOVING_TYPES,
  addActor,
  addProp,
  addStage,
  addStepFromStroke,
  addWaitStep,
  aimPass,
  dependsOn,
  endPositions,
  distance,
  findActorAt,
  findOrigin,
  insertWaypoint,
  isDrawn,
  moveStage,
  removeActor,
  removeProp,
  removeStage,
  removeStep,
  removeWaypoint,
  resizeStep,
  retimeStep,
  setStageSketches,
  updateActor,
} from '../drill-authoring';
import { DrillRink, RinkGhost, RinkHandle } from '../drill-rink/drill-rink';
import { RinkPath } from '../drill-rink/rink-paths';
import { Actor, ActorKind, Area, DrillScript, Edge, Point, PropKind, Side, Stage, Step, StepType, isSet } from '../drill.model';
import { DEFAULT_SPEED, schedule } from '../drill-schedule';
import { RinkPlayer } from '../rink-player/rink-player';

export interface ScriptEdit {
  script: DrillScript;
  changeSummary: string;
}

/** What the next touch on the rink does. */
export type Tool =
  | { kind: 'select' }
  | { kind: 'actor'; actorKind: ActorKind; side: Side }
  | { kind: 'prop'; propKind: PropKind }
  | { kind: 'step'; type: StepType };

interface ToolButton {
  id: string;
  label: string;
  title: string;
  tool: Tool;
}

const SELECT: ToolButton = { id: 'select', label: 'Auswahl', title: 'Figuren, Objekte und Wege auswählen und verschieben', tool: { kind: 'select' } };

const TYPE_LABELS: Record<StepType, string> = {
  RUN: 'Laufweg',
  DRIBBLE: 'Dribbling',
  PASS: 'Pass',
  SHOT: 'Schuss',
  ROAM: 'Bewegung',
  WAIT: 'Warten',
};

const TOOL_GROUPS: { name: string; tools: ToolButton[] }[] = [
  {
    name: 'Figuren',
    tools: [
      { id: 'actor-a', label: 'A', title: 'Spieler Team A setzen', tool: { kind: 'actor', actorKind: 'PLAYER', side: 'A' } },
      { id: 'actor-b', label: 'B', title: 'Spieler Team B setzen', tool: { kind: 'actor', actorKind: 'PLAYER', side: 'B' } },
      { id: 'actor-g', label: 'G', title: 'Goalie setzen', tool: { kind: 'actor', actorKind: 'GOALIE', side: 'NEUTRAL' } },
      { id: 'actor-t', label: 'T', title: 'Trainer setzen', tool: { kind: 'actor', actorKind: 'COACH', side: 'NEUTRAL' } },
    ],
  },
  {
    name: 'Objekte',
    tools: [
      { id: 'prop-cone', label: 'Hütchen', title: 'Hütchen setzen', tool: { kind: 'prop', propKind: 'CONE' } },
      { id: 'prop-pole', label: 'Stange', title: 'Stange setzen', tool: { kind: 'prop', propKind: 'POLE' } },
      { id: 'prop-goal', label: 'Minitor', title: 'Minitor setzen', tool: { kind: 'prop', propKind: 'SMALL_GOAL' } },
    ],
  },
  {
    name: 'Wege',
    tools: (['RUN', 'DRIBBLE', 'PASS', 'SHOT', 'ROAM', 'WAIT'] as StepType[]).map((type) => ({
      id: 'step-' + type.toLowerCase(),
      label: TYPE_LABELS[type],
      title: TYPE_LABELS[type] + ' zeichnen',
      tool: { kind: 'step', type } as Tool,
    })),
  },
];

const TOOLS = new Map<string, ToolButton>([SELECT, ...TOOL_GROUPS.flatMap((g) => g.tools)].map((t) => [t.id, t]));

const HELP: Record<string, string> = {
  select: 'Tippe eine Figur, ein Objekt oder einen Weg an. Ziehe Punkte, um sie zu verschieben. Tippe auf den gewählten Weg, um einen Wegpunkt einzufügen.',
  actor: 'Tippe aufs Spielfeld, um eine Figur zu setzen.',
  prop: 'Tippe aufs Spielfeld, um ein Objekt zu setzen.',
  path: 'Ziehe von einer Figur aus, um den Weg zu zeichnen. Oder tippe Punkt für Punkt und drücke „Fertig“.',
  fly: 'Ziehe von einer Figur oder von der Mitte eines Laufwegs aus zum Ziel. Tippen geht auch: erst der Start, dann das Ziel.',
  wait: 'Tippe eine Figur an, die kurz warten soll.',
};

/** Pixels per second of the timeline. */
export const TIMELINE_PPS = 48;
/** A bar is at least this wide, so a short Step can still be touched. */
const MIN_BAR = 36;

/**
 * Draws and edits a Drill's animation by hand (ADR-0018, ADR-0020), built for a phone: a bottom tool
 * bar, tapping and drawing on the rink, sheets instead of side panels, and a timeline whose bars are
 * dragged to re-time Steps. All the maths is in `drill-authoring.ts`; this keeps the state (the
 * script being edited, undo and redo, the selection, the tool). Works on a copy until saved. Used
 * for a Drill already there, and, with `creating`, to draw a new one.
 */
@Component({
  selector: 'app-drill-editor',
  imports: [DrillRink, FormsModule, RinkPlayer],
  templateUrl: './drill-editor.html',
  styleUrl: './drill-editor.scss',
})
export class DrillEditor {
  readonly script = input.required<DrillScript>();
  readonly stageIndex = input(0);
  readonly busy = input(false);
  readonly error = input<string | null>(null);
  /** Drawing a new Drill: no change summary, and the first tool is a figure. */
  readonly creating = input(false);
  /** The Drill's photos, for choosing which a Stage or Step belongs to. */
  readonly sketchPositions = input<number[]>([]);

  readonly saved = output<ScriptEdit>();
  readonly cancelled = output<void>();
  /** Every change, so the screen can keep a draft. */
  readonly changed = output<DrillScript>();

  protected readonly toolGroups = TOOL_GROUPS;
  protected readonly selectTool = SELECT;
  protected readonly stepTypes: StepType[] = ['RUN', 'DRIBBLE', 'PASS', 'SHOT', 'ROAM', 'WAIT'];
  protected readonly typeLabels = TYPE_LABELS;
  protected readonly sides: Side[] = ['A', 'B', 'NEUTRAL'];
  protected readonly kinds: ActorKind[] = ['PLAYER', 'GOALIE', 'COACH'];
  protected readonly kindLabels: Record<ActorKind, string> = { PLAYER: 'Spieler', GOALIE: 'Goalie', COACH: 'Trainer' };
  protected readonly propKinds: PropKind[] = ['CONE', 'POLE', 'SMALL_GOAL', 'OTHER'];
  protected readonly propLabels: Record<PropKind, string> = { CONE: 'Hütchen', POLE: 'Stange', SMALL_GOAL: 'Minitor', OTHER: 'Anderes' };
  protected readonly moving = MOVING_TYPES;
  protected readonly pps = TIMELINE_PPS;

  protected readonly working = signal<DrillScript>({ stages: [], assumptions: [] });
  private readonly past = signal<DrillScript[]>([]);
  private readonly future = signal<DrillScript[]>([]);
  private original = '';
  protected readonly index = signal(0);
  protected readonly selectedStepId = signal<string | null>(null);
  protected readonly selectedActorId = signal<string | null>(null);
  protected readonly selectedPropId = signal<string | null>(null);
  protected readonly selectedWaypoint = signal<number | null>(null);
  /**
   * Whether the selected item's form is open. Drawing selects what it makes (so its path lights up)
   * without opening the form: on a phone the form is a sheet over the lower half of the rink, and
   * it must not cover what the next stroke is meant to start from.
   */
  protected readonly sheetOpen = signal(false);
  protected readonly preview = signal(false);
  protected readonly changeSummary = signal('Von Hand angepasst');
  protected readonly toolId = signal('select');
  protected readonly draft = signal<Point[]>([]);
  protected readonly hint = signal<string | null>(null);
  protected readonly barDrag = signal<{ id: string; mode: 'move' | 'resize'; dx: number } | null>(null);

  /** Passes the editor aimed at a receiver; it keeps them aimed when either one's Steps change. */
  private readonly aimed = new Set<string>();
  private dragRemembered = false;
  private barStart: { id: string; mode: 'move' | 'resize'; x: number; start: number; end: number; moved: boolean } | null = null;
  private suppressClick = false;

  protected readonly tool = computed(() => (TOOLS.get(this.toolId()) ?? SELECT).tool);
  protected readonly help = computed(() => {
    const tool = this.tool();
    if (tool.kind === 'step') {
      return HELP[tool.type === 'WAIT' ? 'wait' : tool.type === 'PASS' || tool.type === 'SHOT' ? 'fly' : 'path'];
    }
    return HELP[tool.kind];
  });

  protected readonly stage = computed<Stage | null>(() => this.working().stages[this.index()] ?? null);
  protected readonly run = computed(() => {
    const stage = this.stage();
    return stage ? schedule(stage) : null;
  });
  protected readonly selectedStep = computed(() => this.stage()?.steps.find((s) => s.id === this.selectedStepId()) ?? null);
  protected readonly selectedActor = computed(() => this.stage()?.actors.find((a) => a.id === this.selectedActorId()) ?? null);
  protected readonly selectedProp = computed(() => this.stage()?.props.find((p) => p.id === this.selectedPropId()) ?? null);
  protected readonly selectedPart = computed(() => {
    const actor = this.selectedActor();
    return actor ? this.stage()?.parts.find((p) => p.actorId === actor.id) ?? null : null;
  });
  protected readonly hasSelection = computed(() => !!(this.selectedStep() || this.selectedActor() || this.selectedProp()));
  protected readonly showSheet = computed(() => this.sheetOpen() && this.hasSelection() && !this.preview());
  protected readonly dirty = computed(() => JSON.stringify(this.working()) !== this.original);
  protected readonly canUndo = computed(() => this.past().length > 0);
  protected readonly canRedo = computed(() => this.future().length > 0);
  protected readonly canSave = computed(() => (this.creating() ? isDrawn(this.working()) : this.dirty()));

  protected readonly paths = computed<RinkPath[]>(() => {
    const run = this.run();
    if (!run) return [];
    const selected = this.selectedStepId();
    const paths: RinkPath[] = [];
    for (const [, movements] of run.movements) {
      for (const move of movements) {
        if (move.kind === 'WAIT') continue;
        paths.push({ id: move.stepId, kind: move.kind === 'ROAM' ? 'ROAM' : move.dribble ? 'DRIBBLE' : 'RUN', points: move.points, highlight: move.stepId === selected });
      }
    }
    for (const flight of run.flights) {
      paths.push({ id: flight.stepId, kind: flight.shot ? 'SHOT' : 'PASS', points: flight.points, highlight: flight.stepId === selected });
    }
    return paths;
  });

  protected readonly handles = computed<RinkHandle[]>(() => {
    const stage = this.stage();
    if (!stage) return [];
    const handles: RinkHandle[] = stage.actors.map((actor) => ({
      id: `actor:${actor.id}`,
      point: actor.start,
      kind: 'actor',
      label: actor.label,
      selected: actor.id === this.selectedActorId(),
    }));
    for (const prop of stage.props) {
      handles.push({ id: `prop:${prop.id}`, point: prop.position, kind: 'prop', selected: prop.id === this.selectedPropId() });
    }
    const step = this.selectedStep();
    step?.path.forEach((point, i) => handles.push({ id: `waypoint:${step.id}:${i}`, point, kind: 'waypoint', selected: i === this.selectedWaypoint() }));
    return handles;
  });

  /** Where each figure stands once its Steps are done, for those that move at all. */
  protected readonly ghosts = computed<RinkGhost[]>(() => {
    const stage = this.stage();
    if (!stage || stage.steps.length === 0) return [];
    const ends = endPositions(stage);
    return stage.actors
      .filter((actor) => distance(ends.get(actor.id) ?? actor.start, actor.start) > 0.05)
      .map((actor) => ({ id: actor.id, point: ends.get(actor.id)!, label: actor.label, side: actor.side }));
  });

  /** One timeline row per Part, with its Steps as bars on a scale of pixels per second. */
  protected readonly timeline = computed(() => {
    const stage = this.stage();
    const run = this.run();
    if (!stage || !run) return { duration: 1, width: 240, rows: [] };
    const duration = Math.max(run.duration, 0.1);
    const rows = stage.parts.map((part) => {
      const actor = stage.actors.find((a) => a.id === part.actorId);
      return {
        part,
        label: `${actor?.label ?? '?'} · ${part.name}`,
        bars: run.steps
          .filter((s) => s.step.partId === part.id)
          .map((s) => ({ id: s.step.id, type: s.step.type, start: s.start, end: s.end, left: s.start * TIMELINE_PPS, width: Math.max(MIN_BAR, (s.end - s.start) * TIMELINE_PPS) })),
      };
    });
    return { duration, width: Math.max(240, duration * TIMELINE_PPS + 80), rows };
  });

  constructor() {
    effect(() => {
      const script = this.script();
      const index = this.stageIndex();
      untracked(() => {
        this.working.set(clone(script));
        this.original = JSON.stringify(script);
        this.past.set([]);
        this.future.set([]);
        this.index.set(Math.min(index, Math.max(0, script.stages.length - 1)));
        this.selectedStepId.set(null);
        this.selectedActorId.set(null);
        this.selectedPropId.set(null);
        this.selectedWaypoint.set(null);
        this.sheetOpen.set(false);
        this.aimed.clear();
        this.draft.set([]);
        this.toolId.set(this.creating() && script.stages.every((s) => s.actors.length === 0) ? 'actor-a' : 'select');
        this.hint.set(null);
      });
    });
  }

  // --- tools --------------------------------------------------------------------

  protected chooseTool(id: string): void {
    this.toolId.set(id);
    this.sheetOpen.set(false);
    this.draft.set([]);
    this.hint.set(null);
    this.preview.set(false);
  }

  /** A touch on the rink that stayed put. */
  protected tapped(point: Point): void {
    const tool = this.tool();
    const stage = this.stage();
    if (!stage) return;
    switch (tool.kind) {
      case 'select': {
        const step = this.selectedStep();
        if (step && this.tryEdit((s, st) => insertWaypoint(st, step.id, point))) return;
        this.clearSelection();
        return;
      }
      case 'actor':
        this.edit((script, st) => {
          const actor = addActor(script, st, tool.actorKind, tool.side, point);
          this.selectedActorId.set(actor.id);
        });
        this.hint.set(null);
        return;
      case 'prop':
        this.edit((script, st) => this.selectedPropId.set(addProp(st, tool.propKind, point)));
        return;
      case 'step':
        this.tappedForStep(tool.type, point, stage);
    }
  }

  /** A freehand stroke; only a path tool does anything with one. */
  protected stroked(points: Point[]): void {
    const tool = this.tool();
    if (tool.kind !== 'step' || tool.type === 'WAIT') return;
    const flies = tool.type === 'PASS' || tool.type === 'SHOT';
    this.commitStroke(tool.type, flies ? [points[0], points[points.length - 1]] : points, !flies);
  }

  /** Ends a path tapped out point by point. */
  protected finishDraft(): void {
    const tool = this.tool();
    if (tool.kind === 'step' && tool.type !== 'WAIT' && this.draft().length >= 2) {
      this.commitStroke(tool.type, this.draft(), false);
    }
  }

  protected cancelDraft(): void {
    this.draft.set([]);
  }

  private tappedForStep(type: StepType, point: Point, stage: Stage): void {
    if (type === 'WAIT') {
      const actorId = findActorAt(stage, point);
      if (!actorId) {
        this.hint.set('Tippe eine Figur an.');
        return;
      }
      let added: string | null = null;
      this.edit((script, st) => (added = addWaitStep(st, actorId)));
      this.selectedStepId.set(added);
      this.hint.set(null);
      return;
    }
    const flies = type === 'PASS' || type === 'SHOT';
    const draft = this.draft();
    if (draft.length === 0) {
      if (!findOrigin(stage, point, flies)) {
        this.hint.set(flies ? 'Beginne bei einer Figur oder auf einem Laufweg.' : 'Beginne bei einer Figur.');
        return;
      }
      this.hint.set(null);
      this.draft.set([point]);
      return;
    }
    const next = [...draft, point];
    if (flies) this.commitStroke(type, next, false);
    else this.draft.set(next);
  }

  private commitStroke(type: Exclude<StepType, 'WAIT'>, points: Point[], simplifyIt: boolean): void {
    const next = clone(this.working());
    const stage = next.stages[this.index()];
    if (!stage) return;
    const result = addStepFromStroke(stage, type, points, { simplify: simplifyIt });
    this.draft.set([]);
    if (!result.stepId) {
      this.hint.set(result.hint);
      return;
    }
    if (result.lead) this.aimed.add(result.stepId);
    this.remember();
    this.apply(next, stage);
    this.selectedStepId.set(result.stepId);
    this.selectedWaypoint.set(null);
    this.hint.set(result.hint);
  }

  // --- selection --------------------------------------------------------------

  protected selectHandle(id: string): void {
    this.dragRemembered = false;
    this.sheetOpen.set(true);
    const [kind, ref, index] = id.split(':');
    if (kind === 'actor') {
      this.selectedActorId.set(ref);
      this.selectedPropId.set(null);
      this.selectedWaypoint.set(null);
    } else if (kind === 'prop') {
      this.selectedPropId.set(ref);
      this.selectedActorId.set(null);
      this.selectedWaypoint.set(null);
    } else if (kind === 'waypoint') {
      this.selectedWaypoint.set(Number(index));
    }
  }

  protected selectStep(stepId: string): void {
    if (this.suppressClick) {
      this.suppressClick = false;
      return;
    }
    this.selectedWaypoint.set(null);
    if (stepId !== this.selectedStepId()) {
      this.selectedStepId.set(stepId);
      this.sheetOpen.set(true);
    } else if (!this.sheetOpen()) {
      // Picked while drawing: the first tap on its bar opens the form, the next puts it away.
      this.sheetOpen.set(true);
    } else {
      this.selectedStepId.set(null);
      this.sheetOpen.set(false);
    }
  }

  protected clearSelection(): void {
    this.sheetOpen.set(false);
    this.selectedStepId.set(null);
    this.selectedActorId.set(null);
    this.selectedPropId.set(null);
    this.selectedWaypoint.set(null);
  }

  // --- dragging (the snapshot for undo is taken when the drag starts) ------------

  protected moveHandle(event: { id: string; point: Point }): void {
    if (!this.dragRemembered) {
      this.remember();
      this.dragRemembered = true;
    }
    const [kind, ref, index] = event.id.split(':');
    this.edit((script, stage) => {
      if (kind === 'actor') {
        const actor = stage.actors.find((a) => a.id === ref);
        if (actor) actor.start = event.point;
      } else if (kind === 'prop') {
        const prop = stage.props.find((p) => p.id === ref);
        if (prop) prop.position = event.point;
      } else if (kind === 'waypoint') {
        const step = stage.steps.find((s) => s.id === ref);
        if (step) step.path[Number(index)] = event.point;
      }
    }, false);
  }

  // --- the timeline: tap a bar to pick it, drag the picked one to re-time, its grip to resize ----

  protected barLeft(bar: { id: string; left: number }): number {
    const drag = this.barDrag();
    return drag && drag.id === bar.id && drag.mode === 'move' ? Math.max(0, bar.left + drag.dx) : bar.left;
  }

  protected barWidth(bar: { id: string; width: number }): number {
    const drag = this.barDrag();
    return drag && drag.id === bar.id && drag.mode === 'resize' ? Math.max(MIN_BAR, bar.width + drag.dx) : bar.width;
  }

  protected barDown(event: PointerEvent, bar: { id: string; start: number; end: number }, mode: 'move' | 'resize'): void {
    if (bar.id !== this.selectedStepId()) return;
    event.stopPropagation();
    (event.currentTarget as Element).setPointerCapture?.(event.pointerId);
    this.barStart = { id: bar.id, mode, x: event.clientX, start: bar.start, end: bar.end, moved: false };
  }

  protected barMove(event: PointerEvent): void {
    const drag = this.barStart;
    if (!drag) return;
    const dx = event.clientX - drag.x;
    if (Math.abs(dx) > 3) drag.moved = true;
    if (drag.moved) this.barDrag.set({ id: drag.id, mode: drag.mode, dx });
  }

  protected barUp(event: PointerEvent): void {
    const drag = this.barStart;
    this.barStart = null;
    this.barDrag.set(null);
    if (!drag || !drag.moved) return;
    this.suppressClick = true;
    const seconds = (event.clientX - drag.x) / TIMELINE_PPS;
    if (drag.mode === 'move') this.edit((script, stage) => retimeStep(stage, drag.id, drag.start + seconds));
    else this.edit((script, stage) => resizeStep(stage, drag.id, drag.end - drag.start + seconds));
  }

  protected barCancel(): void {
    this.barStart = null;
    this.barDrag.set(null);
  }

  // --- step fields ------------------------------------------------------------

  protected setStepField<K extends keyof Step>(field: K, value: Step[K]): void {
    const id = this.selectedStepId();
    this.edit((script, stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (!step) return;
      step[field] = value;
      if (field === 'type') this.fitStepToType(step);
    });
  }

  protected setNumber(field: 'delay' | 'speed' | 'duration', value: number | string): void {
    const number = Number(value);
    if (Number.isFinite(number) && number >= 0) this.setStepField(field, number);
  }

  protected setAfter(value: string): void {
    const id = this.selectedStepId();
    this.edit((script, stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (!step) return;
      step.after = value;
      if (!isSet(value)) {
        step.afterEdge = 'END';
        step.afterFraction = 0;
      }
    });
  }

  protected setEdge(value: Edge): void {
    const id = this.selectedStepId();
    this.edit((script, stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (!step) return;
      step.afterEdge = value;
      if (value === 'DURING') {
        if (!(step.afterFraction > 0 && step.afterFraction < 1)) step.afterFraction = 0.5;
      } else {
        step.afterFraction = 0;
      }
    });
  }

  /** How far through the anchor Step a `DURING` Step starts, as a percentage. */
  protected setFractionPercent(value: number | string): void {
    const percent = Number(value);
    if (Number.isFinite(percent) && percent >= 1 && percent <= 99) this.setStepField('afterFraction', percent / 100);
  }

  protected removeSelectedWaypoint(): void {
    const id = this.selectedStepId();
    const chosen = this.selectedWaypoint();
    this.edit((script, stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (step) removeWaypoint(step, chosen ?? step.path.length - 1);
    });
    this.selectedWaypoint.set(null);
  }

  protected addWaypoint(): void {
    const id = this.selectedStepId();
    this.edit((script, stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (!step) return;
      const last = step.path[step.path.length - 1] ?? { x: 0, y: 0 };
      step.path.push({ x: Math.min(7, last.x + 1), y: last.y });
    });
  }

  protected deleteStep(): void {
    const id = this.selectedStepId();
    if (!id) return;
    this.edit((script, stage) => removeStep(stage, id));
    this.selectedStepId.set(null);
  }

  /** Steps this one may wait for: not itself, and none that already waits for it. */
  protected anchors(stage: Stage, step: Step): Step[] {
    return stage.steps.filter((other) => other.id !== step.id && !dependsOn(stage, other.id, step.id));
  }

  /** "A1 · Rolle 1": a Part named by the figure that plays it. */
  protected partLabel(stage: Stage, part: { actorId: string; name: string }): string {
    return `${stage.actors.find((a) => a.id === part.actorId)?.label ?? '?'} · ${part.name}`;
  }

  protected percent(fraction: number): number {
    return Math.round(fraction * 100);
  }

  /** The photos a Step can be marked as drawn on: none (by hand), the Drill's, and its own if that one was removed. */
  protected photoChoices(step: Step): number[] {
    const positions = new Set(this.sketchPositions());
    if (step.sketch > 0) positions.add(step.sketch);
    return [...positions].sort((a, b) => a - b);
  }

  // --- actor, prop, Stage and repetition fields -----------------------------------

  protected setActorField<K extends 'kind' | 'side' | 'label' | 'hasBall'>(field: K, value: Actor[K]): void {
    const id = this.selectedActorId();
    if (!id) return;
    this.edit((script, stage) => {
      if (field === 'hasBall') {
        const actor = stage.actors.find((a) => a.id === id);
        if (actor) actor.hasBall = value as boolean;
      } else {
        // Kind, side and label are one figure across every Stage.
        updateActor(script, id, { [field]: value } as Partial<Pick<Actor, 'kind' | 'side' | 'label'>>);
      }
    });
  }

  protected setPartName(name: string): void {
    const part = this.selectedPart();
    this.edit((script, stage) => {
      const found = stage.parts.find((p) => p.id === part?.id);
      if (found) found.name = name;
    });
  }

  protected deleteActor(): void {
    const id = this.selectedActorId();
    if (!id) return;
    this.edit((script, stage) => removeActor(stage, id));
    this.selectedActorId.set(null);
  }

  protected setPropKind(kind: PropKind): void {
    const id = this.selectedPropId();
    this.edit((script, stage) => {
      const prop = stage.props.find((p) => p.id === id);
      if (prop) prop.kind = kind;
    });
  }

  protected deleteProp(): void {
    const id = this.selectedPropId();
    if (!id) return;
    this.edit((script, stage) => removeProp(stage, id));
    this.selectedPropId.set(null);
  }

  protected setRepetition(mode: 'REPLAY' | 'SEAMLESS', mirrored: boolean): void {
    this.edit((script, stage) => (stage.repetition = { mode, mirrored }));
  }

  protected setNextPart(partId: string, nextPartId: string): void {
    this.edit((script, stage) => {
      const part = stage.parts.find((p) => p.id === partId);
      if (part) part.nextPartId = nextPartId;
    });
  }

  protected renameStage(name: string): void {
    this.edit((script, stage) => (stage.name = name));
  }

  protected setArea(area: Area): void {
    this.edit((script, stage) => (stage.area = area));
  }

  protected toggleSketch(position: number, on: boolean): void {
    this.edit((script, stage) => setStageSketches(stage, position, on));
  }

  protected removeAssumption(i: number): void {
    this.edit((script) => script.assumptions.splice(i, 1));
  }

  protected showStage(i: number): void {
    this.index.set(i);
    this.clearSelection();
    this.draft.set([]);
  }

  protected newStage(): void {
    let at = this.index();
    this.edit((script) => (at = addStage(script, this.index())));
    this.showStage(at);
  }

  protected deleteStage(): void {
    let at = this.index();
    this.edit((script) => (at = removeStage(script, this.index())));
    this.showStage(at);
  }

  protected moveStageBy(by: -1 | 1): void {
    let at = this.index();
    this.edit((script) => (at = moveStage(script, this.index(), by)));
    this.index.set(at);
  }

  // --- history and saving -------------------------------------------------------

  protected undo(): void {
    const past = this.past();
    if (past.length === 0) return;
    this.future.update((list) => [...list, clone(this.working())]);
    this.working.set(past[past.length - 1]);
    this.past.set(past.slice(0, -1));
    this.settle();
  }

  protected redo(): void {
    const future = this.future();
    if (future.length === 0) return;
    this.past.update((list) => [...list, clone(this.working())]);
    this.working.set(future[future.length - 1]);
    this.future.set(future.slice(0, -1));
    this.settle();
  }

  protected save(): void {
    this.saved.emit({
      script: clone(this.working()),
      changeSummary: this.creating() ? 'Gezeichnet' : this.changeSummary().trim() || 'Von Hand angepasst',
    });
  }

  protected isSet = isSet;

  private settle(): void {
    if (this.index() >= this.working().stages.length) this.index.set(this.working().stages.length - 1);
    this.changed.emit(this.working());
  }

  private remember(): void {
    this.past.update((list) => [...list.slice(-49), clone(this.working())]);
    this.future.set([]);
  }

  /** Applies a change to the working script and to the Stage being edited; `remember` records an undo step first. */
  private edit(apply: (script: DrillScript, stage: Stage) => unknown, remember = true): void {
    const next = clone(this.working());
    const stage = next.stages[this.index()];
    if (!stage) return;
    if (remember) this.remember();
    apply(next, stage);
    this.apply(next, stage);
  }

  /** Like `edit`, for a change that may find nothing to do: only a change that happened is remembered. */
  private tryEdit(apply: (script: DrillScript, stage: Stage) => boolean): boolean {
    const next = clone(this.working());
    const stage = next.stages[this.index()];
    if (!stage || !apply(next, stage)) return false;
    this.remember();
    this.apply(next, stage);
    return true;
  }

  private apply(next: DrillScript, stage: Stage): void {
    for (const id of [...this.aimed]) {
      if (stage.steps.some((s) => s.id === id)) aimPass(stage, id);
      else this.aimed.delete(id);
    }
    this.working.set(next);
    this.changed.emit(next);
  }

  /** Keeps a Step playable when its type changes: a speed for movement, a duration otherwise. */
  private fitStepToType(step: Step): void {
    if (MOVING_TYPES.includes(step.type)) {
      if (!(step.speed > 0)) step.speed = DEFAULT_SPEED[step.type];
      step.duration = 0;
    } else {
      step.speed = 0;
      if (!(step.duration > 0)) step.duration = 2;
    }
    if (step.type !== 'PASS') step.targetPartId = '';
  }
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
