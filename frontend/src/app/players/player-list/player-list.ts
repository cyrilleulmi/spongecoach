import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LineBadge } from '../../lines/line-badge/line-badge';
import { ThemeToggle } from '../../theme/theme-toggle/theme-toggle';
import { PlayerApiService } from '../player-api.service';
import { PlayerSummary } from '../player.model';
import { PlayerAvatar } from '../player-avatar/player-avatar';

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

  ngOnInit(): void {
    this.api.listPlayers().subscribe({
      next: (players) => {
        this.players.set(players);
        this.loaded.set(true);
      },
      error: () => this.errorMessage.set('Spieler konnten nicht geladen werden. Läuft das Backend?'),
    });
  }
}
