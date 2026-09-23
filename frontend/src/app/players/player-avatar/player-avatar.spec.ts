import { TestBed } from '@angular/core/testing';
import { PlayerAvatar } from './player-avatar';

describe('PlayerAvatar', () => {
  function render(inputs: { name: string; playerId?: string; avatarVersion?: number | null }) {
    const fixture = TestBed.createComponent(PlayerAvatar);
    fixture.componentRef.setInput('name', inputs.name);
    if (inputs.playerId !== undefined) {
      fixture.componentRef.setInput('playerId', inputs.playerId);
    }
    if (inputs.avatarVersion !== undefined) {
      fixture.componentRef.setInput('avatarVersion', inputs.avatarVersion);
    }
    fixture.detectChanges();
    return fixture;
  }

  // spec: ui.avatar-painted-or-initials
  it('shows the painted Avatar when the Player has one, and initials otherwise', () => {
    const painted = render({ name: 'Carmela', playerId: 'p-1', avatarVersion: 42 }).nativeElement as HTMLElement;
    const plain = render({ name: 'Nives Muster', playerId: 'p-2', avatarVersion: null }).nativeElement as HTMLElement;

    expect(painted.querySelector('img')?.getAttribute('src')).toBe('/api/players/p-1/avatar?v=42');
    expect(plain.querySelector('img')).toBeNull();
    expect(plain.textContent?.trim()).toBe('NM');
  });

  // spec: ui.avatar-painted-or-initials
  it('falls back to initials when the image fails to load, and retries on a new version', () => {
    const fixture = render({ name: 'Carmela', playerId: 'p-1', avatarVersion: 42 });
    const el = fixture.nativeElement as HTMLElement;

    el.querySelector('img')!.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(el.querySelector('img')).toBeNull();
    expect(el.textContent?.trim()).toBe('C');

    fixture.componentRef.setInput('avatarVersion', 43);
    fixture.detectChanges();
    expect(el.querySelector('img')?.getAttribute('src')).toBe('/api/players/p-1/avatar?v=43');
  });
});
