import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FeedbackService } from '../../core/services/feedback.service';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { NavbarComponent } from '../../shared/navbar/navbar.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly feedback = inject(FeedbackService);
  name = '';
  email = '';
  password = '';
  errorMessage = '';
  submitting = false;

  submit(): void {
    if (this.submitting) {
      return;
    }
    this.errorMessage = '';
    this.submitting = true;
    this.authService.register({ fullName: this.name, email: this.email, password: this.password }).subscribe({
      next: () => {
        this.submitting = false;
        this.feedback.showSuccess('Account created successfully! Please sign in with your credentials.', 'Registration Complete');
        void this.router.navigate(['/login'], { queryParams: { registered: 'true' } });
      },
      error: (error: unknown) => {
        this.submitting = false;
        this.errorMessage = getApiErrorMessage(error);
        this.feedback.showFromApiError(error, 'Registration Failed');
      }
    });
  }
}