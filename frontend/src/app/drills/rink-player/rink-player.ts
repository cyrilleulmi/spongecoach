import { Component, OnDestroy, computed, effect, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DrillRink } from '../drill-rink/drill-rink';
import { PathKind, RinkPath } from '../drill-rink/rink-paths';
import { Stage } from '../drill.model';
import { Playback } from '../drill-sampler';

/**
 * Plays a Stage on the rink, looping (ADR-0018): play and pause, scrub through the loop, speed,
 * and step from one Step to the next. The faint lines are the current run's Step paths, drawn the
 * way the coach drew them on the board.
 */
@Component({
  selector: 'app-rink-player',
  imports: [DrillRink, FormsModule],
  templateUrl: './rink-player.html',
  styleUrl: './rink-player.scss',
})
export class RinkPlayer implements OnDestroy {
  readonly stage = input.required<Stage>();
  /** Start playing as soon as there is something to play. */
  readonly autoplay = input(true);

  protected readonly speeds = [0.5, 1, 2];
  protected readonly time = signal(0);
  protected readonly playing = signal(false);
  protected readonly speed = signal(1);

  protected readonly playback = computed(() => new Playback(this.stage()));
  protected readonly frame = computed(() => this.playback().frameAt(this.time()));
  protected readonly slot = computed(() => this.playback().slotAt(this.time()));
  protected readonly loopTime = computed(() => this.playback().wrap(this.time()));

  protected readonly paths = computed<RinkPath[]>(() => {
    const run = this.slot().run;
    const paths: RinkPath[] = [];
    for (const [actorId, movements] of run.movements) {
      for (const move of movements) {
        if (move.kind === 'WAIT') continue;
        paths.push({ id: `${actorId}-${move.stepId}`, kind: move.kind === 'ROAM' ? 'ROAM' : move.dribble ? 'DRIBBLE' : 'RUN', points: move.points });
      }
    }
    for (const flight of run.flights) {
      paths.push({ id: `ball-${flight.stepId}`, kind: (flight.shot ? 'SHOT' : 'PASS') as PathKind, points: flight.points });
    }
    return paths;
  });

  private frameRequest: number | null = null;
  private lastTick: number | null = null;

  constructor() {
    // A new Stage starts from the beginning.
    effect(() => {
      this.stage();
      untracked(() => {
        this.time.set(0);
        if (this.autoplay()) this.play();
      });
    });
  }

  toggle(): void {
    if (this.playing()) this.pause();
    else this.play();
  }

  play(): void {
    if (this.playing()) return;
    this.playing.set(true);
    this.lastTick = null;
    this.requestTick();
  }

  pause(): void {
    this.playing.set(false);
    if (this.frameRequest !== null && typeof cancelAnimationFrame === 'function') {
      cancelAnimationFrame(this.frameRequest);
    }
    this.frameRequest = null;
  }

  protected seek(value: number | string): void {
    this.time.set(Number(value));
  }

  protected stepForward(): void {
    this.pause();
    this.time.set(this.playback().wrap(this.playback().nextStepTime(this.time())));
  }

  protected setSpeed(value: number | string): void {
    this.speed.set(Number(value));
  }

  ngOnDestroy(): void {
    this.pause();
  }

  private requestTick(): void {
    if (typeof requestAnimationFrame !== 'function') return;
    this.frameRequest = requestAnimationFrame((now) => this.tick(now));
  }

  private tick(now: number): void {
    if (!this.playing()) return;
    if (this.lastTick !== null) {
      const elapsed = Math.min(0.1, (now - this.lastTick) / 1000);
      this.time.set(this.playback().wrap(this.time() + elapsed * this.speed()));
    }
    this.lastTick = now;
    this.requestTick();
  }
}
