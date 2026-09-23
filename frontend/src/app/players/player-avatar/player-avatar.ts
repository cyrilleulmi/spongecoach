import { Component, computed, effect, input, signal } from '@angular/core';
import { avatarUrl } from '../player-api.service';

/**
 * A Player's painted Avatar (ADR-0016), or their initials when they have none — or when the image
 * fails to load, so a broken picture never replaces a name.
 */
@Component({
  selector: 'app-player-avatar',
  template: `
    @if (imageUrl(); as url) {
      <img class="avatar" [class]="size()" [src]="url" alt="" (error)="failed.set(true)" />
    } @else {
      <span class="avatar" [class]="size()" aria-hidden="true">{{ initials() }}</span>
    }
  `,
  styleUrl: './player-avatar.scss',
})
export class PlayerAvatar {
  readonly name = input.required<string>();
  readonly playerId = input<string | null>(null);
  readonly avatarVersion = input<number | null>(null);
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  protected readonly failed = signal(false);

  protected readonly imageUrl = computed(() => {
    const id = this.playerId();
    const version = this.avatarVersion();
    return id && version != null && !this.failed() ? avatarUrl(id, version) : null;
  });

  protected readonly initials = computed(() =>
    this.name()
      .split(' ')
      .filter(Boolean)
      .map((part) => part[0])
      .join(''),
  );

  constructor() {
    // A new version is a new image: give it its own chance to load.
    effect(() => {
      this.avatarVersion();
      this.failed.set(false);
    });
  }
}
