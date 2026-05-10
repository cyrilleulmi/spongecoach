import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { PersonApiService, Person } from '../../core/api/person.api';

@Component({
  selector: 'app-persons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './persons.component.html',
  styleUrl: './persons.component.scss',
})
export class PersonsComponent {
  private readonly api = inject(PersonApiService);
  private readonly fb = inject(FormBuilder);

  persons = signal<Person[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  showForm = signal(false);
  editingId = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  constructor() {
    this.loadPersons();
  }

  loadPersons(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: persons => { this.persons.set(persons); this.loading.set(false); },
      error: () => { this.error.set('Failed to load persons'); this.loading.set(false); },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset();
    this.showForm.set(true);
  }

  openEdit(person: Person): void {
    this.editingId.set(person.id);
    this.form.setValue({ firstName: person.firstName, lastName: person.lastName, email: person.email });
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.form.reset();
    this.error.set(null);
  }

  submit(): void {
    if (this.form.invalid) return;
    const { firstName, lastName, email } = this.form.getRawValue();
    const id = this.editingId();

    const request$ = id
      ? this.api.update(id, { firstName, lastName, email })
      : this.api.create({ firstName, lastName, email });

    request$.subscribe({
      next: () => { this.cancelForm(); this.loadPersons(); },
      error: (err) => this.error.set(err.status === 409 ? 'Email already in use' : 'Failed to save person'),
    });
  }

  delete(id: string): void {
    if (!confirm('Delete this person?')) return;
    this.api.delete(id).subscribe({
      next: () => this.loadPersons(),
      error: () => this.error.set('Failed to delete person'),
    });
  }
}
