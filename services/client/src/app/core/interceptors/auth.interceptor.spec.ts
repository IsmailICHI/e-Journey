import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { environment } from '../../../environments/environment';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    localStorage.setItem('e-journey.token', tokenFor('admin@example.test', 'Admin', 7));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    httpClient = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
    sessionStorage.clear();
  });

  it('adds the bearer token to API requests', () => {
    httpClient.get(`${environment.apiUrl}/api/v1/admin/users`).subscribe();
    const request = http.expectOne(`${environment.apiUrl}/api/v1/admin/users`);
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${tokenFor('admin@example.test', 'Admin', 7)}`);
    request.flush([]);
  });

  it('does not attach credentials to non-API requests', () => {
    httpClient.get('https://images.example.test/photo.jpg').subscribe();
    const request = http.expectOne('https://images.example.test/photo.jpg');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  function tokenFor(email: string, role: string, userId: number): string {
    const payload = btoa(JSON.stringify({
      sub: email,
      role,
      userId,
      exp: Math.floor(Date.now() / 1000) + 3600
    })).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    return `header.${payload}.signature`;
  }
});
