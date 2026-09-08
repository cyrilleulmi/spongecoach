import { TestBed } from '@angular/core/testing';
import { ColorPicker } from './color-picker';
import { CATALOG_PALETTE } from '../palette';

describe('ColorPicker', () => {
  async function render(color: string | null) {
    await TestBed.configureTestingModule({ imports: [ColorPicker] }).compileComponents();
    const fixture = TestBed.createComponent(ColorPicker);
    fixture.componentRef.setInput('color', color);
    await fixture.whenStable();
    return fixture;
  }

  it('renders one swatch per palette color and marks the selected one', async () => {
    const fixture = await render(CATALOG_PALETTE[2]);
    const swatches: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.swatch'));

    expect(swatches).toHaveLength(CATALOG_PALETTE.length);
    expect(swatches[2].classList.contains('selected')).toBe(true);
    expect(swatches[0].classList.contains('selected')).toBe(false);
  });

  it('emits the picked color on click', async () => {
    const fixture = await render(null);
    const picked: string[] = [];
    fixture.componentInstance.colorChange.subscribe((c) => picked.push(c));

    const swatches: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.swatch'));
    swatches[4].click();

    expect(picked).toEqual([CATALOG_PALETTE[4]]);
  });
});
