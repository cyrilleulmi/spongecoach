import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LineBadge } from '../../lines/line-badge/line-badge';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { PlayerApiService } from '../player-api.service';
import { PlayerSummary } from '../player.model';
import { PlayerAvatar } from '../player-avatar/player-avatar';

/** Players with a jersey number first, ordered by number; the others keep their order after them. */
function byJerseyNumber(a: PlayerSummary, b: PlayerSummary): number {
  return (a.jerseyNumber ?? Number.MAX_SAFE_INTEGER) - (b.jerseyNumber ?? Number.MAX_SAFE_INTEGER);
}

@Component({
  selector: 'app-player-list',
  imports: [RouterLink, LineBadge, PlayerAvatar, ThemeToggle],
  templateUrl: './player-list.html',
  styleUrl: './player-list.scss',
})
export class PlayerList implements OnInit {
  private readonly api = inject(PlayerApiService);

  protected readonly players = signal<PlayerSummary[]>([]);
  protected readonly loaded = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  /** The name with the jersey number in front, e.g. "#7 Carmela". */
  protected label(player: PlayerSummary): string {
    return player.jerseyNumber == null ? player.name : `#${player.jerseyNumber} ${player.name}`;
  }

  ngOnInit(): void {
    this.api.listPlayers().subscribe({
      next: (players) => {
        this.players.set([...players].sort(byJerseyNumber));
        this.loaded.set(true);
      },
      error: () => this.errorMessage.set('Spieler konnten nicht geladen werden. Läuft das Backend?'),
    });
  }
}
