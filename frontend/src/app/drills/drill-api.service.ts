import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DrillDetail, DrillScript, DrillSummary, DrillTag, SketchRelation } from './drill.model';

/** A photo ready to upload: already downscaled and upright (see `image-prep.ts`). */
export interface SketchUpload {
  jpeg: Blob;
  note: string;
}

/** A sketch never changes once uploaded, so its URL is cached for good. */
export function sketchUrl(drillId: string, position: number): string {
  return `/api/drills/${drillId}/sketches/${position}`;
}

@Injectable({ providedIn: 'root' })
export class DrillApiService {
  private readonly http = inject(HttpClient);

  listDrills(): Observable<DrillSummary[]> {
    return this.http.get<DrillSummary[]>('/api/drills');
  }

  listTags(): Observable<DrillTag[]> {
    return this.http.get<DrillTag[]>('/api/drill-tags');
  }

  getDrill(drillId: string): Observable<DrillDetail> {
    return this.http.get<DrillDetail>(`/api/drills/${drillId}`);
  }

  /** Answers 202 with the Drill PENDING; the interpretation runs in the background (ADR-0019). */
  upload(name: string, sketches: SketchUpload[], tagIds: string[], relation: SketchRelation): Observable<DrillDetail> {
    const form = new FormData();
    form.append('name', name);
    form.append('sketchRelation', relation);
    sketches.forEach((sketch, i) => {
      form.append('sketches', sketch.jpeg, `sketch-${i + 1}.jpg`);
      form.append('notes', sketch.note);
    });
    tagIds.forEach((id) => form.append('tagIds', id));
    return this.http.post<DrillDetail>('/api/drills', form);
  }

  update(drillId: string, changes: { name?: string; tagIds?: string[] }): Observable<DrillDetail> {
    return this.http.put<DrillDetail>(`/api/drills/${drillId}`, changes);
  }

  answer(drillId: string, answers: { questionId: string; text: string }[], guess: boolean): Observable<DrillDetail> {
    return this.http.post<DrillDetail>(`/api/drills/${drillId}/answers`, { answers, guess, message: null });
  }

  chat(drillId: string, message: string): Observable<DrillDetail> {
    return this.http.post<DrillDetail>(`/api/drills/${drillId}/chat`, { message });
  }

  retry(drillId: string): Observable<DrillDetail> {
    return this.http.post<DrillDetail>(`/api/drills/${drillId}/retry`, null);
  }

  saveScript(drillId: string, script: DrillScript, changeSummary: string): Observable<DrillDetail> {
    return this.http.put<DrillDetail>(`/api/drills/${drillId}/script`, { script, changeSummary });
  }

  revert(drillId: string, version: number): Observable<DrillDetail> {
    return this.http.post<DrillDetail>(`/api/drills/${drillId}/revert/${version}`, null);
  }

  delete(drillId: string): Observable<void> {
    return this.http.delete<void>(`/api/drills/${drillId}`);
  }
}
