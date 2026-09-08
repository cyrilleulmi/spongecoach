import { Component, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'sc-theme';

function readStored(): Theme | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY);
    return value === 'light' || value === 'dark' ? value : null;
  } catch {
    return null;
  }
}

function systemPrefersDark(): boolean {
  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  } catch {
    return false;
  }
}

/** Persists an explicit light/dark choice on <html data-theme> so it overrides the OS setting. */
@Component({
  selector: 'app-theme-toggle',
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.scss',
})
export class ThemeToggle {
  protected readonly theme = signal<Theme>(readStored() ?? (systemPrefersDark() ? 'dark' : 'light'));

  constructor() {
    this.apply(this.theme());
  }

  protected toggle(): void {
    const next: Theme = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(next);
    this.apply(next);
  }

  private apply(theme: Theme): void {
    try {
      document.documentElement.dataset['theme'] = theme;
    } catch {
      /* no document (non-browser test host) — nothing to style */
    }
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      /* storage unavailable — the choice just won't persist across reloads */
    }
  }
}
