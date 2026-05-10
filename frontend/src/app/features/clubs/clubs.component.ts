import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClubApiService, Club } from '../../core/api/club.api';

@Component({
  selector: 'app-clubs',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './clubs.component.html',
  styleUrl: './clubs.component.scss',
})
export class ClubsComponent {
  private readonly api = inject(ClubApiService);
  private readonly fb = inject(FormBuilder);

  clubs = signal<Club[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showForm = signal(false);
  editingId = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    location: ['', Validators.required],
  });

  constructor() {
    this.loadClubs();
  }

  loadClubs(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: clubs => { this.clubs.set(clubs); this.loading.set(false); },
      error: () => { this.error.set('Failed to load clubs'); this.loading.set(false); },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset();
    this.showForm.set(true);
  }

  openEdit(club: Club): void {
    this.editingId.set(club.id);
    this.form.setValue({ name: club.name, location: club.location });
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.form.reset();
  }

  submit(): void {
    if (this.form.invalid) return;
    const { name, location } = this.form.getRawValue();
    const id = this.editingId();

    const request$ = id
      ? this.api.update(id, { name, location })
      : this.api.create({ name, location });

    request$.subscribe({
      next: () => { this.cancelForm(); this.loadClubs(); },
      error: () => this.error.set('Failed to save club'),
    });
  }

  delete(id: string): void {
    if (!confirm('Delete this club? All its teams will also be deleted.')) return;
    this.api.delete(id).subscribe({
      next: () => this.loadClubs(),
      error: () => this.error.set('Failed to delete club'),
    });
  }
}
