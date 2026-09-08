import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-rating-bar',
  templateUrl: './rating-bar.html',
  styleUrl: './rating-bar.scss',
})
export class RatingBar {
  readonly rating = input.required<number>();
  readonly color = input.required<string>();
  readonly levelChange = output<number>();

  protected readonly segments = [1, 2, 3, 4, 5];

  protected readonly level = computed(() => Math.max(1, Math.min(5, Math.round((this.rating() / 100) * 5))));

  protected setLevel(level: number): void {
    this.levelChange.emit(level * 20);
  }
}
