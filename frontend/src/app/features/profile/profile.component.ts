import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { PersonApiService } from '../../core/api/person.api';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private readonly api = inject(PersonApiService);
  protected readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (!user) return;
    this.loading.set(true);
    this.api.get(user.personId).subscribe({
      next: person => {
        this.form.setValue({ firstName: person.firstName, lastName: person.lastName, email: person.email });
        this.loading.set(false);
      },
      error: () => { this.error.set('Failed to load profile'); this.loading.set(false); },
    });
  }

  save(): void {
    if (this.form.invalid) return;
    const user = this.auth.currentUser();
    if (!user) return;
    this.saving.set(true);
    this.error.set(null);
    this.success.set(false);
    const { firstName, lastName, email } = this.form.getRawValue();
    this.api.update(user.personId, { firstName, lastName, email }).subscribe({
      next: () => { this.saving.set(false); this.success.set(true); },
      error: err => {
        this.saving.set(false);
        this.error.set(err.status === 409 ? 'Email already in use' : 'Failed to save profile');
      },
    });
  }

  deleteAccount(): void {
    if (!confirm('Are you sure you want to delete your account? This action cannot be undone.')) return;
    const user = this.auth.currentUser();
    if (!user) return;
    this.error.set(null);
    this.api.delete(user.personId).subscribe({
      next: () => this.router.navigate(['/clubs']),
      error: err => {
        if (err.status === 409) {
          this.error.set('You are the last admin of one or more clubs. Assign another admin before deleting your account.');
        } else {
          this.error.set('Failed to delete account');
        }
      },
    });
  }
}
