import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('saves a successful login and identifies an administrator', () => {
    let responseRole = '';
    service.login({ email: 'admin@example.test', password: 'secret-password' })
      .subscribe((response) => responseRole = response.role);

    const request = http.expectOne(`${environment.apiUrl}/api/v1/auth/login`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'admin@example.test', password: 'secret-password' });
    request.flush({ token: tokenFor('admin@example.test', 'Admin', 7), tokenType: 'Bearer', role: 'Admin', state: true });

    expect(responseRole).toBe('Admin');
    expect(service.token).toBe(tokenFor('admin@example.test', 'Admin', 7));
    expect(localStorage.getItem('e-journey.token')).toBe(tokenFor('admin@example.test', 'Admin', 7));
    expect(service.email).toBe('admin@example.test');
    expect(service.userId).toBe(7);
    expect(service.isAdmin).toBeTrue();

    service.logout();
    expect(service.token).toBeNull();
    expect(service.isAdmin).toBeFalse();
  });

  it('does not grant admin access to a user login', () => {
    service.login({ email: 'traveler@example.test', password: 'secret-password' }).subscribe();
    http.expectOne(`${environment.apiUrl}/api/v1/auth/login`)
      .flush({ token: tokenFor('traveler@example.test', 'User', 8), tokenType: 'Bearer', role: 'User', state: true });

    expect(service.isLoggedIn).toBeTrue();
    expect(service.isAdmin).toBeFalse();
  });

  it('does not treat malformed or expired stored tokens as authenticated', () => {
    localStorage.setItem('e-journey.token', 'not-a-jwt');
    expect(service.isLoggedIn).toBeFalse();
    localStorage.setItem('e-journey.token', tokenFor('admin@example.test', 'Admin', 7, 1));
    expect(service.isLoggedIn).toBeFalse();
    expect(service.isAdmin).toBeFalse();
  });

  function tokenFor(email: string, role: string, userId: number, exp = Math.floor(Date.now() / 1000) + 3600): string {
    const payload = btoa(JSON.stringify({ sub: email, role, userId, exp }))
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
    return `header.${payload}.signature`;
  }
});
