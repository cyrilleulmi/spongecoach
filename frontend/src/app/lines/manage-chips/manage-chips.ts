import { Component, input, output } from '@angular/core';

export interface ChipItem {
  id: string;
  name: string;
  color: string;
  tooltip?: string;
}

@Component({
  selector: 'app-manage-chips',
  templateUrl: './manage-chips.html',
  styleUrl: './manage-chips.scss',
})
export class ManageChips {
  readonly items = input.required<ChipItem[]>();
  readonly associatedIds = input.required<ReadonlySet<string>>();
  readonly toggle = output<string>();

  protected isAssociated(id: string): boolean {
    return this.associatedIds().has(id);
  }
}
