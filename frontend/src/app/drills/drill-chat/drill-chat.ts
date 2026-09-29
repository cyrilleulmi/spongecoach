import { Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DrillMessage, DrillVersion } from '../drill.model';

/**
 * The conversation with the interpreter and the script's versions (ADR-0019): the coach writes a
 * correction, the interpreter answers with a new version or asks back. Every version can be made
 * current again; "Rückgängig" goes back to the one before the current.
 */
@Component({
  selector: 'app-drill-chat',
  imports: [FormsModule],
  templateUrl: './drill-chat.html',
  styleUrl: './drill-chat.scss',
})
export class DrillChat {
  readonly messages = input.required<DrillMessage[]>();
  readonly versions = input.required<DrillVersion[]>();
  readonly currentVersion = input<number | null>(null);
  /** Writing to Claude starts a paid job, so it is a Coach's (ADR-0019). */
  readonly canWrite = input(false);
  /** Going back to an earlier version costs nothing, so anyone in the Team may (ADR-0020). */
  readonly canRevert = input(false);
  readonly busy = input(false);

  readonly sent = output<string>();
  readonly reverted = output<number>();

  protected readonly draft = signal('');

  /** The version "Rückgängig" goes back to: the newest one before the current. */
  protected readonly previousVersion = computed(() => {
    const current = this.currentVersion();
    if (current === null) return null;
    return this.versions().find((version) => version.version < current)?.version ?? null;
  });

  protected readonly summaries = computed(() => new Map(this.versions().map((v) => [v.version, v.changeSummary])));

  protected send(): void {
    const message = this.draft().trim();
    if (!message || this.busy()) return;
    this.sent.emit(message);
    this.draft.set('');
  }

  protected sourceLabel(source: DrillVersion['source']): string {
    return source === 'AI' ? 'Claude' : source === 'EDIT' ? 'von Hand' : 'wiederhergestellt';
  }
}
