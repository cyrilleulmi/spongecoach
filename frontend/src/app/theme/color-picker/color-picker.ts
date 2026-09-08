import { Component, input, output } from '@angular/core';
import { CATALOG_PALETTE } from '../palette';

/** A row of swatches; the coach picks one color from the fixed palette. */
@Component({
  selector: 'app-color-picker',
  templateUrl: './color-picker.html',
  styleUrl: './color-picker.scss',
})
export class ColorPicker {
  readonly color = input<string | null>(null);
  readonly colorChange = output<string>();

  protected readonly palette = CATALOG_PALETTE;

  protected pick(color: string): void {
    this.colorChange.emit(color);
  }
}
