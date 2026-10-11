import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { getApiErrorMessage } from '../../core/interceptors/api-error';
import { NavbarComponent } from '../../shared/navbar/navbar.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
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
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (response) => {
        this.submitting = false;
        const requestedPath = this.route.snapshot.queryParamMap.get('returnUrl');
        const returnUrl = requestedPath?.startsWith('/') && !requestedPath.startsWith('//')
          ? requestedPath
          : '/admin';
        void this.router.navigateByUrl(response.role.toUpperCase().replace(/^ROLE_/, '') === 'ADMIN'
          ? returnUrl
          : '/home');
      },
      error: (error: unknown) => {
        this.submitting = false;
        this.errorMessage = getApiErrorMessage(error);
      }
    });
  }
}