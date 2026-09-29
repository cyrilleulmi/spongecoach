import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CurrentUserService } from '../../auth/current-user.service';
import { DrillUpload, LEGEND } from './drill-upload';

// Decoding and re-encoding photos needs a real canvas; the maths is covered in image-prep.spec.ts.
jest.mock('../image-prep', () => ({
  ...jest.requireActual('../image-prep'),
  prepareSketch: jest.fn(async (file: Blob, turns: number) => new Blob([`jpeg:${turns}`], { type: 'image/jpeg' })),
}));
import { prepareSketch } from '../image-prep';

const TAGS = [
  { id: 't-pass', name: 'Passen' },
  { id: 't-goalie', name: 'Mit Goalie' },
];

describe('DrillUpload', () => {
  let httpMock: HttpTestingController;

  async function render(role: 'COACH' | 'PLAYER' = 'COACH') {
    await TestBed.configureTestingModule({
      imports: [DrillUpload],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(CurrentUserService).me.set({ id: 'u', name: 'U', role, teamId: 'team', playerId: null, lineIds: [] });

    const fixture = TestBed.createComponent(DrillUpload);
    fixture.detectChanges();
    httpMock.expectOne('/api/drill-tags').flush(TAGS);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  async function addPhotos(fixture: { nativeElement: HTMLElement; detectChanges(): void; whenStable(): Promise<unknown> }, count: number) {
    const input = fixture.nativeElement.querySelector('.file-input') as HTMLInputElement;
    const files = Array.from({ length: count }, (_, i) => new File([`photo ${i}`], `foto-${i}.jpg`, { type: 'image/jpeg' }));
    Object.defineProperty(input, 'files', { value: files, configurable: true });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    // ngModel inside a form wires up new fields asynchronously.
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function type(fixture: { nativeElement: HTMLElement }, selector: string, value: string) {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  afterEach(() => httpMock.verify());

  // spec: ui.drill-upload-legend
  it('shows the legend of the usual symbols next to the form', async () => {
    const fixture = await render();

    expect(fixture.nativeElement.querySelectorAll('.legend-row')).toHaveLength(LEGEND.length);
    expect(fixture.nativeElement.querySelector('.legend')?.textContent).toContain('Pass');
  });

  // spec: ui.drill-upload
  it('uploads the photos in order, turned, with notes, tags and relation, then opens the Drill', async () => {
    const fixture = await render();
    const navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    type(fixture, 'input[name="name"]', 'Bresil');
    await addPhotos(fixture, 2);
    const items = () => Array.from(fixture.nativeElement.querySelectorAll('.sketch-item')) as HTMLElement[];
    (items()[1].querySelector('[aria-label="Nach vorne"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    (items()[0].querySelector('[aria-label="Foto drehen"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    type(fixture, '.sketch-item:first-child .sketch-note', 'Steigerung');
    (fixture.nativeElement.querySelector('input[value="CONTINUOUS"]') as HTMLInputElement).click();
    (Array.from(fixture.nativeElement.querySelectorAll('.tag-chip')) as HTMLButtonElement[])[1].click();
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve));

    const request = httpMock.expectOne('/api/drills');
    const form = request.request.body as FormData;
    expect(form.get('name')).toBe('Bresil');
    expect(form.get('sketchRelation')).toBe('CONTINUOUS');
    expect(form.getAll('notes')).toEqual(['Steigerung', '']);
    expect(form.getAll('tagIds')).toEqual(['t-goalie']);
    expect(form.getAll('sketches')).toHaveLength(2);
    // The second photo moved to the front, then turned once.
    expect((prepareSketch as jest.Mock).mock.calls.map(([file, turns]) => [(file as File).name, turns])).toEqual([
      ['foto-1.jpg', 1],
      ['foto-0.jpg', 0],
    ]);

    request.flush({ id: 'd-9' });
    expect(navigate).toHaveBeenCalledWith(['/uebungen', 'd-9']);
  });

  it('does not ask how a single photo relates to others', async () => {
    const fixture = await render();
    await addPhotos(fixture, 1);

    expect(fixture.nativeElement.querySelector('.relation')).toBeNull();
  });

  // spec: ui.drill-player-read-only
  it('shows no upload form to a Player', async () => {
    const fixture = await render('PLAYER');

    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Nur Coaches');
  });
});
