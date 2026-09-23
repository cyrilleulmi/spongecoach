import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-player-avatar',
  template: `<span class="avatar" [class]="size()" aria-hidden="true">{{ initials() }}</span>`,
  styleUrl: './player-avatar.scss',
})
export class PlayerAvatar {
  readonly name = input.required<string>();
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  protected readonly initials = computed(() =>
    this.name()
      .split(' ')
      .filter(Boolean)
      .map((part) => part[0])
      .join(''),
  );
}
