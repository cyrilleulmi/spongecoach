import { Component, computed, input, output } from '@angular/core';
import { Iteration } from '../iteration.model';

/** Prev/next selector over the ordered Iterations — one Iteration is shown at a time. */
@Component({
  selector: 'app-iteration-selector',
  templateUrl: './iteration-selector.html',
  styleUrl: './iteration-selector.scss',
})
export class IterationSelector {
  readonly iterations = input.required<Iteration[]>();
  readonly index = input.required<number>();
  readonly indexChange = output<number>();

  protected readonly current = computed(() => this.iterations()[this.index()] ?? null);
  protected readonly count = computed(() => this.iterations().length);

  protected go(index: number): void {
    if (index >= 0 && index < this.count() && index !== this.index()) {
      this.indexChange.emit(index);
    }
  }
}
