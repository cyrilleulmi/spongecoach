import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { CATALOG_PALETTE } from '../../theme/palette';
import { CANVAS_SIZE, History, Point, brushWidth, floodFill, hexToRgba, sprayDots } from './paint-engine';

export type PaintTool = 'brush' | 'spray' | 'fill' | 'eraser';
type StartMode = 'edit' | 'new';

/** Canvas pixels per brush size; the canvas is 512 px, shown at roughly half that. */
export const BRUSH_SIZES = [
  { label: 'S', px: 8 },
  { label: 'M', px: 20 },
  { label: 'L', px: 44 },
] as const;

/** Ink, white, skin and hair tones, a few brights, then the catalog colors the team already knows. */
export const PAINT_COLORS: readonly string[] = [
  '#1b2430',
  '#ffffff',
  '#f3d2b5',
  '#d9a37c',
  '#a86b45',
  '#5e3a22',
  '#f2c94c',
  '#3a2418',
  '#d93b3b',
  '#f28c28',
  '#6ec3f0',
  ...CATALOG_PALETTE,
];

/** What "Neu" starts from: the light accent tone the initials avatar sits on. */
const BLANK_COLOR = '#e2efe9';
/** The backend's cap (ADR-0016); a busier painting is saved at half size instead of failing. */
const MAX_UPLOAD_BYTES = 200 * 1024;
const SPRAY_INTERVAL_MS = 30;

const TOOLS: { id: PaintTool; label: string; icon: string }[] = [
  { id: 'brush', label: 'Pinsel', icon: '🖌️' },
  { id: 'spray', label: 'Spray', icon: '💨' },
  { id: 'fill', label: 'Füllen', icon: '🪣' },
  { id: 'eraser', label: 'Radierer', icon: '🧽' },
];

/**
 * Paint-your-own Avatar (ADR-0016). A 512 px square the Player paints on with a circle guide over
 * it: the full square is saved, the round crop is only how it is shown, so the guide and the dimmed
 * corners tell the painter what will be visible. Emits the PNG; the caller does the upload.
 */
@Component({
  selector: 'app-avatar-painter',
  templateUrl: './avatar-painter.html',
  styleUrl: './avatar-painter.scss',
})
export class AvatarPainter implements AfterViewInit, OnDestroy {
  readonly playerName = input.required<string>();
  /** The current Avatar's URL, or null — then there is nothing to edit and it starts blank. */
  readonly existingUrl = input<string | null>(null);
  readonly saving = input(false);
  readonly error = input<string | null>(null);

  readonly save = output<Blob>();
  readonly remove = output<void>();
  readonly cancel = output<void>();

  protected readonly tools = TOOLS;
  protected readonly sizes = BRUSH_SIZES;
  protected readonly colors = PAINT_COLORS;
  protected readonly canvasSize = CANVAS_SIZE;

  protected readonly tool = signal<PaintTool>('brush');
  protected readonly brushSize = signal<number>(BRUSH_SIZES[1].px);
  protected readonly color = signal<string>(PAINT_COLORS[0]);
  protected readonly mode = signal<StartMode>('new');
  protected readonly canUndo = signal(false);
  protected readonly canRedo = signal(false);
  protected readonly dirty = signal(false);

  private readonly canvasRef = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly previewLarge = viewChild.required<ElementRef<HTMLCanvasElement>>('previewLarge');
  private readonly previewSmall = viewChild.required<ElementRef<HTMLCanvasElement>>('previewSmall');
  private readonly dialogRef = viewChild.required<ElementRef<HTMLElement>>('dialog');

  private context: CanvasRenderingContext2D | null = null;
  private readonly history = new History<ImageData>();
  private existingImage: HTMLImageElement | null = null;
  private stroke: { last: Point; mid: Point; width: number } | null = null;
  private sprayAt: Point | null = null;
  private sprayTimer: ReturnType<typeof setInterval> | null = null;
  private previewFrame: number | null = null;

  ngAfterViewInit(): void {
    this.context = this.canvasRef().nativeElement.getContext('2d', { willReadFrequently: true });
    this.paintBlank();
    const url = this.existingUrl();
    if (url) {
      this.mode.set('edit');
      this.loadExisting(url);
    }
    this.dialogRef().nativeElement.focus();
  }

  ngOnDestroy(): void {
    this.stopSpray();
    if (this.previewFrame !== null) {
      cancelAnimationFrame(this.previewFrame);
    }
  }

  // --- start from ------------------------------------------------------------

  /** Swapping between the existing Avatar and a blank canvas is itself an undoable step. */
  protected startFrom(mode: StartMode): void {
    if (mode === this.mode()) {
      return;
    }
    this.recordChange();
    this.mode.set(mode);
    if (mode === 'new') {
      this.paintBlank();
    } else if (this.existingImage) {
      this.drawExisting(this.existingImage);
    } else {
      this.loadExisting(this.existingUrl()!);
    }
  }

  private paintBlank(): void {
    const ctx = this.context;
    if (!ctx) {
      return;
    }
    ctx.save();
    ctx.globalCompositeOperation = 'source-over';
    ctx.fillStyle = BLANK_COLOR;
    ctx.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
    ctx.restore();
    this.schedulePreview();
  }

  private loadExisting(url: string): void {
    const image = new Image();
    image.onload = () => {
      this.existingImage = image;
      // The coach may have hit "Neu" while it loaded; don't paint over their choice.
      if (this.mode() === 'edit') {
        this.drawExisting(image);
      }
    };
    image.src = url;
  }

  private drawExisting(image: HTMLImageElement): void {
    const ctx = this.context;
    if (!ctx) {
      return;
    }
    ctx.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
    ctx.drawImage(image, 0, 0, CANVAS_SIZE, CANVAS_SIZE);
    this.schedulePreview();
  }

  // --- painting --------------------------------------------------------------

  protected onPointerDown(event: PointerEvent): void {
    if (event.pointerType === 'mouse' && event.button !== 0) {
      return;
    }
    const ctx = this.context;
    if (!ctx) {
      return;
    }
    event.preventDefault();
    const point = this.toCanvas(event);
    this.recordChange();

    switch (this.tool()) {
      case 'fill': {
        const pixels = ctx.getImageData(0, 0, CANVAS_SIZE, CANVAS_SIZE);
        if (floodFill(pixels, point.x, point.y, hexToRgba(this.color()))) {
          ctx.putImageData(pixels, 0, 0);
          this.schedulePreview();
        }
        return;
      }
      case 'spray':
        this.capture(event);
        this.sprayAt = point;
        this.spray();
        this.sprayTimer = setInterval(() => this.spray(), SPRAY_INTERVAL_MS);
        return;
      default:
        this.capture(event);
        this.stroke = { last: point, mid: point, width: this.strokeSize() };
        this.prepareStroke(ctx);
        ctx.beginPath();
        ctx.arc(point.x, point.y, this.strokeSize() / 2, 0, Math.PI * 2);
        ctx.fill();
        this.schedulePreview();
    }
  }

  protected onPointerMove(event: PointerEvent): void {
    if (this.sprayAt) {
      this.sprayAt = this.toCanvas(event);
      return;
    }
    const ctx = this.context;
    if (!this.stroke || !ctx) {
      return;
    }
    // Coalesced events carry the samples between frames, so fast strokes stay smooth curves.
    const samples = event.getCoalescedEvents?.() ?? [];
    for (const sample of samples.length > 0 ? samples : [event]) {
      this.strokeTo(ctx, this.toCanvas(sample));
    }
    this.schedulePreview();
  }

  protected onPointerUp(): void {
    const ctx = this.context;
    if (this.stroke && ctx) {
      const { last, mid, width } = this.stroke;
      ctx.lineWidth = width;
      ctx.beginPath();
      ctx.moveTo(mid.x, mid.y);
      ctx.lineTo(last.x, last.y);
      ctx.stroke();
      this.schedulePreview();
    }
    this.stroke = null;
    this.stopSpray();
  }

  /** Quadratic curves through the midpoints of successive samples: smooth, with a pressure-like width. */
  private strokeTo(ctx: CanvasRenderingContext2D, point: Point): void {
    const { last, mid, width: previousWidth } = this.stroke!;
    const nextMid = { x: (last.x + point.x) / 2, y: (last.y + point.y) / 2 };
    // The eraser keeps a steady width: thinning on fast strokes would leave the edges of what it erases.
    const width =
      this.tool() === 'eraser' ? this.strokeSize() : brushWidth(this.brushSize(), previousWidth, last, point);
    ctx.lineWidth = width;
    ctx.beginPath();
    ctx.moveTo(mid.x, mid.y);
    ctx.quadraticCurveTo(last.x, last.y, nextMid.x, nextMid.y);
    ctx.stroke();
    this.stroke = { last: point, mid: nextMid, width };
  }

  /**
   * The eraser paints the blank background back rather than cutting transparent holes, which would
   * show whatever sits behind the image — a different color per theme, and in the saved PNG. It is
   * a size up from the brush so it swallows a stroke's soft edge in one pass.
   */
  private prepareStroke(ctx: CanvasRenderingContext2D): void {
    const paint = this.tool() === 'eraser' ? BLANK_COLOR : this.color();
    ctx.globalCompositeOperation = 'source-over';
    ctx.strokeStyle = paint;
    ctx.fillStyle = paint;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
  }

  private strokeSize(): number {
    return this.tool() === 'eraser' ? this.brushSize() * 1.5 : this.brushSize();
  }

  private spray(): void {
    const ctx = this.context;
    if (!ctx || !this.sprayAt) {
      return;
    }
    const size = this.brushSize();
    ctx.globalCompositeOperation = 'source-over';
    ctx.fillStyle = this.color();
    const dotSize = Math.max(2.5, size / 8);
    for (const dot of sprayDots(this.sprayAt, size * 1.5, Math.round(size * 1.5))) {
      ctx.fillRect(dot.x, dot.y, dotSize, dotSize);
    }
    this.schedulePreview();
  }

  private stopSpray(): void {
    if (this.sprayTimer !== null) {
      clearInterval(this.sprayTimer);
      this.sprayTimer = null;
    }
    this.sprayAt = null;
  }

  private capture(event: PointerEvent): void {
    this.canvasRef().nativeElement.setPointerCapture?.(event.pointerId);
  }

  /** Page coordinates to canvas pixels: the canvas is shown smaller than its 512 px. */
  private toCanvas(event: { clientX: number; clientY: number }): Point {
    const rect = this.canvasRef().nativeElement.getBoundingClientRect();
    const scale = rect.width > 0 ? CANVAS_SIZE / rect.width : 1;
    return { x: (event.clientX - rect.left) * scale, y: (event.clientY - rect.top) * scale };
  }

  // --- undo ------------------------------------------------------------------

  private recordChange(): void {
    const ctx = this.context;
    if (ctx) {
      this.history.record(ctx.getImageData(0, 0, CANVAS_SIZE, CANVAS_SIZE));
    }
    this.dirty.set(true);
    this.syncHistory();
  }

  protected undo(): void {
    this.restore((current) => this.history.undo(current));
  }

  protected redo(): void {
    this.restore((current) => this.history.redo(current));
  }

  private restore(step: (current: ImageData) => ImageData | null): void {
    const ctx = this.context;
    if (!ctx) {
      return;
    }
    const target = step(ctx.getImageData(0, 0, CANVAS_SIZE, CANVAS_SIZE));
    if (target) {
      ctx.putImageData(target, 0, 0);
      this.schedulePreview();
    }
    this.syncHistory();
  }

  private syncHistory(): void {
    this.canUndo.set(this.history.canUndo);
    this.canRedo.set(this.history.canRedo);
  }

  // --- preview ---------------------------------------------------------------

  /** Mirrors the canvas into the round previews, at most once per frame. */
  private schedulePreview(): void {
    if (this.previewFrame !== null || typeof requestAnimationFrame !== 'function') {
      return;
    }
    this.previewFrame = requestAnimationFrame(() => {
      this.previewFrame = null;
      const source = this.canvasRef().nativeElement;
      for (const ref of [this.previewLarge, this.previewSmall]) {
        const preview = ref().nativeElement;
        const ctx = preview.getContext('2d');
        if (ctx) {
          ctx.clearRect(0, 0, preview.width, preview.height);
          ctx.drawImage(source, 0, 0, preview.width, preview.height);
        }
      }
    });
  }

  // --- leaving ---------------------------------------------------------------

  protected onSave(): void {
    if (this.saving()) {
      return;
    }
    const canvas = this.canvasRef().nativeElement;
    let png = toPng(canvas);
    if (png.size > MAX_UPLOAD_BYTES) {
      const half = document.createElement('canvas');
      half.width = half.height = CANVAS_SIZE / 2;
      half.getContext('2d')?.drawImage(canvas, 0, 0, half.width, half.height);
      png = toPng(half);
    }
    this.save.emit(png);
  }

  protected onRemove(): void {
    if (confirm(`Profilbild von ${this.playerName()} entfernen?`)) {
      this.remove.emit();
    }
  }

  protected onCancel(): void {
    if (!this.dirty() || confirm('Bild verwerfen?')) {
      this.cancel.emit();
    }
  }

  @HostListener('document:keydown', ['$event'])
  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.preventDefault();
      this.onCancel();
      return;
    }
    if (!(event.ctrlKey || event.metaKey)) {
      return;
    }
    const key = event.key.toLowerCase();
    if (key === 'z' && !event.shiftKey) {
      event.preventDefault();
      this.undo();
    } else if (key === 'y' || (key === 'z' && event.shiftKey)) {
      event.preventDefault();
      this.redo();
    }
  }
}

/**
 * Synchronous on purpose: Chrome encodes `toBlob` in idle time, which a busy page can put off for
 * seconds — long enough for "Speichern" to look dead. A 512 px `toDataURL` takes milliseconds.
 */
function toPng(canvas: HTMLCanvasElement): Blob {
  const base64 = canvas.toDataURL('image/png').split(',')[1] ?? '';
  const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
  return new Blob([bytes], { type: 'image/png' });
}
