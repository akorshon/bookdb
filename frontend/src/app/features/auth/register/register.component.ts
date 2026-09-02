import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: '../../../shared/styles/auth.css',
})
export class RegisterComponent {
  email = '';
  password = '';
  displayName = '';
  error = signal<string | null>(null);
  loading = signal(false);

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  submit(): void {
    this.error.set(null);
    this.loading.set(true);
    this.auth.register(this.email, this.password, this.displayName).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(
          err.status === 409 ? 'An account with this email already exists.' : 'Registration failed. Check your input.',
        );
      },
    });
  }
}
