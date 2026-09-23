import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AvatarPainter, BRUSH_SIZES, PAINT_COLORS } from './avatar-painter';

/**
 * jsdom has no canvas, so each canvas gets a recording stand-in context. `getImageData` hands out
 * numbered snapshots, which is enough to follow undo/redo without real pixels.
 */
function stubContext() {
  let snapshots = 0;
  const calls: Record<string, jest.Mock> = {
    getImageData: jest.fn(() => ({ snapshot: ++snapshots })),
  };
  return new Proxy(calls, {
    get: (target, key: string) => (target[key] ??= jest.fn()),
    set: () => true,
  }) as unknown as Record<string, jest.Mock>;
}

describe('AvatarPainter', () => {
  let fixture: ComponentFixture<AvatarPainter>;
  let el: HTMLElement;
  let contexts: Map<HTMLCanvasElement, Record<string, jest.Mock>>;
  let frames: FrameRequestCallback[];
  let blobSizes: number[];

  function mainContext() {
    return contexts.get(el.querySelector('canvas.canvas') as HTMLCanvasElement)!;
  }

  function render(existingUrl: string | null = null) {
    fixture = TestBed.createComponent(AvatarPainter);
    fixture.componentRef.setInput('playerName', 'Carmela');
    fixture.componentRef.setInput('existingUrl', existingUrl);
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  function button(label: string): HTMLButtonElement {
    const match = [...el.querySelectorAll('button')].find((b) => b.textContent?.includes(label));
    if (!match) {
      throw new Error(`no button "${label}"`);
    }
    return match;
  }

  function pointer(type: string, x = 40, y = 40) {
    const event = Object.assign(
      new MouseEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y, button: 0 }),
      { pointerType: 'mouse', pointerId: 1 },
    );
    el.querySelector('canvas.canvas')!.dispatchEvent(event);
    fixture.detectChanges();
  }

  function paintAStroke() {
    pointer('pointerdown');
    pointer('pointermove', 60, 60);
    pointer('pointerup', 60, 60);
  }

  beforeEach(() => {
    contexts = new Map();
    frames = [];
    blobSizes = [100];
    jest.spyOn(HTMLCanvasElement.prototype, 'getContext').mockImplementation(function (this: HTMLCanvasElement) {
      if (!contexts.has(this)) {
        contexts.set(this, stubContext());
      }
      return contexts.get(this) as never;
    });
    jest
      .spyOn(HTMLCanvasElement.prototype, 'toDataURL')
      .mockImplementation(() => 'data:image/png;base64,' + btoa('x'.repeat(blobSizes.shift() ?? 100)));
    jest.spyOn(window, 'requestAnimationFrame').mockImplementation((callback) => frames.push(callback));
  });

  afterEach(() => jest.restoreAllMocks());

  // spec: ui.avatar-painter-opens
  it('offers brush, spray, fill and eraser, three brush sizes and a color palette', () => {
    render();

    for (const tool of ['Pinsel', 'Spray', 'Füllen', 'Radierer']) {
      expect(button(tool)).toBeTruthy();
    }
    expect(el.querySelectorAll('.size')).toHaveLength(BRUSH_SIZES.length);
    expect(el.querySelectorAll('button.swatch')).toHaveLength(PAINT_COLORS.length);
    expect(el.querySelector('input[type="color"]')).not.toBeNull();

    button('Spray').click();
    fixture.detectChanges();
    expect(button('Spray').getAttribute('aria-pressed')).toBe('true');
    expect(button('Pinsel').getAttribute('aria-pressed')).toBe('false');
  });

  // spec: ui.avatar-painter-shows-crop
  it('draws a circle guide over the canvas and mirrors each stroke into round previews', () => {
    render();

    expect(el.querySelector('.stage .crop-guide')).not.toBeNull();
    expect(el.querySelectorAll('canvas.preview')).toHaveLength(2);

    frames.splice(0).forEach((frame) => frame(0));
    paintAStroke();
    frames.splice(0).forEach((frame) => frame(0));

    for (const preview of el.querySelectorAll<HTMLCanvasElement>('canvas.preview')) {
      expect(contexts.get(preview)!['drawImage']).toHaveBeenCalledWith(
        el.querySelector('canvas.canvas'),
        0,
        0,
        preview.width,
        preview.height,
      );
    }
  });

  // spec: ui.avatar-painter-edit-or-new
  it('starts on the existing Avatar, and "Neu" swaps to a blank canvas as an undoable step', () => {
    render('/api/players/p-1/avatar?v=1');

    expect(button('Bearbeiten').getAttribute('aria-pressed')).toBe('true');
    const ctx = mainContext();
    ctx['fillRect'].mockClear();

    button('Neu').click();
    fixture.detectChanges();

    expect(button('Neu').getAttribute('aria-pressed')).toBe('true');
    expect(ctx['fillRect']).toHaveBeenCalledWith(0, 0, 512, 512);
    expect(button('Rückgängig').disabled).toBe(false);
  });

  // spec: ui.avatar-painter-edit-or-new
  it('has no edit/new choice when there is no Avatar yet', () => {
    render();

    expect(el.querySelector('.segmented')).toBeNull();
    expect(el.textContent).not.toContain('Entfernen');
  });

  // spec: ui.avatar-painter-undo
  it('undoes a stroke and redoes it', () => {
    render();
    expect(button('Rückgängig').disabled).toBe(true);

    paintAStroke();
    const ctx = mainContext();
    const beforeStroke = ctx['getImageData'].mock.results[0].value;

    button('Rückgängig').click();
    fixture.detectChanges();
    expect(ctx['putImageData']).toHaveBeenLastCalledWith(beforeStroke, 0, 0);
    expect(button('Wiederholen').disabled).toBe(false);

    const afterStroke = ctx['getImageData'].mock.results[1].value;
    button('Wiederholen').click();
    fixture.detectChanges();
    expect(ctx['putImageData']).toHaveBeenLastCalledWith(afterStroke, 0, 0);
  });

  // spec: ui.avatar-painter-undo
  it('undoes with Ctrl+Z', () => {
    render();
    paintAStroke();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'z', ctrlKey: true }));
    fixture.detectChanges();

    expect(mainContext()['putImageData']).toHaveBeenCalled();
    expect(button('Wiederholen').disabled).toBe(false);
  });

  // spec: ui.avatar-save
  it('emits the canvas as a PNG on save', async () => {
    render();
    const saved: Blob[] = [];
    fixture.componentInstance.save.subscribe((png) => saved.push(png));

    button('Speichern').click();
    await fixture.whenStable();

    expect(saved).toHaveLength(1);
    expect(saved[0].type).toBe('image/png');
  });

  // spec: ui.avatar-save
  it('saves a busy painting at half size rather than exceeding the upload cap', async () => {
    render();
    blobSizes = [300 * 1024, 90 * 1024];
    const saved: Blob[] = [];
    fixture.componentInstance.save.subscribe((png) => saved.push(png));

    button('Speichern').click();
    await fixture.whenStable();

    expect(saved[0].size).toBe(90 * 1024);
  });

  // spec: ui.avatar-cancel
  it('cancels straight away when nothing was painted', () => {
    render();
    const confirm = jest.spyOn(window, 'confirm');
    const cancel = jest.fn();
    fixture.componentInstance.cancel.subscribe(cancel);

    button('Abbrechen').click();

    expect(confirm).not.toHaveBeenCalled();
    expect(cancel).toHaveBeenCalled();
  });

  // spec: ui.avatar-cancel
  it('asks before discarding a painting, on Abbrechen or Escape', () => {
    render();
    paintAStroke();
    const confirm = jest.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
    const cancel = jest.fn();
    fixture.componentInstance.cancel.subscribe(cancel);

    button('Abbrechen').click();
    expect(cancel).not.toHaveBeenCalled();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(confirm).toHaveBeenCalledTimes(2);
    expect(cancel).toHaveBeenCalledTimes(1);
  });

  // spec: ui.avatar-remove
  it('removes the Avatar after confirming', () => {
    render('/api/players/p-1/avatar?v=1');
    jest.spyOn(window, 'confirm').mockReturnValue(true);
    const remove = jest.fn();
    fixture.componentInstance.remove.subscribe(remove);

    button('Entfernen').click();

    expect(remove).toHaveBeenCalled();
  });
});
