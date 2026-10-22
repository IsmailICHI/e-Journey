import { Component, HostListener, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { FeedbackService } from '../../core/services/feedback.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.component.html'
})
export class NavbarComponent implements OnInit {
  @Input() activePage: 'home' | 'admin' | 'profile' | 'login' | 'register' = 'home';

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly feedback = inject(FeedbackService);

  menuOpen = false;
  scrolled = false;

  @HostListener('window:scroll')
  onScroll(): void {
    this.scrolled = window.scrollY > 8;
  }

  ngOnInit(): void {
    this.scrolled = window.scrollY > 8;
    if (this.authService.token && this.authService.isLoggedIn) {
      this.authService.verifySession().subscribe({ error: () => undefined });
    } else if (this.authService.token) {
      this.authService.logout();
    }
  }

  get isAdmin(): boolean {
    return this.authService.isSessionVerified && this.authService.isAdmin;
  }

  get isLoggedIn(): boolean {
    return this.authService.isSessionVerified;
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.closeMenu();
    void this.router.navigateByUrl('/home');
  }

  openFeedbackLog(): void {
    this.closeMenu();
    this.feedback.openLog();
  }
}