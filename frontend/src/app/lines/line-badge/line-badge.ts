import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CatalogRef } from '../line.model';

@Component({
  selector: 'app-line-badge',
  imports: [RouterLink],
  template: `<a
    class="line-badge"
    routerLink="/lines"
    [queryParams]="{ line: line().id }"
    [style.background]="line().color"
    [attr.aria-label]="'Block ' + line().name + ' öffnen'"
    >{{ line().name }}</a
  >`,
  styles: `
    .line-badge {
      display: inline-block;
      padding: 0.15rem 0.55rem;
      border-radius: 999px;
      color: #fff;
      font-family: 'Barlow Condensed', ui-sans-serif, system-ui, sans-serif;
      font-size: 0.78rem;
      font-weight: 600;
      letter-spacing: 0.04em;
      text-transform: uppercase;
      text-decoration: none;
    }
    .line-badge:hover {
      filter: brightness(1.12);
    }
  `,
})
export class LineBadge {
  readonly line = input.required<CatalogRef>();
}
