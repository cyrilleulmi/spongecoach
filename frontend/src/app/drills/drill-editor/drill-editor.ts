import { Component, computed, effect, input, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DrillRink, RinkHandle } from '../drill-rink/drill-rink';
import { RinkPath } from '../drill-rink/rink-paths';
import { Actor, DrillScript, Edge, Point, Side, Stage, Step, StepType, isSet } from '../drill.model';
import { DEFAULT_SPEED, schedule } from '../drill-schedule';
import { RinkPlayer } from '../rink-player/rink-player';

export interface ScriptEdit {
  script: DrillScript;
  changeSummary: string;
}

const MOVING: StepType[] = ['RUN', 'DRIBBLE', 'PASS', 'SHOT'];

/**
 * Edits a Stage by hand (ADR-0018): drag start points, waypoints and props on the rink; change a
 * Step's type, timing and speed; see the run as a timeline per Part; undo; preview; save as a new
 * version. Works on a copy until saved.
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

  readonly saved = output<ScriptEdit>();
  readonly cancelled = output<void>();

  protected readonly stepTypes: StepType[] = ['RUN', 'DRIBBLE', 'PASS', 'SHOT', 'ROAM', 'WAIT'];
  protected readonly typeLabels: Record<StepType, string> = {
    RUN: 'Laufweg',
    DRIBBLE: 'Dribbling',
    PASS: 'Pass',
    SHOT: 'Schuss',
    ROAM: 'Bewegung',
    WAIT: 'Warten',
  };
  protected readonly sides: Side[] = ['A', 'B', 'NEUTRAL'];
  protected readonly moving = MOVING;

  protected readonly working = signal<DrillScript>({ stages: [], assumptions: [] });
  protected readonly history = signal<DrillScript[]>([]);
  protected readonly selectedStepId = signal<string | null>(null);
  protected readonly selectedActorId = signal<string | null>(null);
  protected readonly preview = signal(false);
  protected readonly changeSummary = signal('Von Hand angepasst');

  protected readonly stage = computed<Stage | null>(() => this.working().stages[this.stageIndex()] ?? null);
  protected readonly run = computed(() => {
    const stage = this.stage();
    return stage ? schedule(stage) : null;
  });
  protected readonly selectedStep = computed(() => this.stage()?.steps.find((s) => s.id === this.selectedStepId()) ?? null);
  protected readonly selectedActor = computed(() => this.stage()?.actors.find((a) => a.id === this.selectedActorId()) ?? null);
  protected readonly dirty = computed(() => this.history().length > 0);

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
      handles.push({ id: `prop:${prop.id}`, point: prop.position, kind: 'prop' });
    }
    const step = this.selectedStep();
    step?.path.forEach((point, i) => handles.push({ id: `waypoint:${step.id}:${i}`, point, kind: 'waypoint', selected: true }));
    return handles;
  });

  /** One timeline row per Part, with its Steps as bars in seconds. */
  protected readonly timeline = computed(() => {
    const stage = this.stage();
    const run = this.run();
    if (!stage || !run) return { duration: 1, rows: [] };
    const duration = Math.max(run.duration, 0.1);
    const rows = stage.parts.map((part) => {
      const actor = stage.actors.find((a) => a.id === part.actorId);
      return {
        part,
        label: `${actor?.label ?? '?'} · ${part.name}`,
        bars: run.steps
          .filter((s) => s.step.partId === part.id)
          .map((s) => ({ id: s.step.id, type: s.step.type, left: (s.start / duration) * 100, width: Math.max(1.5, ((s.end - s.start) / duration) * 100) })),
      };
    });
    return { duration, rows };
  });

  constructor() {
    effect(() => {
      const script = this.script();
      untracked(() => {
        this.working.set(clone(script));
        this.history.set([]);
        this.selectedStepId.set(null);
        this.selectedActorId.set(null);
      });
    });
  }

  // --- selection --------------------------------------------------------------

  protected selectHandle(id: string): void {
    this.remember();
    const [kind, ref] = id.split(':');
    if (kind === 'actor') {
      this.selectedActorId.set(ref);
    }
  }

  protected selectStep(stepId: string): void {
    this.selectedStepId.set(stepId === this.selectedStepId() ? null : stepId);
  }

  // --- dragging (the snapshot for undo is taken when the drag starts) ------------

  protected moveHandle(event: { id: string; point: Point }): void {
    const [kind, ref, index] = event.id.split(':');
    this.change((stage) => {
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

  // --- step fields ------------------------------------------------------------

  protected setStepField<K extends keyof Step>(field: K, value: Step[K]): void {
    const id = this.selectedStepId();
    this.change((stage) => {
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
    this.setStepField('after', value);
  }

  protected setEdge(value: Edge): void {
    this.setStepField('afterEdge', value);
  }

  protected addWaypoint(): void {
    const id = this.selectedStepId();
    this.change((stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (!step) return;
      const last = step.path[step.path.length - 1] ?? { x: 0, y: 0 };
      step.path.push({ x: Math.min(7, last.x + 1), y: last.y });
    });
  }

  protected removeWaypoint(): void {
    const id = this.selectedStepId();
    this.change((stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (step && step.path.length > (step.type === 'WAIT' ? 0 : 1)) step.path.pop();
    });
  }

  protected deleteStep(): void {
    const id = this.selectedStepId();
    this.change((stage) => {
      const step = stage.steps.find((s) => s.id === id);
      if (!step) return;
      // Whatever waited for it now waits for what it waited for.
      stage.steps.forEach((other) => {
        if (other.after === step.id) {
          other.after = step.after;
          other.afterEdge = step.afterEdge;
          other.delay += step.after ? 0 : step.delay;
        }
      });
      stage.steps = stage.steps.filter((s) => s.id !== step.id);
    });
    this.selectedStepId.set(null);
  }

  // --- actor fields and repetition ----------------------------------------------

  protected setActorField<K extends keyof Actor>(field: K, value: Actor[K]): void {
    const id = this.selectedActorId();
    this.change((stage) => {
      const actor = stage.actors.find((a) => a.id === id);
      if (actor) actor[field] = value;
    });
  }

  protected setRepetition(mode: 'REPLAY' | 'SEAMLESS', mirrored: boolean): void {
    this.change((stage) => (stage.repetition = { mode, mirrored }));
  }

  protected setNextPart(partId: string, nextPartId: string): void {
    this.change((stage) => {
      const part = stage.parts.find((p) => p.id === partId);
      if (part) part.nextPartId = nextPartId;
    });
  }

  // --- history and saving -------------------------------------------------------

  protected undo(): void {
    const history = this.history();
    if (history.length === 0) return;
    this.working.set(history[history.length - 1]);
    this.history.set(history.slice(0, -1));
  }

  protected save(): void {
    this.saved.emit({ script: clone(this.working()), changeSummary: this.changeSummary().trim() || 'Von Hand angepasst' });
  }

  protected isSet = isSet;

  private remember(): void {
    this.history.update((list) => [...list.slice(-49), clone(this.working())]);
  }

  /** Applies a change to the edited Stage; `remember` records an undo step first. */
  private change(apply: (stage: Stage) => void, remember = true): void {
    if (remember) this.remember();
    const next = clone(this.working());
    const stage = next.stages[this.stageIndex()];
    if (!stage) return;
    apply(stage);
    this.working.set(next);
  }

  /** Keeps a Step playable when its type changes: a speed for movement, a duration otherwise. */
  private fitStepToType(step: Step): void {
    if (MOVING.includes(step.type)) {
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
